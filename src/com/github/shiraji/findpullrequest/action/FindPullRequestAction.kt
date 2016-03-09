package com.github.shiraji.findpullrequest.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepository
import org.jetbrains.plugins.github.util.GithubUtil

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val eventData = calcData(e)

        val foo = eventData?.repository?.remotes?.joinToString {
            it.pushUrls.toString() + "\n"
        }

        Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
                "Plugin Importer+Exporter",
                "EventData: " + foo,
                NotificationType.INFORMATION))
    }

    private fun calcData(e : AnActionEvent): EventData? {
        val project = e.getData(CommonDataKeys.PROJECT)
        project ?: return null

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        virtualFile ?: return null

        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: return null

        val repository = GithubUtil.getGitRepository(project, virtualFile)
        repository ?: return null

        return EventData(project, repository)
    }

    private data class EventData(val project: Project, val repository: GitRepository) {
    }
}
