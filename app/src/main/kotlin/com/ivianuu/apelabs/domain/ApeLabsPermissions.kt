/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.domain

import android.Manifest
import com.ivianuu.essentials.app.AppForegroundScope
import com.ivianuu.essentials.app.ScopeWorker
import com.ivianuu.essentials.permission.PermissionManager
import com.ivianuu.essentials.permission.runtime.RuntimePermission
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.typeKeyOf

@Provide class ApeLabsBluetoothConnectPermission : RuntimePermission(
  permissionName = Manifest.permission.BLUETOOTH_CONNECT,
  title = "Bluetooth connect"
)

@Provide
class ApeLabsBluetoothScanPermission : RuntimePermission(
  permissionName = Manifest.permission.BLUETOOTH_SCAN,
  title = "Bluetooth scan"
)

@Provide
class ApeLabsLocationPermission : RuntimePermission(
  permissionName = Manifest.permission.ACCESS_FINE_LOCATION,
  title = "Location"
)

val apeLabsPermissionKeys = listOf(
  typeKeyOf<ApeLabsBluetoothConnectPermission>(),
  typeKeyOf<ApeLabsBluetoothScanPermission>(),
  typeKeyOf<ApeLabsLocationPermission>()
)

// always request permissions when launching the ui
@Provide fun apeLabsPermissionRequestWorker(permissionManager: PermissionManager) =
  ScopeWorker<AppForegroundScope> {
    permissionManager.requestPermissions(apeLabsPermissionKeys)
  }
