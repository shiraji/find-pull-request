package com.github.shiraji.findpullrequest.presentation.action

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class FindPullRequestAction : BaseFindPullRequestAction() {
    override fun actionPerform(config: PropertiesComponent, url: String) {
        BrowserUtil.open(url)
    }

    override fun menuText(project: Project, config: PropertiesComponent): String {
        return "Find ${HostingService.findBy(config.getHosting()).pullRequestName}"
    }

    override fun menuIcon(project: Project, config: PropertiesComponent): Icon? {
        return HostingService.findBy(config.getHosting()).icon
    }

    override fun handleNoPullRequestFound(
        config: PropertiesComponent,
        url: String
    ) {
        val message =
            StringBuilder("Could not find the ${HostingService.findBy(config.getHosting()).pullRequestName.toLowerCase()}. <a href=\"$url\">Open the commit page</a> ")
        showInfoNotification(message.toString())
    }
}
