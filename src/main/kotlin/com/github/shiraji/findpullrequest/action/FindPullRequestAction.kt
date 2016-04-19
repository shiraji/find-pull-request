package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.helper.FindPullRequestHelper
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.github.shiraji.getPullRequestNumber
import com.github.shiraji.getPullRequestNumberFromSquashCommit
import com.github.shiraji.isSquashPullRequestCommit
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsException

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val model = FindPullRequestModel(e)
        if (!model.isEnable()) return

        val repository = model.getRepository()
        if (repository == null) {
            FindPullRequestHelper.showErrorNotification("Could not find git repository.")
            return
        }

        val annotate = model.getFileAnnotation(repository)
        if (annotate == null ) {
            FindPullRequestHelper.showErrorNotification("Could not load file annotations.")
            return
        }

        val revisionHash = model.createRevisionHash(annotate)
        if (revisionHash == null) {
            FindPullRequestHelper.showErrorNotification("Could not find revision hash")
            return
        }

        val githubRepoUrl = model.createGithubRepoUrl(repository)
        if (githubRepoUrl == null) {
            FindPullRequestHelper.showErrorNotification("Could not find GitHub repository url")
            return
        }

        var path: String?
        try {
            val pullRequestCommit = model.findPullRequestCommit(repository, revisionHash)
            val mergedCommits = if (pullRequestCommit == null)
                                    null
                                else
                                    model.findMergedCommitdFromPullRequestCommit(repository, pullRequestCommit)
            path = if (mergedCommits == null || mergedCommits.filter { it.id.asString() == revisionHash.asString() }.size == 0) {
                val commit = model.findCommitLog(repository, revisionHash)
                if (commit.isSquashPullRequestCommit()) {
                    "pull/${commit.getPullRequestNumberFromSquashCommit()}/files"
                } else {
                    // show opening commit pages info
                    FindPullRequestHelper.showInfoNotification("Could not find the pull request. Open the commit which the line is added")
                    "commit/$revisionHash"
                }
            } else {
                "pull/${pullRequestCommit!!.getPullRequestNumber()}/files"
            }
        } catch (e: VcsException) {
            FindPullRequestHelper.showErrorNotification("Could not find the pull request for $revisionHash")
            return
        }

        val url = "$githubRepoUrl/$path#diff-${model.createMd5Hash(repository, annotate, revisionHash)}"
        BrowserUtil.open(url)
    }

    override fun update(e: AnActionEvent?) {
        e ?: return
        super.update(e)
        e.presentation.isEnabledAndVisible = FindPullRequestModel(e).isEnable()
    }
}
