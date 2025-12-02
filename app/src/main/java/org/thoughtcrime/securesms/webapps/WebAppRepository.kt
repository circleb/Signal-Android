/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.webapps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.signal.core.util.logging.Log

class WebAppRepository(
  private val httpClient: OkHttpClient,
  private val json: Json = Json { ignoreUnknownKeys = true }
) {
  companion object {
    private val TAG = Log.tag(WebAppRepository::class.java)
    private const val API_URL = "https://my.homesteadheritage.org/api/v2/webapps.php"
  }

  suspend fun getWebApps(): Result<List<WebApp>> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(API_URL)
        .get()
        .build()

      val response = httpClient.newCall(request).execute()

      if (!response.isSuccessful) {
        Log.w(TAG, "Failed to fetch web apps: ${response.code}")
        return@withContext Result.failure(Exception("HTTP ${response.code}"))
      }

      val body = response.body?.string()
      if (body == null) {
        Log.w(TAG, "Response body is null")
        return@withContext Result.failure(Exception("Empty response body"))
      }

      val webApps = json.decodeFromString<List<WebApp>>(body)
      Result.success(webApps)
    } catch (e: Exception) {
      Log.e(TAG, "Error fetching web apps", e)
      Result.failure(e)
    }
  }
}

