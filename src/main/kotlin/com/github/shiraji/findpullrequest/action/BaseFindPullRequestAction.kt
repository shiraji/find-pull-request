package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.github.shiraji.findpullrequest.model.GitConfService
import com.github.shiraji.findpullrequest.model.GitHistoryService
import com.github.shiraji.findpullrequest.model.GitRepositoryUrlService
import com.github.shiraji.findpullrequest.model.getHosting
import com.github.shiraji.getLine
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository
import javax.swing.Icon

abstract class BaseFindPullRequestAction : AnAction() {

    abstract fun actionPerform(e: AnActionEvent, url: String)

    abstract fun menuText(project: Project): String?
    abstract fun menuIcon(project: Project): Icon?

    abstract fun actionPerformForNoPullRequestFount(e: AnActionEvent, ex: NoPullRequestFoundException, url: String)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = getGitRepository(project, virtualFile) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return
        val gitRepositoryService = GitConfService()
        val gitUrlService = GitRepositoryUrlService()
        val gitHistoryService = GitHistoryService()

        val model = FindPullRequestModel(project, editor, virtualFile, gitRepositoryService, gitUrlService, gitHistoryService)
        if (!model.isEnable(repository)) return

        val lineNumber = editor.getLine(editor.selectionModel.selectionStart)
        val revisionHash = try {
            gitHistoryService.findRevisionHash(project, repository, virtualFile, lineNumber)
        } catch (e: VcsException) {
            showErrorNotification("Could not find revision hash")
            return
        }

        val webRepoUrl = model.createWebRepoUrl(repository)
        if (webRepoUrl == null) {
            showErrorNotification("Could not find GitHub repository url")
            return
        }

        val hostingServices = FindPullRequestHostingServices.findBy(config.getHosting())
        try {
            ApplicationManager.getApplication().executeOnPooledThread {
                val url = "$webRepoUrl/${model.createPullRequestPath(repository, revisionHash)}"
                actionPerform(e, url)
            }
        } catch (ex: VcsException) {
            val name = FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.toLowerCase()
            showErrorNotification("Could not find the $name for $revisionHash : ${ex.message}")
        } catch (ex: NoPullRequestFoundException) {
            val url = model.createCommitUrl(repository, hostingServices, webRepoUrl, revisionHash)
            actionPerformForNoPullRequestFount(e, ex, url = url)
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = false

        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = getGitRepository(project, virtualFile) ?: return
        val text = menuText(project) ?: return
        val icon = menuIcon(project)
        val gitRepositoryService = GitConfService()
        val gitUrlService = GitRepositoryUrlService()
        val gitHistoryService = GitHistoryService()

        e.presentation.isEnabledAndVisible = FindPullRequestModel(project, editor, virtualFile, gitRepositoryService, gitUrlService, gitHistoryService).isEnable(repository)
        e.presentation.text = text
        icon?.let { e.presentation.icon = it }
    }

    private fun getGitRepository(project: Project, file: VirtualFile?): GitRepository? {
        val manager = GitUtil.getRepositoryManager(project)
        val repositories = manager.repositories
        return when (repositories.size) {
            0 -> null
            1 -> repositories[0]
            else -> {
                if (file != null) {
                    val repository = manager.getRepositoryForFile(file)
                    if (repository != null) return repository
                }
                val root = project.root ?: return null
                manager.getRepositoryForFile(root)
            }
        }
    }
}
