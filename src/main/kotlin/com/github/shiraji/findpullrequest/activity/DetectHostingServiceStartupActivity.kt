package com.github.shiraji.findpullrequest.activity

import com.github.shiraji.findpullrequest.model.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class DetectHostingServiceStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val gitHistoryService = GitHistoryService()
        val mergeCommit = gitHistoryService.findLatestMergeCommit(project)
        val pair = gitHistoryService.findPrNumberAndHostingService(mergeCommit)
        val hostingService = pair.second ?: return
        val config = PropertiesComponent.getInstance(project)
        config.setHosting(hostingService)
    }
}