/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.apelabs.domain

import android.*
import com.ivianuu.essentials.app.*
import com.ivianuu.essentials.permission.*
import com.ivianuu.injekt.*

@Provide class ApeLabsBluetoothConnectPermission : RuntimePermission(
  permissionName = Manifest.permission.BLUETOOTH_CONNECT,
  title = "Bluetooth connect"
)

@Provide class ApeLabsBluetoothScanPermission : RuntimePermission(
  permissionName = Manifest.permission.BLUETOOTH_SCAN,
  title = "Bluetooth scan"
)

@Provide class ApeLabsLocationPermission : RuntimePermission(
  permissionName = Manifest.permission.ACCESS_FINE_LOCATION,
  title = "Location"
)

val apeLabsPermissionKeys = listOf(
  ApeLabsBluetoothConnectPermission::class,
  ApeLabsBluetoothScanPermission::class,
  ApeLabsLocationPermission::class
)

// always request permissions when launching the ui
@Provide fun apeLabsPermissionRequestWorker(permissionManager: PermissionManager) =
  ScopeWorker<AppVisibleScope> {
    permissionManager.requestPermissions(apeLabsPermissionKeys)
  }
