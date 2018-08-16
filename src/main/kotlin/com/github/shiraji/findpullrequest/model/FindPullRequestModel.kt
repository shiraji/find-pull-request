package com.github.shiraji.findpullrequest.model

import com.github.shiraji.*
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
        private val config: PropertiesComponent = PropertiesComponent.getInstance(project)

) {

    /**
     * The expected remote url format is
     * * https://[HOST]/[USER]/[REPO].git
     * * git@[HOST]:[USER]/[REPO].git
     */
    private val repoUserRegex = Regex(".*[/:](.*)/(.*).git")

    fun isEnable(
            repository: GitRepository,
            changeListManager: ChangeListManager = ChangeListManager.getInstance(project)
    ): Boolean {
        if (config.isDisable()) return false
        if (project.isDisposed) return false
        if (!hasOriginOrUpstreamRepository(repository)) return false
        if (changeListManager.isUnversioned(virtualFile)) return false
        changeListManager.getChange(virtualFile)?.let {
            if (it.type == Change.Type.NEW) return false
        }
        return editor.isPointSingleLine()
    }

    private fun Editor.isPointSingleLine() = getLine(selectionModel.selectionStart) == getLine(selectionModel.selectionEnd)

    private fun Editor.getLine(offset: Int) = document.getLineNumber(offset)

    fun getFileAnnotation(repository: GitRepository) = repository.vcs?.annotationProvider?.annotate(virtualFile)

    fun createWebRepoUrl(repository: GitRepository): String? {
        val remoteUrl: String = findUpstreamUrl(repository) ?: findOriginUrl(repository) ?: return null
        return makeWebRemoteRepoUrlFromRemoteUrl(remoteUrl, config.getProtocol())
    }

    fun createFileMd5Hash(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.baseDir.canonicalPath?.plus("/") ?: return null
        return annotate.file?.canonicalPath?.subtract(projectDir)?.toMd5()
    }

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

        fun findPullRequestCommit(repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit? {
            // I think there is a bug in history() since it does not keep the order correctly
            // It seems GitLogUtil#readFullDetails is the place that store the results in list
            val results = GitHistoryUtils.history(project, repository.root, "$revisionHash..HEAD", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse")
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
                it.forEach { debugMessage.appendln(it.id.asString()) }
            }
        }

        fun hasCommitsFromRevisionNumber(commits: List<GitCommit>, revisionHash: VcsRevisionNumber)
                = commits.any { it.id.asString() == revisionHash.asString() }.also {
            if (config.isDebugMode()) {
                debugMessage.appendln("### Result of `hasCommitsFromRevisionNumber`:")
                debugMessage.appendln(it)
            }
        }

        val pullRequestCommit = findPullRequestCommit(repository, revisionHash)

        return if (pullRequestCommit != null && hasCommitsFromRevisionNumber(listCommitsFromMergedCommit(repository, pullRequestCommit), revisionHash)) {
            "pull/${pullRequestCommit.getPullRequestNumber()}/files"
        } else {
            val commit = findCommitLog(repository, revisionHash)
            if (commit.isSquashPullRequestCommit()) {
                "pull/${commit.getPullRequestNumberFromSquashCommit()}/files"
            } else {
                throw NoPullRequestFoundException(debugMessage.toString())
            }
        }
    }

    private fun hasOriginOrUpstreamRepository(repository: GitRepository): Boolean {
        return findOriginUrl(repository) != null || findUpstreamUrl(repository) != null
    }

    private fun findOriginUrl(repository: GitRepository): String? {
        return findRemoteUrl(repository, "origin")
    }

    private fun findUpstreamUrl(repository: GitRepository): String? {
        return findRemoteUrl(repository, "upstream")
    }

    private fun findRemoteUrl(repository: GitRepository, targetRemoteName: String): String? {
        return repository.remotes.firstOrNull { it.name == targetRemoteName }?.firstUrl
    }

    private fun removeProtocolPrefix(url: String): String {
        return if (url.contains("@")) url.substringAfter("@") else url.substringAfter("://")
    }

    private fun getUserAndRepositoryFromRemoteUrl(gitRemoteUrl: String): Pair<String, String>? {
        val (username, repo) = repoUserRegex.find(gitRemoteUrl)?.destructured ?: return null
        if (username.isBlank() || repo.isBlank()) return null
        return Pair(username, repo)
    }

    private fun getHostFromUrl(url: String): String {
        val path = removeProtocolPrefix(url).replace(':', '/')
        return path.substringBefore("/")
    }

    private fun makeWebRemoteRepoUrlFromRemoteUrl(remoteUrl: String, protocol: String): String? {
        val host = getHostFromUrl(remoteUrl)
        val repo = getUserAndRepositoryFromRemoteUrl(remoteUrl) ?: return null
        return "$protocol$host/${repo.first}/${repo.second}"
    }
}