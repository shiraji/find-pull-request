package com.github.shiraji.findpullrequest.presentation.action

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.exceptions.NoRevisionFoundException
import com.github.shiraji.findpullrequest.exceptions.UnsupportedHostingServiceException
import com.github.shiraji.findpullrequest.ext.getGitRepository
import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.infra.git.GitHistory
import com.github.shiraji.findpullrequest.scenario.FindPullRequestScenario
import com.github.shiraji.findpullrequest.service.CreateUrlService
import com.github.shiraji.findpullrequest.service.FindPrInfoService
import com.github.shiraji.findpullrequest.service.GitHistoryService
import com.github.shiraji.findpullrequest.service.GitRepositoryService
import com.github.shiraji.findpullrequest.service.GitUrlService
import com.github.shiraji.findpullrequest.service.UpToDateLineNumberProviderService
import com.github.shiraji.getLine
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl
import git4idea.GitUtil
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import javax.swing.Icon

abstract class BaseFindPullRequestAction : AnAction() {

    abstract fun actionPerform(config: PropertiesComponent, url: String)
    abstract fun menuText(project: Project, config: PropertiesComponent): String
    abstract fun menuIcon(project: Project, config: PropertiesComponent): Icon?
    abstract fun handleNoPullRequestFound(
        config: PropertiesComponent,
        url: String
    )

    private fun actionModule(
        e: AnActionEvent,
        project: Project,
        config: PropertiesComponent
    ): Module? {
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        // If we get line number inside Task.Backgroundable, it will require the event dispatch thread
        // To avoid threading, we get line number at this point.
        // Note: createdAtStart = true not work because it's already in Task.Backgroundable
        val lineNumber = editor.getLine(editor.selectionModel.selectionStart)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        val gitRepository = getGitRepository(project, virtualFile, GitUtil.getRepositoryManager(project)) ?: return null
        return module {
            single { project }
            single { config }
            single { editor }
            single { virtualFile }
            single { lineNumber }

            single { UpToDateLineNumberProviderImpl(get<Editor>().document, get<Project>()) }
            single { ChangeListManager.getInstance(get()) }
            single { GitUtil.getRepositoryManager(get()) }

            single { gitRepository }

            single { GitHistory(get(), get()) }

            single { FindPrInfoService(get(), get()) }
            single { GitHistoryService(get()) }
            single { GitRepositoryService(get()) }
            single { GitUrlService(get()) }
            single { CreateUrlService(get(), get(), get(), get()) }
            single { UpToDateLineNumberProviderService(get()) }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val config: PropertiesComponent = PropertiesComponent.getInstance(project) ?: return
        val actionModule = actionModule(e, project, config) ?: return

        object : Task.Backgroundable(project, "Finding Pull Request...") {
            override fun run(indicator: ProgressIndicator) {
                startKoin { modules(actionModule) }
                val scenario = FindPullRequestScenario()
                val url = scenario.findPrUrl()
                actionPerform(config, url)
            }

            override fun onFinished() {
                super.onFinished()
                stopKoin()
            }

            override fun onThrowable(error: Throwable) {
                when (error) {
                    is NoPullRequestFoundException, is UnsupportedHostingServiceException -> handleNoPullRequestFoundException()
                    is NoRevisionFoundException -> handleNoRevisionFoundException(error)
                    is VcsException -> handleVcsException(error)
                    is IllegalStateException -> showErrorNotification(error.message ?: "")
                    else -> super.onThrowable(error)
                }
            }

            private fun handleNoRevisionFoundException(error: NoRevisionFoundException) {
                showErrorNotification(error.detailMessage)
            }

            private fun handleVcsException(error: VcsException) {
                val name = HostingService.findBy(config.getHosting()).pullRequestName.toLowerCase()
                val revision = FindPullRequestScenario().getLineRevision()
                showErrorNotification("Could not find the $name for $revision : ${error.message}")
            }

            private fun handleNoPullRequestFoundException() {
                object : Task.Backgroundable(project, "Finding commit page...") {
                    override fun run(indicator: ProgressIndicator) {
                        startKoin { modules(actionModule) }
                        val scenario = FindPullRequestScenario()
                        val url = scenario.findCommitUrl()
                        handleNoPullRequestFound(config, url)
                    }

                    override fun onFinished() {
                        super.onFinished()
                        stopKoin()
                    }
                }.queue()
            }
        }.queue()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = false

        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return
        val actionModule = actionModule(e, project, config) ?: return

        startKoin {
            modules(actionModule)
        }

        try {
            e.presentation.isEnabledAndVisible =
                ApplicationManager.getApplication().isReadAccessAllowed && FindPullRequestScenario().isEnabled()
        } finally {
            stopKoin()
        }

        e.presentation.text = menuText(project, config)
        menuIcon(project, config)?.let { e.presentation.icon = it }
    }
}