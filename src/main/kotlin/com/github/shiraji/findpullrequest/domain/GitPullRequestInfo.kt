package com.github.shiraji.findpullrequest.domain

import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import git4idea.GitRevisionNumber

data class GitPullRequestInfo(
    val revisionNumber: GitRevisionNumber,
    val prNumber: Int?,
    val hostingServices: FindPullRequestHostingServices?
)