package com.ivianuu.apelabs.data

import java.util.*
import java.util.regex.Pattern

private val UUIDPattern =
  Pattern.compile("[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}$")

val String.isUUID: Boolean
  get() = UUIDPattern.matcher(this).matches()

fun randomId() = UUID.randomUUID().toString()
