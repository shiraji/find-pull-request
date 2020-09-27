package com.github.shiraji.findpullrequest.domain

import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.github.shiraji.toSHA1
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

enum class HostingService(
    val defaultMergeCommitMessage: Regex,
    val squashCommitMessage: Regex,
    val urlPathFormat: String,
    val commitPathFormat: String,
    val pullRequestName: String,
    val icon: Icon
) {
    GitHub(
        "Merge pull request #(\\d*)".toRegex(),
        ".*\\(#(\\d*)\\)".toRegex(),
        "pull/%d/files",
        "commit/%s",
        "Pull Request",
        AllIcons.Vcs.Vendors.Github
    ),
    GitLab(
        "See merge request .*!(\\d*)".toRegex(),
        "See merge request .*!(\\d*)".toRegex(),
        "merge_requests/%d/diffs",
        "commit/%s",
        "Merge Request",
        IconLoader.getIcon("/icons/gitlab.svg")
    ),
    Bitbucket(
        "\\(pull request #(\\d*)\\)".toRegex(),
        "\\(pull request #(\\d*)\\)".toRegex(),
        "pull-requests/%d/diff",
        "commits/%s",
        "Pull Request",
        IconLoader.getIcon("/icons/bitbucket.svg")
    ),

    ;

    companion object {
        fun findBy(name: String): HostingService {
            return values().firstOrNull { it.name == name } ?: GitHub
        }

        fun findFromMergeCommitMessage(commitFullMessage: String): HostingService? {
            return values().firstOrNull {
                it.defaultMergeCommitMessage.containsMatchIn(commitFullMessage)
            }
        }

        fun findFromSquashCommitMessage(commitFullMessage: String): HostingService? {
            return values().firstOrNull {
                it.squashCommitMessage.containsMatchIn(commitFullMessage)
            }
        }
    }

    fun createFileAnchorValue(root: VirtualFile?, file: VirtualFile): String? {
        val projectDir = root?.canonicalPath?.plus("/") ?: return null
        val filePath = file.canonicalPath?.subtract(projectDir) ?: return null
        return when (this) {
            GitHub -> "#diff-${filePath.toMd5()}"
            GitLab -> "#${filePath.toSHA1()}"
            Bitbucket -> "#chg-$filePath"
        }
    }

    private fun getPrNumberFromRegex(regex: Regex, commitFullMessage: String): Int? {
        val result = regex.find(commitFullMessage) ?: return null
        val group = result.groups[1]
        checkNotNull(group)
        return group.value.toInt()
    }

    fun getPrNumberFromMergeCommit(commitFullMessage: String): Int? {
        return getPrNumberFromRegex(defaultMergeCommitMessage, commitFullMessage)
    }

    fun getPrNumberFromSquashCommit(commitFullMessage: String): Int? {
        return getPrNumberFromRegex(squashCommitMessage, commitFullMessage)
    }
}