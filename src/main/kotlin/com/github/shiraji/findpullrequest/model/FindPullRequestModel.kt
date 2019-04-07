package com.github.shiraji.findpullrequest.model

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository


class FindPullRequestModel(
        private val project: Project,
        private val editor: Editor,
        private val virtualFile: VirtualFile,
        private val gitRepositoryService: GitRepositoryService,
        private val gitRepositoryUrlService: GitRepositoryUrlService,
        private val config: PropertiesComponent = PropertiesComponent.getInstance(project)
) {

    fun isEnable(
            repository: GitRepository,
            changeListManager: ChangeListManager = ChangeListManager.getInstance(project)
    ): Boolean {
        if (config.isDisable()) return false
        if (project.isDisposed) return false
        if (!gitRepositoryService.hasOriginOrUpstreamRepository(repository)) return false
        if (changeListManager.isUnversioned(virtualFile)) return false
        changeListManager.getChange(virtualFile)?.let {
            if (it.type == Change.Type.NEW) return false
        }
        return editor.isPointSingleLine()
    }

    private fun Editor.isPointSingleLine() = getLine(selectionModel.selectionStart) == getLine(selectionModel.selectionEnd)

    private fun Editor.getLine(offset: Int) = document.getLineNumber(offset)

    fun getFileAnnotation(repository: GitRepository) = repository.vcs?.annotationProvider?.annotate(virtualFile)

    fun createRevisionHash(annotate: FileAnnotation): VcsRevisionNumber? {
        val lineNumber = editor.getLine(editor.selectionModel.selectionStart)
        return annotate.originalRevision(lineNumber)
    }

    fun createPullRequestPath(repository: GitRepository, revisionHash: VcsRevisionNumber): String {
        val debugMessage = StringBuilder()
        if (config.isDebugMode()) {
            debugMessage
                    .appendln("### Revision hash:")
                    .appendln(revisionHash.asString())
        }

        fun findCommitLog(repository: GitRepository, revisionHash: VcsRevisionNumber)
                = GitHistoryUtils.history(project, repository.root, "$revisionHash").first().also {
            if (config.isDebugMode()) {
                debugMessage.appendln("### Squash PR commit:")
                debugMessage.appendln(it.id.asString())
            }
        }

        fun findClosestPullRequestCommit(repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit? {
            // I think there is a bug in history() since it does not keep the order correctly
            // It seems GitLogUtil#readFullDetails is the place that store the results in list
            val results = GitHistoryUtils.history(project, repository.root, "$revisionHash..HEAD", "--merges", "--ancestry-path", "--reverse")
            val result = results.minBy { it.commitTime }
            if (config.isDebugMode()) {
                debugMessage.appendln("### PR commit:")
                debugMessage.appendln(result?.id?.asString())
            }
            return result
        }

        fun listCommitsFromMergedCommit(repository: GitRepository, pullRequestCommit: GitCommit)
                = GitHistoryUtils.history(project, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}").also {
            if (config.isDebugMode()) {
                debugMessage.appendln("### Merged commits lists:")
                it.forEach { commit -> debugMessage.appendln(commit.id.asString()) }
            }
        }

        fun hasCommitsFromRevisionNumber(commits: List<GitCommit>, revisionHash: VcsRevisionNumber)
                = commits.any { it.id.asString() == revisionHash.asString() }.also {
            if (config.isDebugMode()) {
                debugMessage.appendln("### Result of `hasCommitsFromRevisionNumber`:")
                debugMessage.appendln(it)
            }
        }

        fun createUrl(hostingServices: FindPullRequestHostingServices, path: String): String {
            return if (config.isJumpToFile()) {
                val fileAnnotation = getFileAnnotation(repository) ?: return path
                path + hostingServices.createFileAnchorValue(repository, fileAnnotation)
            } else {
                path
            }
        }

        val pullRequestCommit = findClosestPullRequestCommit(repository, revisionHash)

        return if (pullRequestCommit != null && hasCommitsFromRevisionNumber(listCommitsFromMergedCommit(repository, pullRequestCommit), revisionHash)) {
            val hosting = FindPullRequestHostingServices.findBy(config.getHosting())
            val prNumberUsingConfig = pullRequestCommit.getNumberFromCommitMessage(hosting.defaultMergeCommitMessage)

            val (prNumber, targetHostingService) = if (prNumberUsingConfig == null) {
                // Check if the merge commit message comes from other supporting hosting service
                findPrNumberAndHostingService(pullRequestCommit)
            } else {
                Pair(prNumberUsingConfig, hosting)
            }

            if (prNumber == null || targetHostingService == null) {
                throw NoPullRequestFoundException(debugMessage.toString())
            }

            val path = targetHostingService.urlPathFormat.format(prNumber)
            createUrl(targetHostingService, path)
        } else {
            val commit = findCommitLog(repository, revisionHash)
            val hostingServices = FindPullRequestHostingServices.values().firstOrNull {
                commit.isSquashPullRequestCommit(it)
            }

            if (hostingServices != null) {
                val path = hostingServices.urlPathFormat.format(commit.getNumberFromCommitMessage(hostingServices.squashCommitMessage))
                createUrl(hostingServices, path)
            } else {
                throw NoPullRequestFoundException(debugMessage.toString())
            }
        }
    }

    private fun findPrNumberAndHostingService(pullRequestCommit: GitCommit): Pair<Int?, FindPullRequestHostingServices?> {
        var prNumber: Int? = null
        val targetHostingService = FindPullRequestHostingServices.values().firstOrNull {
            prNumber = pullRequestCommit.getNumberFromCommitMessage(it.defaultMergeCommitMessage)
            prNumber != null
        }
        return Pair(prNumber, targetHostingService)
    }

    private fun GitCommit.isSquashPullRequestCommit(hostingServices: FindPullRequestHostingServices): Boolean {
        return hostingServices.squashCommitMessage.containsMatchIn(this.fullMessage)
    }

    private fun GitCommit.getNumberFromCommitMessage(commitMessageTemplate: Regex): Int? {
        return commitMessageTemplate.find(this.fullMessage)?.groups?.get(1)?.value?.toInt()
    }

    fun createWebRepoUrl(repository: GitRepository): String? {
        val remoteUrl: String = gitRepositoryService.findUpstreamUrl(repository) ?: gitRepositoryService.findOriginUrl(repository) ?: return null
        val host = gitRepositoryUrlService.getHostFromUrl(remoteUrl)
        val username = gitRepositoryUrlService.getUserFromRemoteUrl(remoteUrl)
        val repositoryName = gitRepositoryUrlService.getRepositoryFromRemoteUrl(remoteUrl)
        if (username.isNullOrBlank() || repositoryName.isNullOrBlank()) return null
        return "${config.getProtocol()}$host/$username/$repositoryName"
    }
}