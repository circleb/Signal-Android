/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.sso

/**
 * Static configuration for Heritage Community Platform SSO.
 *
 * The values mirror the configuration used by the iOS fork so we can talk to the same Keycloak realm.
 */
object SSOConfig {
  private const val BASE_URL = "https://auth.homesteadheritage.org"
  private const val REALM = "heritage"

  const val clientId: String = "signal_homesteadheritage_org"
  const val clientSecret: String = ""

  val scopes: List<String> = listOf(
    "openid",
    "profile",
    "email",
    "offline_access",
    "phone",
    "roles"
  )

  val authorizationEndpoint: String = "$BASE_URL/realms/$REALM/protocol/openid-connect/auth"
  val tokenEndpoint: String = "$BASE_URL/realms/$REALM/protocol/openid-connect/token"
  val userInfoEndpoint: String = "$BASE_URL/realms/$REALM/protocol/openid-connect/userinfo"
  val endSessionEndpoint: String = "$BASE_URL/realms/$REALM/protocol/openid-connect/logout"

  const val redirectUri: String = "heritagesignal://oauth/callback"

  val requiredRoles: Set<String> = setOf("heritage-member", "heritage-member-associate")
  val requiredGroups: Set<String> = setOf("heritage_members")

  val roleBasedFeatures: Map<String, Set<String>> = mapOf(
    "heritage_member" to setOf("messaging", "calls", "groups", "heritage_features"),
    "admin" to setOf("messaging", "calls", "groups", "heritage_features", "admin_panel")
  )
}
