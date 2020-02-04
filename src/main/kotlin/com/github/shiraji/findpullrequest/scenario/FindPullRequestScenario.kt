package com.github.shiraji.findpullrequest.scenario

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.config.isDisable
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.domain.PathInfo
import com.github.shiraji.findpullrequest.exceptions.NoRevisionFoundException
import com.github.shiraji.findpullrequest.service.CreateUrlService
import com.github.shiraji.findpullrequest.service.FindPrInfoService
import com.github.shiraji.findpullrequest.service.GitHistoryService
import com.github.shiraji.findpullrequest.service.GitRepositoryService
import com.github.shiraji.findpullrequest.service.GitUrlService
import com.github.shiraji.findpullrequest.service.UpToDateLineNumberProviderService
import com.github.shiraji.isPointSingleLine
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import org.koin.core.KoinComponent
import org.koin.core.inject

class FindPullRequestScenario : KoinComponent {
    private val project: Project by inject()
    private val config: PropertiesComponent by inject()
    private val editor: Editor by inject()
    private val virtualFile: VirtualFile by inject()
    private val gitHistoryService: GitHistoryService by inject()
    private val lineNumber: Int by inject()
    private val gitRepositoryService: GitRepositoryService by inject()
    private val upToDateLineNumberProviderService: UpToDateLineNumberProviderService by inject()
    private val createUrlService: CreateUrlService by inject()
    private val findPrInfoService: FindPrInfoService by inject()
    private val gitUrlService: GitUrlService by inject()
    private val changeListManager: ChangeListManager by inject()

    fun findPrUrl(): String {
        val lineRevision =
            getLineRevision() ?: throw NoRevisionFoundException("No revision found at line $lineNumber")
        val mergeCommit = gitHistoryService.findMergedCommit(lineRevision)
        val prInfo = findPrInfoService.findPrInfo(lineRevision, mergeCommit)
        return createUrlService.createUrl(prInfo)
    }

    fun getLineRevision(): VcsRevisionNumber? {
        val fileAnnotation = gitRepositoryService.getFileAnnotation(virtualFile)
        return upToDateLineNumberProviderService.getLineRevision(fileAnnotation, lineNumber)
    }

    fun isEnabled(): Boolean {
        if (project.isDisposed) return false
        if (config.isDisable()) return false
        if (!gitUrlService.hasOriginOrUpstreamRepository()) return false
        if (changeListManager.isUnversioned(virtualFile)) return false
        changeListManager.getChange(virtualFile)?.let {
            if (it.type == Change.Type.NEW) return false
        }
        return editor.isPointSingleLine()
    }

    fun findCommitUrl(): String {
        val lineRevision = getLineRevision() ?: throw NoRevisionFoundException("No revision found")
        return createUrlService.createUrl(
            PathInfo.Commit(
                revisionNumber = lineRevision,
                hostingService = HostingService.findBy(config.getHosting())
            )
        )
    }
}