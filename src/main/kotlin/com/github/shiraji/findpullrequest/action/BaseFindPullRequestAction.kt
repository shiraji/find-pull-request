package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.model.*
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

abstract class BaseFindPullRequestAction : AnAction() {

    abstract fun actionPerform(e: AnActionEvent, url: String)

    abstract fun actionPerformForNoPullRequestFount(e: AnActionEvent, ex: NoPullRequestFoundException, url: String)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = getGitRepository(project, virtualFile) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return
        val gitRepositoryService = GitRepositoryService()
        val gitUrlService = GitRepositoryUrlService()

        val model = FindPullRequestModel(project, editor, virtualFile, gitRepositoryService, gitUrlService)
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

        val hostingServices = FindPullRequestHostingServices.findBy(config.getHosting())
        try {
            val path = "$webRepoUrl/${model.createPullRequestPath(repository, revisionHash)}"
            val url = createUrl(config, hostingServices, path, repository, model)
            actionPerform(e, url)
        } catch (ex: VcsException) {
            val name = FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.toLowerCase()
            showErrorNotification("Could not find the $name for $revisionHash : ${ex.message}")
        } catch (ex: NoPullRequestFoundException) {
            val path = hostingServices.commitPathFormat.format(webRepoUrl, revisionHash)
            val url = createUrl(config, hostingServices, path, repository, model)
            actionPerformForNoPullRequestFount(e, ex, url = url)
        }
    }

    private fun createUrl(
            config: PropertiesComponent,
            hostingServices: FindPullRequestHostingServices,
            path: String,
            repository: GitRepository,
            model: FindPullRequestModel
    ): String {
        return if (config.isJumpToFile()) {
            val fileAnnotation = model.getFileAnnotation(repository) ?: return path
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
        val gitRepositoryService = GitRepositoryService()
        val gitUrlService = GitRepositoryUrlService()

        e.presentation.isEnabledAndVisible = FindPullRequestModel(project, editor, virtualFile, gitRepositoryService, gitUrlService).isEnable(repository)
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