package com.github.shiraji.findpullrequest

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcs.log.VcsLogDataKeys

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val commit = ContainerUtil.getFirstItem(e.getRequiredData(VcsLogDataKeys.VCS_LOG).selectedCommits)

        Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
                "Plugin Importer+Exporter",
                "Commit: " + commit, NotificationType.INFORMATION));
    }
}
