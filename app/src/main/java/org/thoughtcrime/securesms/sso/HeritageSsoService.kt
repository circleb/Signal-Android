/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.sso

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.keyvalue.SignalStore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HeritageSsoService(
  private val context: Context,
  private val userInfoStore: SSOUserInfoStore = SignalStoreUserInfoStore(),
  private val httpClient: OkHttpClient = OkHttpClient(),
  private val json: Json = Json { ignoreUnknownKeys = true }
) {

  companion object {
    private val TAG = Log.tag(HeritageSsoService::class.java)
  }

  private val serviceConfiguration = AuthorizationServiceConfiguration(
    Uri.parse(SSOConfig.authorizationEndpoint),
    Uri.parse(SSOConfig.tokenEndpoint),
    null,
    Uri.parse(SSOConfig.endSessionEndpoint)
  )

  private val authorizationService: AuthorizationService by lazy { AuthorizationService(context) }

  private var authState: AuthState? = SignalStore.sso.authState?.let { storedState ->
    runCatching { AuthState.jsonDeserialize(storedState) }.getOrNull()
  }

  fun createAuthorizationRequest(loginHint: String? = null): AuthorizationRequest {
    val builder = AuthorizationRequest.Builder(
      serviceConfiguration,
      SSOConfig.clientId,
      ResponseTypeValues.CODE,
      Uri.parse(SSOConfig.redirectUri)
    ).setScopes(SSOConfig.scopes)

    if (!loginHint.isNullOrBlank()) {
      builder.setLoginHint(loginHint)
    }

    return builder.build()
  }

  fun performAuthorizationRequest(
    request: AuthorizationRequest,
    completionIntent: PendingIntent,
    cancelIntent: PendingIntent
  ) {
    authorizationService.performAuthorizationRequest(request, completionIntent, cancelIntent)
  }

  suspend fun handleAuthorizationResponse(intent: Intent): SSOUserInfo {
    val response = AuthorizationResponse.fromIntent(intent)
    val exception = AuthorizationException.fromIntent(intent)

    if (response == null) {
      if (exception?.code == AuthorizationException.GeneralErrors.USER_CANCELED.code) {
        throw SSOError.UserCancelled
      }
      throw SSOError.ServerError(exception?.errorDescription)
    }

    val tokenRequest = response.createTokenExchangeRequest()
    return exchangeToken(tokenRequest, exception)
  }

  suspend fun refreshUserInfo(): SSOUserInfo {
    val state = authState ?: throw SSOError.RefreshUnavailable

    return suspendCancellableCoroutine { continuation ->
      state.performActionWithFreshTokens(authorizationService) { accessToken, _, exception ->
        when {
          exception != null -> continuation.resumeWithException(SSOError.NetworkError(exception))
          accessToken.isNullOrBlank() -> continuation.resumeWithException(SSOError.InvalidToken)
          else -> {
            val result = runCatching {
              fetchAndPersistUserInfo(accessToken, state.refreshToken)
            }
            if (result.isSuccess) {
              continuation.resume(result.getOrThrow())
            } else {
              continuation.resumeWithException(result.exceptionOrNull()!!)
            }
          }
        }
      }
    }
  }

  fun cachedUserInfo(): SSOUserInfo? = userInfoStore.getUserInfo()

  fun signOut() {
    authState = null
    SignalStore.sso.authState = null
    userInfoStore.clearUserInfo()
  }

  private suspend fun exchangeToken(
    tokenRequest: TokenRequest,
    authorizationException: AuthorizationException?
  ): SSOUserInfo {
    val tokenResponse = executeTokenRequest(tokenRequest)

    val newAuthState = authState ?: AuthState(serviceConfiguration)
    newAuthState.update(tokenResponse, authorizationException)
    authState = newAuthState
    SignalStore.sso.authState = newAuthState.jsonSerializeString()

    val accessToken = tokenResponse.accessToken ?: throw SSOError.InvalidToken
    return fetchAndPersistUserInfo(accessToken, tokenResponse.refreshToken)
  }

  private suspend fun executeTokenRequest(request: TokenRequest): TokenResponse {
    val clientAuthentication = clientAuthentication()
    return suspendCancellableCoroutine { continuation ->
      val callback = AuthorizationService.TokenResponseCallback { response, exception ->
        when {
          exception != null -> continuation.resumeWithException(SSOError.NetworkError(exception))
          response == null -> continuation.resumeWithException(SSOError.MissingUserInfo)
          else -> continuation.resume(response)
        }
      }

      if (clientAuthentication != null) {
        authorizationService.performTokenRequest(request, clientAuthentication, callback)
      } else {
        authorizationService.performTokenRequest(request, callback)
      }
    }
  }

  private fun clientAuthentication(): ClientAuthentication? {
    return if (SSOConfig.clientSecret.isBlank()) {
      null
    } else {
      ClientSecretPost(SSOConfig.clientSecret)
    }
  }

  private suspend fun fetchAndPersistUserInfo(
    accessToken: String,
    refreshToken: String?
  ): SSOUserInfo {
    val userInfo = fetchUserInfo(accessToken, refreshToken)
    userInfoStore.storeUserInfo(userInfo)
    Log.i(TAG, "SSO user authenticated – sub=${userInfo.sub}")
    return userInfo
  }

  private suspend fun fetchUserInfo(accessToken: String, refreshToken: String?): SSOUserInfo {
    val request = Request.Builder()
      .url(SSOConfig.userInfoEndpoint)
      .header("Authorization", "Bearer $accessToken")
      .build()

    val response = withContext(Dispatchers.IO) {
      httpClient.newCall(request).execute()
    }

    response.use { httpResponse ->
      if (!httpResponse.isSuccessful) {
        val errorBody = httpResponse.body?.string()
        throw SSOError.ServerError(errorBody)
      }

      val rawBody = httpResponse.body?.string() ?: throw SSOError.MissingUserInfo
      val keycloak = json.decodeFromString<KeycloakUserInfo>(rawBody)
      val userInfo = keycloak.toUserInfo(accessToken, refreshToken)
      ensureRoleAccess(userInfo)
      return userInfo
    }
  }

  private fun ensureRoleAccess(userInfo: SSOUserInfo) {
    val hasRequiredRole = userInfo.roles.any { SSOConfig.requiredRoles.contains(it) }
    val hasRequiredGroup = userInfo.groups.any { SSOConfig.requiredGroups.contains(it) }
    if (!hasRequiredRole && !hasRequiredGroup) {
      throw SSOError.RoleAccessDenied
    }
  }
}
