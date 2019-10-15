package com.github.shiraji.findpullrequest.model

import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.github.shiraji.toSHA1
import com.intellij.openapi.vcs.annotate.FileAnnotation
import git4idea.repo.GitRepository
import icons.FindPullRequestIcons
import icons.GithubIcons
import javax.swing.Icon

enum class FindPullRequestHostingServices(val defaultMergeCommitMessage: Regex, val squashCommitMessage: Regex, val urlPathFormat: String, val commitPathFormat: String, val pullRequestName: String, val icon: Icon) {
    GitHub("Merge pull request #(\\d*)".toRegex(), ".*\\(#(\\d*)\\)".toRegex(), "pull/%d/files", "%s/commit/%s", "Pull Request", GithubIcons.Github_icon),
    GitLab("See merge request .*!(\\d*)".toRegex(), "See merge request .*!(\\d*)".toRegex(), "merge_requests/%d/diffs", "%s/commit/%s", "Merge Request", FindPullRequestIcons.gitLabIcon),
    Bitbucket("\\(pull request #(\\d*)\\)".toRegex(), "\\(pull request #(\\d*)\\)".toRegex(), "pull-requests/%d/diff", "%s/commits/%s", "Pull Request", FindPullRequestIcons.bitbucketIcon),

    ;

    companion object {
        @JvmStatic
        fun findBy(name: String): FindPullRequestHostingServices {
            return values().firstOrNull { it.name == name } ?: GitHub
        }
    }

    fun createFileAnchorValue(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.root?.canonicalPath?.plus("/") ?: return null
        val filePath = annotate.file?.canonicalPath?.subtract(projectDir) ?: return null
        return when (this) {
            GitHub -> "#diff-${filePath.toMd5()}"
            GitLab -> "#${filePath.toSHA1()}"
            Bitbucket -> "#chg-$filePath"
        }
    }
}