/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.webapps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.thoughtcrime.securesms.PassphraseRequiredActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.databinding.WebappViewerActivityBinding
import org.thoughtcrime.securesms.util.DynamicTheme

class WebAppViewerActivity : PassphraseRequiredActivity() {

  private lateinit var binding: WebappViewerActivityBinding
  private val dynamicTheme = DynamicTheme()

  companion object {
    private const val EXTRA_ENTRY_URL = "entry_url"

    fun createIntent(context: Context, entryUrl: String): Intent {
      return Intent(context, WebAppViewerActivity::class.java)
        .putExtra(EXTRA_ENTRY_URL, entryUrl)
    }
  }

  override fun onPreCreate() {
    super.onPreCreate()
    dynamicTheme.onCreate(this)
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    super.onCreate(savedInstanceState, ready)

    // Hide action bar
    supportActionBar?.hide()

    // Make fullscreen
    window.setFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN
    )

    // Hide system bars
    val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
    windowInsetsController?.let {
      it.hide(WindowInsetsCompat.Type.systemBars())
      it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    binding = WebappViewerActivityBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val entryUrl = intent.getStringExtra(EXTRA_ENTRY_URL) ?: return

    binding.webView.apply {
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.cacheMode = WebSettings.LOAD_DEFAULT
      settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile Chrome/142.0.7444.139 Safari/604.1 HCPApp/2.0"
      webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          updateNavigationButtons()
        }
      }
      loadUrl(entryUrl)
    }

    binding.backButton.setOnClickListener {
      if (binding.webView.canGoBack()) {
        binding.webView.goBack()
      }
    }

    binding.forwardButton.setOnClickListener {
      if (binding.webView.canGoForward()) {
        binding.webView.goForward()
        updateNavigationButtons()
      }
    }

    binding.closeButton.setOnClickListener {
      finish()
    }

    updateNavigationButtons()
  }

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }

  override fun onBackPressed() {
    if (binding.webView.canGoBack()) {
      binding.webView.goBack()
      updateNavigationButtons()
    } else {
      super.onBackPressed()
    }
  }

  private fun updateNavigationButtons() {
    binding.backButton.isEnabled = binding.webView.canGoBack()
    binding.forwardButton.isEnabled = binding.webView.canGoForward()
  }
}

