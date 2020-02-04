package com.github.shiraji.findpullrequest.service

import com.github.shiraji.findpullrequest.config.getProtocol
import com.github.shiraji.findpullrequest.config.isJumpToFile
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.domain.PathInfo
import com.github.shiraji.findpullrequest.helper.root
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CreateUrlService(
    private val project: Project,
    private val virtualFile: VirtualFile,
    private val config: PropertiesComponent,
    private val gitUrlService: GitUrlService
) {

    private fun formatUrl(webRepoUrl: String, path: String, fileAnchor: String): String {
        return "$webRepoUrl/$path$fileAnchor"
    }

    fun createUrl(pathInfo: PathInfo): String {
        val webRepoUrl = createWebRepoUrl()
        val path = pathInfo.createPath()
        val fileAnchor = createFileAnchor(pathInfo.hostingService)
        return formatUrl(webRepoUrl = webRepoUrl, path = path, fileAnchor = fileAnchor)
    }

    private fun createFileAnchor(hostingService: HostingService): String {
        return if (config.isJumpToFile()) {
            hostingService.createFileAnchorValue(project.root, virtualFile) ?: ""
        } else {
            ""
        }
    }

    private fun createWebRepoUrl(): String {
        val remoteUrl: String =
            gitUrlService.findUpstreamUrl() ?: gitUrlService.findOriginUrl() ?: throw IllegalStateException("No upstream/origin URL found")
        val path = gitUrlService.toURLPath(remoteUrl)
        return "${config.getProtocol()}$path"
    }
}