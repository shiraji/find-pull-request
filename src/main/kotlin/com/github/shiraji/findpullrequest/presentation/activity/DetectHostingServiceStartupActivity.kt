package com.github.shiraji.findpullrequest.presentation.activity

import com.github.shiraji.findpullrequest.ext.getGitRepository
import com.github.shiraji.findpullrequest.infra.git.GitHistory
import com.github.shiraji.findpullrequest.scenario.DetectHostingServiceScenario
import com.github.shiraji.findpullrequest.service.GitHistoryService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import git4idea.GitUtil
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

class DetectHostingServiceStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val config = PropertiesComponent.getInstance(project)
        val startUpActivityModule = startUpActivityModule(project, config)

        object : Task.Backgroundable(project, "Detecting hosting service...") {
            override fun run(indicator: ProgressIndicator) {
                startKoin { modules(startUpActivityModule) }
                DetectHostingServiceScenario().saveHostingService()
            }

            override fun onFinished() {
                super.onFinished()
                stopKoin()
            }
        }.queue()
    }

    private fun startUpActivityModule(
        project: Project,
        config: PropertiesComponent?
    ): Module {
        return module {
            single { project }
            single { config }
            single { getGitRepository(get(), null, GitUtil.getRepositoryManager(get())) }
            single { GitHistory(get(), get()) }
            single { GitHistoryService(get()) }
            single { GitUtil.getRepositoryManager(get()) }
        }
    }
}