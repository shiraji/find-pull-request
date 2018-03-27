package com.github.shiraji.findpullrequest.model

import com.github.shiraji.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
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

class FindPullRequestModel(e: AnActionEvent) {
    private val project: Project? = e.getData(CommonDataKeys.PROJECT)
    private val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
    private val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

    fun isEnable(): Boolean {
        if (project == null || project.isDisposed || editor == null || virtualFile == null) {
            return false
        }

        val repo = getRepository() ?: return false
        if (!GithubUtil.isRepositoryOnGitHub(repo)) return false

        val changeListManager = ChangeListManager.getInstance(project)
        if (changeListManager.isUnversioned(virtualFile)) return false

        val change = changeListManager.getChange(virtualFile)
        if (change != null && change.type == Change.Type.NEW) return false

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

    fun createFileMd5Hash(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.baseDir.canonicalPath?.plus("/") ?: return null
        return annotate.file.canonicalPath?.subtract(projectDir)?.toMd5()
    }

    fun createRevisionHash(annotate: FileAnnotation): VcsRevisionNumber? {
        editor ?: return null
        val lineNumber = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        return annotate.originalRevision(lineNumber)
    }

    private fun findCommitLog(repository: GitRepository, revisionHash: VcsRevisionNumber)
            = GitHistoryUtils.history(project!!, repository.root, "$revisionHash").first()

    fun findPullRequestCommit(repository: GitRepository, revisionHash: VcsRevisionNumber)
            = GitHistoryUtils.history(project!!, repository.root, "$revisionHash..HEAD", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse").firstOrNull()

    private fun listCommitsFromMergedCommit(repository: GitRepository, pullRequestCommit: GitCommit)
            = GitHistoryUtils.history(project!!, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}")

    private fun hasCommitsFromRevisionNumber(commits: List<GitCommit>, revisionHash: VcsRevisionNumber)
            = commits.any { it.id.asString() == revisionHash.asString() }

    fun createPullRequestPathFromCommit(pullRequestCommit: GitCommit?, repository: GitRepository, revisionHash: VcsRevisionNumber): String? {
        return if (pullRequestCommit != null && hasCommitsFromRevisionNumber(listCommitsFromMergedCommit(repository, pullRequestCommit), revisionHash)) {
            "pull/${pullRequestCommit.getPullRequestNumber()}/files"
        } else {
            val commit = findCommitLog(repository, revisionHash)
            if (commit.isSquashPullRequestCommit()) {
                "pull/${commit.getPullRequestNumberFromSquashCommit()}/files"
            } else {
                null
            }
        }
    }
}