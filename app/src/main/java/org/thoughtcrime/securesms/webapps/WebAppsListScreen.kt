/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.webapps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.theme.SignalTheme

@Composable
fun WebAppsListScreen(
  repository: WebAppRepository,
  searchQuery: String = "",
  modifier: Modifier = Modifier
) {
  var webApps by remember { mutableStateOf<List<WebApp>?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    repository.getWebApps()
      .onSuccess { apps ->
        webApps = apps
        isLoading = false
      }
      .onFailure { e ->
        error = e.message ?: "Unknown error"
        isLoading = false
      }
  }

  val filteredWebApps = remember(webApps, searchQuery) {
    if (webApps == null || searchQuery.isBlank()) {
      webApps
    } else {
      val query = searchQuery.lowercase().trim()
      webApps!!.filter { app ->
        app.name.lowercase().contains(query) ||
        app.description.lowercase().contains(query) ||
        app.category?.lowercase()?.contains(query) == true ||
        app.type?.lowercase()?.contains(query) == true
      }
    }
  }

  when {
    isLoading -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }
    error != null -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Error: $error",
          color = MaterialTheme.colorScheme.error
        )
      }
    }
    filteredWebApps != null -> {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredWebApps!!) { app ->
          WebAppItem(app = app)
        }
      }
    }
  }
}

@Composable
private fun WebAppItem(
  app: WebApp,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.Top
  ) {
    // Material Icon placeholder - we'll use a simple icon for now
    // In a real implementation, you'd map webIcon names to Material Icons
    // For now, using a simple circle as placeholder
    Box(
      modifier = Modifier
        .size(48.dp)
        .padding(end = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "🌐",
        style = MaterialTheme.typography.headlineSmall
      )
    }

    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = app.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = app.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

