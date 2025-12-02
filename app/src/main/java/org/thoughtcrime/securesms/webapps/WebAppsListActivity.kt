/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.webapps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.thoughtcrime.securesms.PassphraseRequiredActivity
import org.thoughtcrime.securesms.compose.SignalTheme
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.util.DynamicTheme

class WebAppsListActivity : PassphraseRequiredActivity() {
  companion object {
    @JvmStatic
    fun createIntent(context: Context): Intent {
      return Intent(context, WebAppsListActivity::class.java)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState, ready)

    val repository = WebAppRepository(AppDependencies.okHttpClient)

    setContent {
      SignalTheme(
        isDarkMode = DynamicTheme.isDarkTheme(this)
      ) {
        WebAppsListScreen(
          repository = repository,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

