/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.sso

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SSOUserInfo(
  val phoneNumber: String? = null,
  val email: String? = null,
  val name: String? = null,
  val sub: String,
  val accessToken: String,
  val refreshToken: String? = null,
  val roles: List<String> = emptyList(),
  val groups: List<String> = emptyList(),
  val realmAccess: Map<String, List<String>>? = null,
  val resourceAccess: Map<String, List<String>>? = null
)

@Serializable
internal data class KeycloakUserInfo(
  val sub: String,
  val email: String? = null,
  val name: String? = null,
  @SerialName("given_name") val givenName: String? = null,
  @SerialName("family_name") val familyName: String? = null,
  @SerialName("preferred_username") val preferredUsername: String? = null,
  @SerialName("email_verified") val emailVerified: Boolean? = null,
  @SerialName("phone") val phoneNumber: String? = null,
  @SerialName("realm_access") val realmAccess: RealmAccess? = null,
  @SerialName("resource_access") val resourceAccess: Map<String, ResourceAccess>? = null,
  val groups: List<String>? = null
) {

  @Serializable
  data class RealmAccess(
    val roles: List<String> = emptyList()
  )

  @Serializable
  data class ResourceAccess(
    val roles: List<String> = emptyList()
  )

  fun toUserInfo(accessToken: String, refreshToken: String?): SSOUserInfo {
    val aggregatedRoles = buildList {
      realmAccess?.roles?.let(::addAll)
      resourceAccess?.values?.forEach { addAll(it.roles) }
    }

    val realmRoleMap = realmAccess?.roles?.let { mapOf("realm_access" to it) }
    val resourceRoleMap = resourceAccess?.mapValues { it.value.roles }

    return SSOUserInfo(
      phoneNumber = phoneNumber,
      email = email,
      name = name ?: listOfNotNull(givenName, familyName).joinToString(" ").ifBlank { preferredUsername ?: email },
      sub = sub,
      accessToken = accessToken,
      refreshToken = refreshToken,
      roles = aggregatedRoles,
      groups = groups.orEmpty(),
      realmAccess = realmRoleMap,
      resourceAccess = resourceRoleMap
    )
  }
}
