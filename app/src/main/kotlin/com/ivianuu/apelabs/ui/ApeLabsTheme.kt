/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.ui

import androidx.compose.ui.graphics.Color
import com.ivianuu.essentials.rubik.Rubik
import com.ivianuu.essentials.ui.AppTheme
import com.ivianuu.essentials.ui.material.EsTheme
import com.ivianuu.essentials.ui.material.EsTypography
import com.ivianuu.essentials.ui.material.LightAndDarkColors
import com.ivianuu.essentials.ui.material.editEach
import com.ivianuu.injekt.Provide

object ApeLabsTheme {
  val Primary = Color(0xFFEF5777)
  val Secondary = Color(0xFF0BE881)
}

@Provide val apeLabsTheme = AppTheme { content ->
  EsTheme(
    colors = LightAndDarkColors(
      primary = ApeLabsTheme.Primary,
      secondary = ApeLabsTheme.Secondary
    ),
    typography = EsTypography.editEach { copy(fontFamily = Rubik) },
    content = content
  )
}
