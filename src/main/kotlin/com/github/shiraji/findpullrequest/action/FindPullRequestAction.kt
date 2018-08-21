package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
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
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.net.URLEncoder

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = getGitRepository(project, virtualFile) ?: return
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

        val webRepoUrl = model.createWebRepoUrl(repository)
        if (webRepoUrl == null) {
            showErrorNotification("Could not find GitHub repository url")
            return
        }

        try {
            BrowserUtil.open("$webRepoUrl/${model.createPullRequestPath(repository, revisionHash)}")
        } catch (e: VcsException) {
            showErrorNotification("Could not find the pull request for $revisionHash : ${e.message}")
        } catch (e: NoPullRequestFoundException) {
            val path = "$webRepoUrl/commit/$revisionHash"
            val url = createUrl(config, path, repository, model)
            val message = StringBuilder("Could not find the pull request. <a href=\"$url\">Open the commit page</a> ")
            if (config.isDebugMode()) {
                val title = URLEncoder.encode("Could not find the pull request", "UTF-8")
                val encodedMessage = URLEncoder.encode(e.detailMessage, "UTF-8")
                message.append("or <a href=\"https://github.com/shiraji/find-pull-request/issues/new?title=$title&body=$encodedMessage\">Submit Issue</a>")
            }
            showInfoNotification(message.toString())
        }
    }

    private fun createUrl(config: PropertiesComponent, path: String, repository: GitRepository, model: FindPullRequestModel): String {
        return if (config.isJumpToFile()) {
            val fileAnnotation = model.getFileAnnotation(repository) ?: return path
            val hostingServices = FindPullRequestHostingServices.from(path)
            path + hostingServices.createFileAnchorValue(repository, fileAnnotation)
        } else {
            path
        }
    }

    override fun update(e: AnActionEvent?) {
        e ?: return
        super.update(e)
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = getGitRepository(project, virtualFile) ?: return

        e.presentation.isEnabledAndVisible = FindPullRequestModel(project, editor, virtualFile).isEnable(repository)
    }

    private fun getGitRepository(project: Project, file: VirtualFile?): GitRepository? {
        val manager = GitUtil.getRepositoryManager(project)
        val repositories = manager.repositories
        return when(repositories.size) {
            0 -> null
            1 -> repositories[0]
            else -> {
                if (file != null) {
                    val repository = manager.getRepositoryForFile(file)
                    if (repository != null) return repository
                }
                manager.getRepositoryForFile(project.baseDir)
            }
        }
    }
}
