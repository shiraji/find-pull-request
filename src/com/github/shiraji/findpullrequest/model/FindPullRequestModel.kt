package com.github.shiraji.findpullrequest.model

import com.github.shiraji.getPullRequestNumber
import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitCommit
import git4idea.GitFileRevision
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import org.jetbrains.plugins.github.util.GithubUrlUtil
import org.jetbrains.plugins.github.util.GithubUtil

class FindPullRequestModel {
    val project: Project?
    val editor: Editor?
    val virtualFile: VirtualFile?

    constructor(e: AnActionEvent) {
        project = e.getData(CommonDataKeys.PROJECT)
        editor = e.getData(CommonDataKeys.EDITOR)
        virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
    }

    fun isEnable(): Boolean {
        if (project == null || project.isDisposed || editor == null || virtualFile == null) {
            return false
        }
        val startLine = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        val endLine = editor.document.getLineNumber(editor.selectionModel.selectionEnd)
        return startLine == endLine
    }

    fun findPullRequestUrlOrCommitUrl(): String? {
        if (project == null || editor == null || virtualFile == null) {
            return null
        }
        val repository = GithubUtil.getGitRepository(project, virtualFile) ?: return null
        val annotate = repository.vcs?.annotationProvider?.annotate(virtualFile) ?: return null
        val lineNumber = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        val revisionHash = annotate.originalRevision(lineNumber) ?: return null
        val githubRepoUrl = createGithubRepoUrl(repository) ?: return null
        val pullRequestCommit = findPullRequestCommit(repository, revisionHash)
        var path = if (pullRequestCommit == null) {
            "commit/$revisionHash"
        } else {
            "pull/${pullRequestCommit.getPullRequestNumber()}/files"
        }
        return "$githubRepoUrl/$path#diff-${createMd5Hash(repository, annotate, revisionHash)}"
    }

    fun isPullRequestUrl(url: String): Boolean {
        return url.contains("pull/")
    }

    private fun createGithubRepoUrl(repository: GitRepository): String? {
        val remoteUrl: String = GithubUtil.findUpstreamRemote(repository) ?: GithubUtil.findGithubRemoteUrl(repository) ?: return null
        return GithubUrlUtil.makeGithubRepoUrlFromRemoteUrl(remoteUrl, "https://" + GithubUrlUtil.getHostFromUrl(remoteUrl))
    }

    private fun createMd5Hash(repository: GitRepository, annotate: FileAnnotation, revisionHash: VcsRevisionNumber): String {
        val revision = annotate.revisions?.single { it.revisionNumber == revisionHash } as GitFileRevision
        return revision.path.path.subtract("${repository.gitDir.parent.presentableUrl.toString()}/").toMd5()
    }

    private fun findPullRequestCommit(repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit? {
        if(project == null) return null
        try {
            // pull request commit can be found `git log hash..master --grep="Merge pull request" --merges --ancestry-path --reverse`
            val pullRequestCommit = GitHistoryUtils.history(project, repository.root, "$revisionHash..master", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse").firstOrNull() ?: return null

            // List all commits that are merged when pull request commits are merged. The command is `git log hash^..hash`
            val mergedCommits = GitHistoryUtils.history(project, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}")

            if (mergedCommits.filter { it.id.asString() == revisionHash.asString() }.size != 0) {
                return pullRequestCommit
            }
        } catch (e: VcsException) {
        }
        return null
    }
}