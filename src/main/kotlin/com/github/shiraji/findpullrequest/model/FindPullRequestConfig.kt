package com.github.shiraji.findpullrequest.model

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

object FindPullRequestConfig {
    private const val DISABLE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DISABLE"
    private const val DEBUG_MODE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DEBUG_MODE"
    private const val JUMP_TO_FILE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.JUMP_TO_FILE"
    private const val PROTOCOL = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.PROTOCOL"

    private fun Project.getConfigBoolean(key: String, defaultValue: Boolean) = PropertiesComponent.getInstance(this).getBoolean(key, defaultValue)
    private fun Project.setConfigBoolean(key: String, value: Boolean, defaultValue: Boolean) = PropertiesComponent.getInstance(this).setValue(key, value, defaultValue)
    private fun Project.getConfigString(key: String, defaultValue: String) = PropertiesComponent.getInstance(this).getValue(key, defaultValue)
    private fun Project.setConfigString(key: String, value: String, defaultValue: String) = PropertiesComponent.getInstance(this).setValue(key, value, defaultValue)

    @JvmStatic
    fun isDisable(project: Project) = project.getConfigBoolean(DISABLE, false)

    @JvmStatic
    fun setDisable(value: Boolean, project: Project) = project.setConfigBoolean(DISABLE, value, false)

    @JvmStatic
    fun isDebugMode(project: Project) = project.getConfigBoolean(DEBUG_MODE, false)

    @JvmStatic
    fun setDebugMode(value: Boolean, project: Project) = project.setConfigBoolean(DEBUG_MODE, value, false)

    @JvmStatic
    fun isJumpToFile(project: Project) = project.getConfigBoolean(JUMP_TO_FILE, true)

    @JvmStatic
    fun setJumpToFile(value: Boolean, project: Project) = project.setConfigBoolean(JUMP_TO_FILE, value, true)

    @JvmStatic
    fun getProtocol(project: Project) = project.getConfigString(PROTOCOL, "https://")

    @JvmStatic
    fun setProtocol(value: String, project: Project) = project.setConfigString(PROTOCOL, value, "https://")

}