package com.github.shiraji.findpullrequest.service

import com.github.shiraji.findpullrequest.infra.git.GitHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitCommit

class GitHistoryService(private val gitHistory: GitHistory) {

    fun findMergedCommit(revisionHash: VcsRevisionNumber): GitCommit? {
        // See https://stackoverflow.com/questions/8475448/find-merge-commit-which-include-a-specific-commit
        val ancestryPathCommits = gitHistory.findAncestryPathCommits(revisionHash)
        val firstParentsCommits = gitHistory.findFirstParentsCommits(revisionHash)
        val commit = ancestryPathCommits.lastOrNull { firstParentsCommits.contains(it) } ?: return null
        return gitHistory.toGitCommit(commit)
    }

    fun findLatestMergeCommit(project: Project, root: VirtualFile): GitCommit? {
        val commit = gitHistory.findLatestMergeCommit(project, root) ?: return null
        return gitHistory.toGitCommit(commit)
    }

    fun isRevisionMergedAtMergeCommit(revisionHash: VcsRevisionNumber, mergeCommit: GitCommit?): Boolean {
        if (mergeCommit == null) return false
        val mergedCommits = gitHistory.listCommitsFromMergedCommit(mergeCommit)
        return mergedCommits.any { it.id.asString() == revisionHash.asString() }
    }

    fun toGitCommit(revisionHash: VcsRevisionNumber): GitCommit? {
        return gitHistory.toGitCommit(revisionHash)
    }

    @Throws(VcsException::class)
    fun toVcsRevisionNumber(hash: String): VcsRevisionNumber? {
        return gitHistory.toVcsRevisionNumber(hash)
    }

    @Throws(VcsException::class)
    fun findRevisionHashes(virtualFile: VirtualFile): List<String> {
        return gitHistory.findRevisionHashes(virtualFile)
    }
}