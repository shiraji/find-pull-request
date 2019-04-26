package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.getHosting
import com.github.shiraji.findpullrequest.model.isDebugMode
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.net.URLEncoder

class FindPullRequestAction : BaseFindPullRequestAction() {
    override fun actionPerformForNoPullRequestFount(e: AnActionEvent, ex: NoPullRequestFoundException, url: String) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val config = PropertiesComponent.getInstance(project) ?: return
        val message = StringBuilder("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.toLowerCase()}. <a href=\"$url\">Open the commit page</a> ")
        if (config.isDebugMode()) {
            val title = URLEncoder.encode("Could not find the ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName.toLowerCase()}", "UTF-8")
            val encodedMessage = URLEncoder.encode(ex.detailMessage, "UTF-8")
            message.append("or <a href=\"https://github.com/shiraji/find-pull-request/issues/new?title=$title&body=$encodedMessage\">Submit Issue</a>")
        }
        showInfoNotification(message.toString())
    }

    override fun actionPerform(e: AnActionEvent, url: String) {
        BrowserUtil.open(url)
    }

    override fun menuText(project: Project): String? {
        val config = PropertiesComponent.getInstance(project) ?: return null
        return "Find ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName}"
    }
}
