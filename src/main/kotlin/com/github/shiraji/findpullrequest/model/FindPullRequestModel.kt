package com.github.shiraji.findpullrequest.model

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.getNumberFromCommitMessage
import com.github.shiraji.isPointSingleLine
import com.github.shiraji.isSquashPullRequestCommit
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository

class FindPullRequestModel(
    private val project: Project,
    private val editor: Editor,
    private val virtualFile: VirtualFile,
    private val gitConfService: GitConfService,
    private val gitRepositoryUrlService: GitRepositoryUrlService,
    private val gitHistoryService: GitHistoryService,
    private val config: PropertiesComponent = PropertiesComponent.getInstance(project)
) {

    fun isEnable(
        repository: GitRepository,
        changeListManager: ChangeListManager = ChangeListManager.getInstance(project)
    ): Boolean {
        if (config.isDisable()) return false
        if (project.isDisposed) return false
        if (!gitConfService.hasOriginOrUpstreamRepository(repository)) return false
        if (changeListManager.isUnversioned(virtualFile)) return false
        changeListManager.getChange(virtualFile)?.let {
            if (it.type == Change.Type.NEW) return false
        }
        return editor.isPointSingleLine()
    }

    fun createCommitUrl(repository: GitRepository, hostingServices: FindPullRequestHostingServices, webRepoUrl: String, revisionHash: VcsRevisionNumber): String {
        val path = hostingServices.commitPathFormat.format(webRepoUrl, revisionHash)
        return createUrl(repository, hostingServices, path)
    }

    fun createPullRequestPath(repository: GitRepository, revisionHash: VcsRevisionNumber): String {
        val debugMessage = StringBuilder()
        if (config.isDebugMode()) {
            debugMessage
                    .appendln("### Revision hash:")
                    .appendln(revisionHash.asString())
        }

        val pullRequestCommit = gitHistoryService.findMergedCommit(project, repository, revisionHash)

        return if (pullRequestCommit != null && gitHistoryService.hasCommitsFromRevisionNumber(gitHistoryService.listCommitsFromMergedCommit(project, repository, pullRequestCommit), revisionHash)) {
            val hosting = FindPullRequestHostingServices.findBy(config.getHosting())
            val prNumberUsingConfig = pullRequestCommit.getNumberFromCommitMessage(hosting.defaultMergeCommitMessage)

            val (prNumber, targetHostingService) = if (prNumberUsingConfig == null) {
                // Check if the merge commit message comes from other supporting hosting service
                gitHistoryService.findPrNumberAndHostingService(pullRequestCommit)
            } else {
                Pair(prNumberUsingConfig, hosting)
            }

            if (prNumber == null || targetHostingService == null) {
                throw NoPullRequestFoundException(debugMessage.toString())
            }

            val path = targetHostingService.urlPathFormat.format(prNumber)
            createUrl(repository, targetHostingService, path)
        } else {
            val commit = gitHistoryService.findCommitLog(project, repository, revisionHash)
            val hostingServices = FindPullRequestHostingServices.values().firstOrNull {
                commit.isSquashPullRequestCommit(it)
            }

            if (hostingServices != null) {
                val path = hostingServices.urlPathFormat.format(commit.getNumberFromCommitMessage(hostingServices.squashCommitMessage))
                createUrl(repository, hostingServices, path)
            } else {
                throw NoPullRequestFoundException(debugMessage.toString())
            }
        }
    }

    fun createUrl(repository: GitRepository, hostingServices: FindPullRequestHostingServices, path: String): String {
        return if (config.isJumpToFile()) {
            val fileAnnotation = gitConfService.getFileAnnotation(repository, virtualFile) ?: return path
            path + hostingServices.createFileAnchorValue(repository, fileAnnotation)
        } else {
            path
        }
    }

    fun createWebRepoUrl(repository: GitRepository): String? {
        val remoteUrl: String = gitConfService.findUpstreamUrl(repository) ?: gitConfService.findOriginUrl(repository) ?: return null
        val host = gitRepositoryUrlService.getHostFromUrl(remoteUrl)
        val username = gitRepositoryUrlService.getUserFromRemoteUrl(remoteUrl)
        val repositoryName = gitRepositoryUrlService.getRepositoryFromRemoteUrl(remoteUrl)
        if (username.isNullOrBlank() || repositoryName.isNullOrBlank()) return null
        return "${config.getProtocol()}$host/$username/$repositoryName"
    }
}