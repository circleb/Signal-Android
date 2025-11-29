/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.sso

sealed class SSOError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
  object ConfigurationMissing : SSOError("SSO is not configured correctly.")
  object InvalidToken : SSOError("Access token was missing or invalid.")
  object UserCancelled : SSOError("The user cancelled the SSO flow.")
  object MissingUserInfo : SSOError("User info response was empty.")
  object RoleAccessDenied : SSOError("User does not have the required roles to continue.")
  object RefreshUnavailable : SSOError("No refresh token or auth state is available.")
  class NetworkError(cause: Throwable) : SSOError("Unable to reach the SSO service.", cause)
  class ServerError(message: String?) : SSOError(message ?: "SSO server returned an error.")
}
