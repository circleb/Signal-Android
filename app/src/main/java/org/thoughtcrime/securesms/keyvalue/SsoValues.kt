/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.keyvalue

/**
 * KeyValue-backed storage for Heritage SSO state.
 *
 * The serialized payloads stored here are managed by the SSO service layer and are intentionally
 * omitted from backups.
 */
class SsoValues internal constructor(store: KeyValueStore) : SignalStoreValues(store) {

  companion object {
    private const val KEY_USER_INFO = "sso.user.info"
    private const val KEY_AUTH_STATE = "sso.auth.state"
  }

  override fun onFirstEverAppLaunch() {
    userInfo = null
    authState = null
  }

  override fun getKeysToIncludeInBackup(): List<String> = emptyList()

  var userInfo by stringValue(KEY_USER_INFO, null)
  var authState by stringValue(KEY_AUTH_STATE, null)
}
