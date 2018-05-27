package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.github.shiraji.findpullrequest.model.isDebugMode
import com.github.shiraji.findpullrequest.model.isJumpToFile
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.github.util.GithubUtil
import java.net.URLEncoder

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = GithubUtil.getGitRepository(project, virtualFile) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return

        val model = FindPullRequestModel(project, editor, virtualFile)
        if (!model.isEnable(repository)) return

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

        val fileMD5 = model.createFileMd5Hash(repository, annotate)
        try {
            val path = "$githubRepoUrl/${model.createPullRequestPath(repository, revisionHash)}"
            val url = createUrl(config, path, fileMD5)
            BrowserUtil.open(url)
        } catch (e: VcsException) {
            showErrorNotification("Could not find the pull request for $revisionHash : ${e.message}")
        } catch (e: NoPullRequestFoundException) {
            val path = "$githubRepoUrl/commit/$revisionHash"
            val url = createUrl(config, path, fileMD5)
            val message = StringBuilder("Could not find the pull request. <a href=\"$url\">Open the commit page</a> ")
            if (config.isDebugMode()) {
                val title = URLEncoder.encode("Could not find the pull request", "UTF-8")
                val encodedMessage = URLEncoder.encode(e.detailMessage, "UTF-8")
                message.append("or <a href=\"https://github.com/shiraji/find-pull-request/issues/new?title=$title&body=$encodedMessage\">Submit Issue</a>")
            }
            showInfoNotification(message.toString())
        }
    }

    private fun createUrl(config: PropertiesComponent, path: String, fileMD5: String?) = if (config.isJumpToFile()) path + createDiffPathFrom(fileMD5) else path

    private fun createDiffPathFrom(fileMD5: String?) = if(fileMD5 == null) "" else "#diff-$fileMD5"

    override fun update(e: AnActionEvent?) {
        e ?: return
        super.update(e)
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = GithubUtil.getGitRepository(project, virtualFile) ?: return

        e.presentation.isEnabledAndVisible = FindPullRequestModel(project, editor, virtualFile).isEnable(repository)
    }
}
