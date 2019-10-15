package com.github.shiraji.findpullrequest.model

import com.github.shiraji.getNumberFromCommitMessage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository

class GitHistoryService {

    fun findLatestMergeCommit(project: Project): GitCommit? {
        val basePath = project.basePath ?: return null
        val root = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
        val commit = GitHistoryUtils.collectTimedCommits(project, root, "--merges", "-1").firstOrNull() ?: return null
        return GitHistoryUtils.history(project, root, commit.id.asString(), "-1").firstOrNull()
    }

    fun findCommitLog(project: Project, repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit {
        return GitHistoryUtils.history(project, repository.root, "$revisionHash", "-1").first()
    }

    fun findMergedCommit(project: Project, repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit? {
        // See https://stackoverflow.com/questions/8475448/find-merge-commit-which-include-a-specific-commit
        val ancestryPathCommits = GitHistoryUtils.collectTimedCommits(
            project,
            repository.root,
            "$revisionHash..HEAD",
            "--merges",
            "--ancestry-path"
        )
        val firstParentsCommits = GitHistoryUtils.collectTimedCommits(
            project,
            repository.root,
            "$revisionHash..HEAD",
            "--merges",
            "--first-parent"
        )
        val commit = ancestryPathCommits.lastOrNull { firstParentsCommits.contains(it) } ?: return null
        return GitHistoryUtils.history(project, repository.root, commit.id.asString(), "-1").firstOrNull()
    }

    fun listCommitsFromMergedCommit(project: Project, repository: GitRepository, pullRequestCommit: GitCommit): List<GitCommit> {
        return GitHistoryUtils.history(project, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}")
    }

    fun hasCommitsFromRevisionNumber(commits: List<GitCommit>, revisionHash: VcsRevisionNumber): Boolean {
        return commits.any { it.id.asString() == revisionHash.asString() }
    }

    fun findPrNumberAndHostingService(pullRequestCommit: GitCommit): Pair<Int?, FindPullRequestHostingServices?> {
        var prNumber: Int? = null
        val targetHostingService = FindPullRequestHostingServices.values().firstOrNull {
            prNumber = pullRequestCommit.getNumberFromCommitMessage(it.defaultMergeCommitMessage)
            prNumber != null
        }
        return Pair(prNumber, targetHostingService)
    }
}