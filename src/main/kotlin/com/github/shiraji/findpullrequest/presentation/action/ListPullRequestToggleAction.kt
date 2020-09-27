package com.github.shiraji.findpullrequest.presentation.action

import com.github.shiraji.findpullrequest.domain.PathInfo
import com.github.shiraji.findpullrequest.ext.getGitRepository
import com.github.shiraji.findpullrequest.infra.git.GitHistory
import com.github.shiraji.findpullrequest.scenario.ListPullRequestScenario
import com.github.shiraji.findpullrequest.service.CreateUrlService
import com.github.shiraji.findpullrequest.service.FindPrInfoService
import com.github.shiraji.findpullrequest.service.GitHistoryService
import com.github.shiraji.findpullrequest.service.GitRepositoryService
import com.github.shiraji.findpullrequest.service.GitUrlService
import com.github.shiraji.findpullrequest.service.UpToDateLineNumberProviderService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl
import git4idea.GitUtil
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

class ListPullRequestToggleAction : ToggleAction() {

    private fun isEnabled(e: AnActionEvent): Boolean {
        return useActionModule(e) {
            ListPullRequestScenario().isEnabledListPullRequestAction()
        } ?: false
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = isEnabled(e)
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return useActionModule(e) {
            ListPullRequestScenario().isSelectedListPullRequestAction()
        } ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
            listPRAnnotation(e)
        } else {
            closePRAnnotation(e)
        }
    }

    private fun actionModule(
        e: AnActionEvent,
        project: Project
    ): Module? {
        return module {
            single { project }
            single { PropertiesComponent.getInstance(project) }
            single { e.getData(CommonDataKeys.EDITOR) }
            single { e.getData(CommonDataKeys.VIRTUAL_FILE) }
            single { e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) }
            single { GitUtil.getRepositoryManager(get()) }
            single { getGitRepository(get(), get(), get()) }
            single { GitHistory(get(), get()) }
            single { GitHistoryService(get()) }
            single { FindPrInfoService(get(), get()) }
            single { UpToDateLineNumberProviderImpl(get<Editor>().document, get()) }
            single { UpToDateLineNumberProviderService(get()) }
            single { GitUrlService(get()) }
            single { CreateUrlService(get(), get(), get(), get()) }
            single { ProjectLevelVcsManager.getInstance(get()).annotationLocalChangesListener }
            single { GitRepositoryService(get()) }
            single { hashMapOf<String, PathInfo>() }
        }
    }

    private fun listPRAnnotation(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val actionModule = actionModule(e, project) ?: return
        object : Task.Backgroundable(project, "Listing Pull Request...") {
            override fun onFinished() {
                super.onFinished()
                stopKoin()
            }

            override fun run(indicator: ProgressIndicator) {
                startKoin {
                    modules(actionModule)
                }

                val scenario = ListPullRequestScenario()
                scenario.annotate()
            }
        }.queue()
    }

    private fun closePRAnnotation(e: AnActionEvent) {
        useActionModule(e) {
            ListPullRequestScenario().closeListPullRequestTextAnnotationGutterProvider()
        }
    }

    private fun <T> useActionModule(e: AnActionEvent, body: () -> T): T? {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return null
        val actionModule = actionModule(e, project) ?: return null
        startKoin {
            modules(actionModule)
        }
        try {
            return body()
        } finally {
            stopKoin()
        }
    }
}