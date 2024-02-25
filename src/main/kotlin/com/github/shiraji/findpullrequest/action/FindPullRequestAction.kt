package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.getHosting
import com.github.shiraji.findpullrequest.model.isDebugMode
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.net.URLEncoder
import java.util.Locale
import javax.swing.Icon

class FindPullRequestAction : BaseFindPullRequestAction() {
    override fun actionPerformForNoPullRequestFount(e: AnActionEvent, ex: NoPullRequestFoundException, url: String) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return
        val message = StringBuilder("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.lowercase(
            Locale.getDefault()
        )}. <a href=\"$url\">Open the commit page</a> ")
        if (config.isDebugMode()) {
            val title = URLEncoder.encode("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.lowercase(
                Locale.getDefault()
            )}", "UTF-8")
            val encodedMessage = URLEncoder.encode(ex.detailMessage, "UTF-8")
            message.append("or <a href=\"https://github.com/shiraji/find-pull-request/issues/new?title=$title&body=$encodedMessage\">Submit Issue</a>")
        }
        // Use deprecated NotificationListener.URL_OPENING_LISTENER
        // see https://intellij-support.jetbrains.com/hc/en-us/community/posts/5350163597074-Notification-setListener-deprecated
        // TODO: check NotificationAction
        Notifications.Bus.notify(Notification("FindPullRequest.Info", "Find Pull Request", message.toString(), NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER))
    }

    override fun actionPerform(e: AnActionEvent, url: String) {
        BrowserUtil.open(url)
    }

    override fun menuText(project: Project): String? {
        val config = PropertiesComponent.getInstance(project) ?: return null
        return FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName
    }
}
