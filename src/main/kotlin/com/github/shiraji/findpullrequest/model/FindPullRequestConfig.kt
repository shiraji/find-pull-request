package com.github.shiraji.findpullrequest.model

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

object FindPullRequestConfig {
    private const val DISABLE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DISABLE"
    private const val DEBUG_MODE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.DEBUG_MODE"
    private const val JUMP_TO_FILE = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.JUMP_TO_FILE"
    private const val PROTOCOL = "com.github.shiraji.findpullrequest.model.FindPullRequestConfig.PROTOCOL"

    @JvmStatic
    fun isDebugMode(project: Project) = PropertiesComponent.getInstance(project).getBoolean(DEBUG_MODE, false)

    @JvmStatic
    fun setDebugMode(value: Boolean, project: Project) = PropertiesComponent.getInstance(project).setValue(DEBUG_MODE, value, false)

    @JvmStatic
    fun isJumpToFile(project: Project) = PropertiesComponent.getInstance(project).getBoolean(JUMP_TO_FILE, true)

    @JvmStatic
    fun setJumpToFile(value: Boolean, project: Project) = PropertiesComponent.getInstance(project).setValue(JUMP_TO_FILE, value, true)

    @JvmStatic
    fun getProtocol(project: Project) = PropertiesComponent.getInstance(project).getValue(PROTOCOL, "https://")

    @JvmStatic
    fun setProtocol(value: String, project: Project) = PropertiesComponent.getInstance(project).setValue(PROTOCOL, value, "https://")

}