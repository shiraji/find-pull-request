package com.github.shiraji.findpullrequest.model

import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.intellij.openapi.vcs.annotate.FileAnnotation
import git4idea.repo.GitRepository

enum class FindPullRequestHostingServices(val defaultMergeCommitMessage: Regex, val urlPathFormat: String) {
    GitHub("Merge pull request #(\\d*)".toRegex(), "pull/%d/files"),
    GitLab("See merge request !(\\d*)".toRegex(), "merge_requests/%d/diffs"),
    BitBucket("\\(pull request #(\\d*)\\)".toRegex(), "pull-requests/%d/diff"),

    ;

    fun createFileAnchorValue(repository: GitRepository, annotate: FileAnnotation): String? {
        val projectDir = repository.project.baseDir.canonicalPath?.plus("/") ?: return null
        val filePath = annotate.file?.canonicalPath?.subtract(projectDir) ?: return null
        return when(this) {
            GitHub ->  "#diff-${filePath.toMd5()}"
            GitLab -> "#${filePath.toMd5()}"
            BitBucket -> "#chg-$filePath"
        }
    }
}