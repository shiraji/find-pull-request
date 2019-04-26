package com.github.shiraji.findpullrequest.model

import com.github.shiraji.getNumberFromCommitMessage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository

class GitHistoryService {

    fun findLatestMergeCommit(project: Project): GitCommit {
        val results = GitHistoryUtils.history(project, project.baseDir, "--merges")
        return results.first()
    }

    fun findCommitLog(project: Project, repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit {
        return GitHistoryUtils.history(project, repository.root, "$revisionHash").first()
    }

    fun findClosestPullRequestCommit(project: Project, repository: GitRepository, revisionHash: VcsRevisionNumber): GitCommit? {
        // I think there is a bug in history() since it does not keep the order correctly
        // It seems GitLogUtil#readFullDetails is the place that store the results in list
        val results = GitHistoryUtils.history(project, repository.root, "$revisionHash..HEAD", "--merges", "--ancestry-path", "--reverse")
        return results.minBy { it.commitTime }
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