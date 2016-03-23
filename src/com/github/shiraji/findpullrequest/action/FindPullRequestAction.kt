package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val model = FindPullRequestModel(e)
        if (!model.isEnable()) return

        val url = model.findPullRequestUrlOrCommitUrl() ?: return

        if (!model.isPullRequestUrl(url)) {
            Notifications.Bus.notify(Notification("FindPullRequest",
                    "Find Pull Request",
                    "Could not find the pull request. Open the commit which the line is added",
                    NotificationType.INFORMATION))
        }

        BrowserUtil.open(url)
    }

    override fun update(e: AnActionEvent?) {
        e ?: return
        super.update(e)

        if(!FindPullRequestModel(e).isEnable()) {
            e.presentation.isEnabled = false
            e.presentation.isVisible = false
        }
    }
}
