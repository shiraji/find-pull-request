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

    fun getRepository(): GitRepository? {
        project ?: return null
        virtualFile ?: return null
        return GithubUtil.getGitRepository(project, virtualFile)
    }

    fun getFileAnnotation(repository: GitRepository): FileAnnotation? {
        virtualFile ?: return null
        return repository.vcs?.annotationProvider?.annotate(virtualFile)
    }

    fun createGithubRepoUrl(repository: GitRepository): String? {
        val remoteUrl: String = GithubUtil.findUpstreamRemote(repository) ?: GithubUtil.findGithubRemoteUrl(repository) ?: return null
        return GithubUrlUtil.makeGithubRepoUrlFromRemoteUrl(remoteUrl, "https://" + GithubUrlUtil.getHostFromUrl(remoteUrl))
    }

    fun createMd5Hash(repository: GitRepository, annotate: FileAnnotation, revisionHash: VcsRevisionNumber): String {
        val revision = annotate.revisions?.single { it.revisionNumber == revisionHash } as GitFileRevision
        return revision.path.path.subtract("${repository.gitDir.parent.presentableUrl.toString()}/").toMd5()
    }

    fun createRevisionHash(annotate: FileAnnotation): VcsRevisionNumber? {
        editor ?: return null
        val lineNumber = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        return annotate.originalRevision(lineNumber)
    }

    fun findPullRequestCommit(repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit? {
        project ?: return null
        return GitHistoryUtils.history(project, repository.root, "$revisionHash..master", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse").firstOrNull()
    }

    fun findMergedCommitdFromPullRequestCommit(repository: GitRepository, pullRequestCommit: GitCommit)
            = GitHistoryUtils.history(project!!, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}")
}