/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.sso

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.thoughtcrime.securesms.keyvalue.SignalStore

interface SSOUserInfoStore {
  fun storeUserInfo(userInfo: SSOUserInfo)
  fun getUserInfo(): SSOUserInfo?
  fun clearUserInfo()
  fun getUserRoles(): List<String>
  fun getUserGroups(): List<String>
  fun hasRole(role: String): Boolean
  fun hasGroup(group: String): Boolean
  fun hasAnyRole(roles: Collection<String>): Boolean
  fun hasAnyGroup(groups: Collection<String>): Boolean
}

class SignalStoreUserInfoStore(
  private val json: Json = Json { ignoreUnknownKeys = true }
) : SSOUserInfoStore {

  override fun storeUserInfo(userInfo: SSOUserInfo) {
    SignalStore.sso.userInfo = json.encodeToString(userInfo)
  }

  override fun getUserInfo(): SSOUserInfo? {
    val serialized = SignalStore.sso.userInfo ?: return null
    return runCatching { json.decodeFromString<SSOUserInfo>(serialized) }.getOrNull()
  }

  override fun clearUserInfo() {
    SignalStore.sso.userInfo = null
  }

  override fun getUserRoles(): List<String> = getUserInfo()?.roles ?: emptyList()

  override fun getUserGroups(): List<String> = getUserInfo()?.groups ?: emptyList()

  override fun hasRole(role: String): Boolean = getUserRoles().contains(role)

  override fun hasGroup(group: String): Boolean = getUserGroups().contains(group)

  override fun hasAnyRole(roles: Collection<String>): Boolean {
    if (roles.isEmpty()) return false
    val userRoles = getUserRoles()
    return roles.any(userRoles::contains)
  }

  override fun hasAnyGroup(groups: Collection<String>): Boolean {
    if (groups.isEmpty()) return false
    val userGroups = getUserGroups()
    return groups.any(userGroups::contains)
  }
}
