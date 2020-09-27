package com.github.shiraji.findpullrequest.presentation.action

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.config.isPopupAfterCopy
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.TextTransferable
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

    private fun copy(text: String) {
        CopyPasteManager.getInstance().setContents(TextTransferable(text as CharSequence))
    }

    override fun actionPerform(config: PropertiesComponent, url: String) {
        copy(url)
        if (config.isPopupAfterCopy()) showInfoNotification("Copied!")
    }

    override fun menuText(project: Project, config: PropertiesComponent): String {
        return "Copy ${HostingService.findBy(config.getHosting()).pullRequestName} URL"
    }

    override fun menuIcon(project: Project, config: PropertiesComponent): Icon? = null

    override fun handleNoPullRequestFound(
        config: PropertiesComponent,
        url: String
    ) {
        val message =
            StringBuilder("Could not find the ${HostingService.findBy(config.getHosting()).pullRequestName.toLowerCase()}. <a href=\"$url\">Copy the commit URL</a> ")

        Notifications.Bus.notify(
            Notification(
                "FindPullRequest.Info.Copy",
                "Find Pull Request",
                message.toString(),
                NotificationType.INFORMATION,
                CopyNotificationAdapter()
            )
        )
    }
}
