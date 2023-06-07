/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.ui

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.ui.AppColors
import com.ivianuu.injekt.Provide

@Provide val apeLabsAppColors = AppColors(
  primary = Color(0xFFEF5777),
  secondary = Color(0xFF0BE881)
)
