/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.webapps

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebApp(
  @SerialName("entry")
  val entry: String,
  val name: String,
  val description: String,
  @SerialName("web_icon")
  val webIcon: String,
  val category: String? = null,
  val type: String? = null
)

