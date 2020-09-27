package com.github.shiraji.findpullrequest.service

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.domain.PathInfo
import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.exceptions.NoRevisionFoundException
import com.github.shiraji.findpullrequest.exceptions.UnsupportedHostingServiceException
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import git4idea.GitCommit

class FindPrInfoService(
    private val config: PropertiesComponent,
    private val gitHistoryService: GitHistoryService
) {
    fun findPrInfo(
        revisionNumber: VcsRevisionNumber,
        mergeCommit: GitCommit?,
        hostingService: HostingService = HostingService.findBy(config.getHosting())
    ): PathInfo.Pr {
        return if (gitHistoryService.isRevisionMergedAtMergeCommit(revisionNumber, mergeCommit)) {
            // merge commit
            checkNotNull(mergeCommit)
            findPrInfoFromMergeCommit(mergeCommit, hostingService)
        } else {
            // squash merge commit
            val gitCommit = gitHistoryService.toGitCommit(revisionNumber)
                ?: throw NoRevisionFoundException("No commit found for: $revisionNumber")
            findPrInfoFromSquashCommit(gitCommit, hostingService)
        }
    }

    private fun findPrInfoFromMergeCommit(
        mergeCommit: GitCommit,
        hostingService: HostingService
    ): PathInfo.Pr {
        val commitMessage = mergeCommit.fullMessage
        val prNumber = hostingService.getPrNumberFromMergeCommit(commitMessage)
        return if (prNumber == null) {
            findPrInfoFromAllHostingServices(commitMessage)
        } else {
            PathInfo.Pr(prNumber, hostingService)
        }
    }

    private fun findPrInfoFromSquashCommit(
        commit: GitCommit,
        hostingService: HostingService
    ): PathInfo.Pr {
        val commitMessage = commit.fullMessage
        val prNumber = hostingService.getPrNumberFromSquashCommit(commitMessage)
        return if (prNumber == null) {
            findPrInfoUsingSquashCommitFromAllHostingServices(commitMessage)
        } else {
            PathInfo.Pr(prNumber, hostingService)
        }
    }

    private fun findPrInfoUsingSquashCommitFromAllHostingServices(
        commitFullMessage: String
    ): PathInfo.Pr {
        val hostingService = HostingService.findFromSquashCommitMessage(commitFullMessage)
            ?: throw UnsupportedHostingServiceException("Cannot guess hosting service. Commit message: $commitFullMessage")
        val prNumber = hostingService.getPrNumberFromSquashCommit(commitFullMessage)
            ?: throw NoPullRequestFoundException("No pull request information found from this commit message: $commitFullMessage")
        return PathInfo.Pr(prNumber, hostingService)
    }

    private fun findPrInfoFromAllHostingServices(commitFullMessage: String): PathInfo.Pr {
        val hostingService = HostingService.findFromMergeCommitMessage(commitFullMessage)
            ?: throw UnsupportedHostingServiceException("Cannot guess hosting service. Commit message: $commitFullMessage")
        val prNumber = hostingService.getPrNumberFromMergeCommit(commitFullMessage)
            ?: throw NoPullRequestFoundException("No pull request information found from the merged commit message: $commitFullMessage")
        return PathInfo.Pr(prNumber, hostingService)
    }
}