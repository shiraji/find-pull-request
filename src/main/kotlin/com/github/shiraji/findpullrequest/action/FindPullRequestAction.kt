package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
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
            showErrorNotification("Could not find git repository.")
            return
        }

        val annotate = model.getFileAnnotation(repository)
        if (annotate == null) {
            showErrorNotification("Could not load file annotations.")
            return
        }

        val revisionHash = model.createRevisionHash(annotate)
        if (revisionHash == null) {
            showErrorNotification("Could not find revision hash")
            return
        }

        val githubRepoUrl = model.createGithubRepoUrl(repository)
        if (githubRepoUrl == null) {
            showErrorNotification("Could not find GitHub repository url")
            return
        }

        val path: String?
        try {
            path = model.createPullRequestPathFromCommit(model.findPullRequestCommit(repository, revisionHash), repository, revisionHash)
        } catch (e: VcsException) {
            showErrorNotification("Could not find the pull request for $revisionHash")
            return
        }

        val fileMD5 = model.createFileMd5Hash(repository, annotate)
        if (path == null) {
            // No pull request found. Show commit page
            showInfoNotification("""Could not find the pull request. <a href="$githubRepoUrl/commit/$revisionHash${createDiffPathFrom(fileMD5)}">Open the commit page</a>""")
        } else {
            val url = "$githubRepoUrl/$path${createDiffPathFrom(fileMD5)}"
            BrowserUtil.open(url)
        }
    }

    private fun createDiffPathFrom(fileMD5: String?) = if(fileMD5 == null) "" else "#diff-$fileMD5"

    override fun update(e: AnActionEvent?) {
        e ?: return
        super.update(e)
        e.presentation.isEnabledAndVisible = FindPullRequestModel(e).isEnable()
    }
}
