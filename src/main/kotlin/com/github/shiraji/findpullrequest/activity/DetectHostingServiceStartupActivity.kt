package com.github.shiraji.findpullrequest.activity

import com.github.shiraji.findpullrequest.model.GitHistoryService
import com.github.shiraji.findpullrequest.model.hasHosting
import com.github.shiraji.findpullrequest.model.setHosting
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import git4idea.GitUtil

class DetectHostingServiceStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val config = PropertiesComponent.getInstance(project)
        if (config.hasHosting()) return
        if (GitUtil.getRepositoryManager(project).repositories.isEmpty()) return
        val gitHistoryService = GitHistoryService()

        object : Task.Backgroundable(project, "Reading git setting...") {
            override fun run(indicator: ProgressIndicator) {
                val mergeCommit = gitHistoryService.findLatestMergeCommit(project) ?: return
                val hostingService = gitHistoryService.findPrNumberAndHostingService(mergeCommit).second ?: return
                config.setHosting(hostingService)
            }
        }
    }
}