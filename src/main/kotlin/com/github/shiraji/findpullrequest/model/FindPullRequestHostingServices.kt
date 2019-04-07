package com.github.shiraji.findpullrequest.model

import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.github.shiraji.toSHA1
import com.intellij.openapi.vcs.annotate.FileAnnotation
import git4idea.repo.GitRepository

enum class FindPullRequestHostingServices(val defaultMergeCommitMessage: Regex, val squashCommitMessage: Regex, val urlPathFormat: String, val commitPathFormat: String, val pullRequestName: String) {
    GitHub("Merge pull request #(\\d*)".toRegex(), ".*\\(#(\\d*)\\)".toRegex(), "pull/%d/files", "%s/commit/%s", "Pull Request"),
    GitLab("See merge request .*!(\\d*)".toRegex(), "See merge request .*!(\\d*)".toRegex(), "merge_requests/%d/diffs", "%s/commit/%s", "Merge Request"),
    Bitbucket("\\(pull request #(\\d*)\\)".toRegex(), "\\(pull request #(\\d*)\\)".toRegex(), "pull-requests/%d/diff", "%s/commits/%s", "Pull Request"),

    ;

    companion object {
        @JvmStatic
        fun findBy(name: String): FindPullRequestHostingServices {
            return values().firstOrNull { it.name == name } ?: GitHub
        }
    }

    fun createFileAnchorValue(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.baseDir.canonicalPath?.plus("/") ?: return null
        val filePath = annotate.file?.canonicalPath?.subtract(projectDir) ?: return null
        return when (this) {
            GitHub -> "#diff-${filePath.toMd5()}"
            GitLab -> "#${filePath.toSHA1()}"
            Bitbucket -> "#chg-$filePath"
        }
    }
}