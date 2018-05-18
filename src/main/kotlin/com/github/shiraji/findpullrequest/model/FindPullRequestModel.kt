package com.github.shiraji.findpullrequest.model

import com.github.shiraji.*
import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
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
import org.jetbrains.plugins.github.util.GithubUrlUtil
import org.jetbrains.plugins.github.util.GithubUtil

class FindPullRequestModel(
        private val project: Project,
        private val editor: Editor,
        private val virtualFile: VirtualFile
) {

    fun isEnable(
            repository: GitRepository,
            changeListManager: ChangeListManager = ChangeListManager.getInstance(project)
    ): Boolean {
        if (project.isDisposed) return false

        if (!GithubUtil.isRepositoryOnGitHub(repository)) return false

        if (changeListManager.isUnversioned(virtualFile)) return false

        val change = changeListManager.getChange(virtualFile)
        if (change != null && change.type == Change.Type.NEW) return false

        val startLine = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        val endLine = editor.document.getLineNumber(editor.selectionModel.selectionEnd)
        return startLine == endLine
    }

    fun getFileAnnotation(repository: GitRepository): FileAnnotation? {
        return repository.vcs?.annotationProvider?.annotate(virtualFile)
    }

    fun createGithubRepoUrl(repository: GitRepository): String? {
        val remoteUrl: String = GithubUtil.findUpstreamRemote(repository) ?: GithubUtil.findGithubRemoteUrl(repository) ?: return null
        return GithubUrlUtil.makeGithubRepoUrlFromRemoteUrl(remoteUrl, "https://" + GithubUrlUtil.getHostFromUrl(remoteUrl))
    }

    fun createFileMd5Hash(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.baseDir.canonicalPath?.plus("/") ?: return null
        return annotate.file?.canonicalPath?.subtract(projectDir)?.toMd5()
    }

    fun createRevisionHash(annotate: FileAnnotation): VcsRevisionNumber? {
        val lineNumber = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        return annotate.originalRevision(lineNumber)
    }

    fun createPullRequestPath(repository: GitRepository, revisionHash: VcsRevisionNumber): String {
        val debugMessage = StringBuilder()
                .appendln("### Revision hash:")
                .appendln(revisionHash.asString())

        fun findCommitLog(repository: GitRepository, revisionHash: VcsRevisionNumber)
                = GitHistoryUtils.history(project, repository.root, "$revisionHash").first().also {
            debugMessage.appendln("### Squash PR commit:")
            debugMessage.appendln(it.id.asString())
        }

        fun findPullRequestCommit(repository: GitRepository, revisionHash: VcsRevisionNumber)
                = GitHistoryUtils.history(project, repository.root, "$revisionHash..HEAD", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse").firstOrNull().also {
            debugMessage.appendln("### PR commit:")
            debugMessage.appendln(it?.id?.asString())
        }

        fun listCommitsFromMergedCommit(repository: GitRepository, pullRequestCommit: GitCommit)
                = GitHistoryUtils.history(project, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}").also {
            debugMessage.appendln("### Merged commits lists:")
            it.forEach { debugMessage.appendln(it.id.asString()) }
        }

        fun hasCommitsFromRevisionNumber(commits: List<GitCommit>, revisionHash: VcsRevisionNumber)
                = commits.any { it.id.asString() == revisionHash.asString() }.also {
            debugMessage.appendln("### Result of `hasCommitsFromRevisionNumber`:")
            debugMessage.appendln(it)
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
}