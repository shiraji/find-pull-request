@file:JvmName("Config")

package com.github.shiraji.findpullrequest.config

import com.github.shiraji.findpullrequest.domain.HostingService
import com.intellij.ide.util.PropertiesComponent

private const val DISABLE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DISABLE"
private const val DEBUG_MODE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DEBUG_MODE"
private const val JUMP_TO_FILE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.JUMP_TO_FILE"
private const val PROTOCOL = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.PROTOCOL"
private const val POPUP_AFTER_COPY = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.POPUP_AFTER_COPY"
private const val HOSTING = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.HOSTING"

fun PropertiesComponent.isDisable() = getBoolean(DISABLE, false)
fun PropertiesComponent.setDisable(value: Boolean) = setValue(DISABLE, value, false)

fun PropertiesComponent.isDebugMode() = getBoolean(DEBUG_MODE, false)
fun PropertiesComponent.setDebugMode(value: Boolean) = setValue(DEBUG_MODE, value, false)

fun PropertiesComponent.isJumpToFile() = getBoolean(JUMP_TO_FILE, true)
fun PropertiesComponent.setJumpToFile(value: Boolean) = setValue(JUMP_TO_FILE, value, true)

fun PropertiesComponent.getProtocol() = getValue(PROTOCOL, "https://")
fun PropertiesComponent.setProtocol(value: String) = setValue(PROTOCOL, value, "https://")

fun PropertiesComponent.isPopupAfterCopy() = getBoolean(POPUP_AFTER_COPY, false)
fun PropertiesComponent.setPopupAfterCopy(value: Boolean) = setValue(POPUP_AFTER_COPY, value, false)

fun PropertiesComponent.hasHosting(): Boolean = getHosting() != ""
fun PropertiesComponent.getHosting() = getValue(HOSTING, "")
fun PropertiesComponent.setHosting(hostingServices: HostingService) = setValue(HOSTING, hostingServices.name)