package com.github.shiraji.findpullrequest.model

import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.github.shiraji.toSHA1
import com.github.shiraji.toSHA256
import com.intellij.icons.AllIcons
import com.intellij.openapi.vcs.annotate.FileAnnotation
import git4idea.repo.GitRepository
import icons.FindPullRequestIcons
import javax.swing.Icon

enum class FindPullRequestHostingServices(
    val defaultMergeCommitMessage: Regex,
    val squashCommitMessage: Regex,
    val urlPathFormat: String,
    val urlPathForDiff: String,
    val commitPathFormat: String,
    val pullRequestName: String,
    val icon: Icon
) {
    GitHub(
        defaultMergeCommitMessage = "Merge pull request #(\\d*)".toRegex(),
        squashCommitMessage = ".*\\(#(\\d*)\\)".toRegex(),
        urlPathFormat = "pull/%d",
        urlPathForDiff = "/files",
        commitPathFormat = "%s/commit/%s",
        pullRequestName = "Pull Request",
        icon = AllIcons.Vcs.Vendors.Github
    ),
    GitLab(
        defaultMergeCommitMessage = "See merge request .*!(\\d*)".toRegex(),
        squashCommitMessage = "See merge request .*!(\\d*)".toRegex(),
        urlPathFormat = "merge_requests/%d",
        urlPathForDiff = "/diffs",
        commitPathFormat = "%s/commit/%s",
        pullRequestName = "Merge Request",
        icon = FindPullRequestIcons.gitLabIcon
    ),
    Bitbucket(
        defaultMergeCommitMessage = "\\(pull request #(\\d*)\\)".toRegex(),
        squashCommitMessage = "\\(pull request #(\\d*)\\)".toRegex(),
        urlPathFormat = "pull-requests/%d",
        urlPathForDiff = "/diff",
        commitPathFormat = "%s/commits/%s",
        pullRequestName = "Pull Request",
        icon = FindPullRequestIcons.bitbucketIcon
    ),

    ;

    companion object {
        @JvmStatic
        fun findBy(name: String): FindPullRequestHostingServices {
            return entries.firstOrNull { it.name == name } ?: GitHub
        }
    }

    fun createFileAnchorValue(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.root?.canonicalPath?.plus("/") ?: return null
        val filePath = annotate.file?.canonicalPath?.subtract(projectDir) ?: return null
        return when (this) {
            GitHub -> "#diff-${filePath.toSHA256()}"
            GitLab -> "#${filePath.toSHA1()}"
            Bitbucket -> "#chg-$filePath"
        }
    }
}