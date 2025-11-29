/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.sso

class SSORoleManager(
  private val userInfoStore: SSOUserInfoStore
) {

  fun getUserRoles(): List<String> = userInfoStore.getUserRoles()

  fun getUserGroups(): List<String> = userInfoStore.getUserGroups()

  fun hasRole(role: String): Boolean = userInfoStore.hasRole(role)

  fun hasGroup(group: String): Boolean = userInfoStore.hasGroup(group)

  fun hasAnyRole(roles: Collection<String>): Boolean = userInfoStore.hasAnyRole(roles)

  fun hasAnyGroup(groups: Collection<String>): Boolean = userInfoStore.hasAnyGroup(groups)

  fun hasAllRoles(roles: Collection<String>): Boolean = roles.all(::hasRole)

  fun hasAllGroups(groups: Collection<String>): Boolean = groups.all(::hasGroup)

  fun getRoleBasedFeatures(): Set<String> {
    val result = mutableSetOf<String>()
    for (role in getUserRoles()) {
      val features = SSOConfig.roleBasedFeatures[role]
      if (features != null) {
        result += features
      }
    }
    return result
  }

  fun isFeatureEnabled(feature: String): Boolean = getRoleBasedFeatures().contains(feature)
}
