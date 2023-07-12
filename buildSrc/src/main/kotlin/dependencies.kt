/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ClassName", "unused")

object Build {
  const val applicationId = "com.ivianuu.apelabs"
  const val compileSdk = 33
  const val minSdk = 32
  const val targetSdk = 31
  const val versionCode = 1
  const val versionName = "0.0.1"
}

object Deps {
  object Essentials {
    private const val version = "0.0.1-dev1205"
    const val android = "com.ivianuu.essentials:android:$version"
    const val backup = "com.ivianuu.essentials:backup:$version"
    const val gradlePlugin = "com.ivianuu.essentials:gradle-plugin:$version"
    const val permission = "com.ivianuu.essentials:permission:$version"
    const val rubik = "com.ivianuu.essentials:rubik:$version"
  }
}
