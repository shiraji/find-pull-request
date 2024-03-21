package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.getHosting
import com.github.shiraji.findpullrequest.model.isDebugMode
import com.github.shiraji.findpullrequest.model.isPopupAfterCopy
import com.github.shiraji.getLine
import com.github.shiraji.subtract
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.TextTransferable
import java.net.URLEncoder
import java.util.Locale
import javax.swing.event.HyperlinkEvent

class FindPullRequestCopyAction @JvmOverloads constructor(currentLine: Int? = null) : BaseFindPullRequestAction(currentLine) {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

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
        val message = StringBuilder("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.lowercase(
            Locale.getDefault()
        )}. <a href=\"$url\">Copy the commit URL</a> ")
        if (config.isDebugMode()) {
            val title = URLEncoder.encode("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.lowercase(
                Locale.getDefault()
            )}", "UTF-8")
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
        if (config.isPopupAfterCopy()) {
            Notifications.Bus.notify(Notification("FindPullRequest.Info", "Find Pull Request", "Copied!", NotificationType.INFORMATION))
        }
    }

    private fun copy(text: String) {
        CopyPasteManager.getInstance().setContents(TextTransferable(text as CharSequence))
    }

    override fun menuText(project: Project, useShortName: Boolean, prNumber: Int?): String? {
        val config = PropertiesComponent.getInstance(project) ?: return null
        return if (useShortName) {
            "Copy Link to ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName}"
        } else {
            if (prNumber != null) {
                "Copy Link to ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName}(#$prNumber) URL"
            } else {
                "Copy Link to ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName} URL"
            }
        }
    }

    override fun description(project: Project, editor: Editor, virtualFile: VirtualFile): String? {
        val config = PropertiesComponent.getInstance(project) ?: return null
        val path = virtualFile.canonicalPath?.subtract(project.root?.parent?.path ?: "") ?: virtualFile.name
        return "Copy ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName} link for the selected line of $path at line ${editor.getLine(editor.selectionModel.selectionStart)}"
    }
}
