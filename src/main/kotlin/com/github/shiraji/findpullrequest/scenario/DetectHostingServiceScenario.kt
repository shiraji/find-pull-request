package com.github.shiraji.findpullrequest.scenario

import com.github.shiraji.findpullrequest.config.hasHosting
import com.github.shiraji.findpullrequest.config.setHosting
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.findpullrequest.service.GitHistoryService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import org.koin.core.KoinComponent
import org.koin.core.inject

class DetectHostingServiceScenario : KoinComponent {
    private val project: Project by inject()
    private val config: PropertiesComponent by inject()
    private val gitHistoryService: GitHistoryService by inject()
    private val gitRepositoryManager: GitRepositoryManager by inject()

    fun saveHostingService() {
        if (config.hasHosting()) return
        if (gitRepositoryManager.repositories.isEmpty()) return
        val root = project.root ?: return
        val mergeCommit = gitHistoryService.findLatestMergeCommit(project, root) ?: return
        // Default is GitHub
        val hostingService = HostingService.findFromMergeCommitMessage(mergeCommit.fullMessage) ?: HostingService.GitHub
        config.setHosting(hostingService)
    }
}