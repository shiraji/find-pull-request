package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.getHosting
import com.github.shiraji.findpullrequest.model.isDebugMode
import com.github.shiraji.findpullrequest.model.isPopupAfterCopy
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.TextTransferable
import java.net.URLEncoder
import javax.swing.Icon
import javax.swing.event.HyperlinkEvent

class FindPullRequestCopyAction : BaseFindPullRequestAction() {

    inner class CopyNotificationAdapter : NotificationListener.Adapter() {
        override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
            val url = e.url ?: return
            copy(url.toString())
            notification.expire()
        }
    }

    override fun actionPerformForNoPullRequestFount(e: AnActionEvent, ex: NoPullRequestFoundException, url: String) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return
        val message = StringBuilder("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.toLowerCase()}. <a href=\"$url\">Copy the commit URL</a> ")
        if (config.isDebugMode()) {
            val title = URLEncoder.encode("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.toLowerCase()}", "UTF-8")
            val encodedMessage = URLEncoder.encode(ex.detailMessage, "UTF-8")
            message.append("or <a href=\"https://github.com/shiraji/find-pull-request/issues/new?title=$title&body=$encodedMessage\">Submit Issue</a>")
        }

        Notifications.Bus.notify(Notification(
                "FindPullRequest.Info.Copy",
                "Find Pull Request",
                message.toString(),
                NotificationType.INFORMATION,
                CopyNotificationAdapter()
        ))
    }

    override fun actionPerform(e: AnActionEvent, url: String) {
        copy(url)

        val project = e.project ?: return
        val config = PropertiesComponent.getInstance(project)
        if (config.isPopupAfterCopy())
            showInfoNotification("Copied!")
    }

    private fun copy(text: String) {
        CopyPasteManager.getInstance().setContents(TextTransferable(text as CharSequence))
    }

    override fun menuText(project: Project): String? {
        val config = PropertiesComponent.getInstance(project) ?: return null
        return "Copy ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName} URL"
    }

    override fun menuIcon(project: Project): Icon? = null
}
