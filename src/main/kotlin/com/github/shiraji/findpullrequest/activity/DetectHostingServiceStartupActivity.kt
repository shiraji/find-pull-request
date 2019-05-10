package com.github.shiraji.findpullrequest.activity

import com.github.shiraji.findpullrequest.model.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import git4idea.GitUtil

class DetectHostingServiceStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val config = PropertiesComponent.getInstance(project)
        if (config.hasHosting()) return
        if (GitUtil.getRepositoryManager(project).repositories.isEmpty()) return
        val gitHistoryService = GitHistoryService()
        val mergeCommit = gitHistoryService.findLatestMergeCommit(project) ?: return
        val hostingService = gitHistoryService.findPrNumberAndHostingService(mergeCommit).second ?: return
        config.setHosting(hostingService)
    }
}