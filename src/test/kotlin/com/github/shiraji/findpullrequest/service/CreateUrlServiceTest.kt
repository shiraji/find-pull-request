package com.github.shiraji.findpullrequest.service

import com.github.shiraji.findpullrequest.config.getProtocol
import com.github.shiraji.findpullrequest.config.isJumpToFile
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.domain.PathInfo
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CreateUrlServiceTest {
    private val project: Project = mockk()
    private val virtualFile: VirtualFile = mockk()
    private val root: VirtualFile = mockk()
    private val config: PropertiesComponent = mockk()
    private val gitUrlService: GitUrlService = mockk()
    private val pr: PathInfo.Pr = mockk()
    private val hostingService: HostingService = mockk()
    private val service = CreateUrlService(project, virtualFile, config, gitUrlService)

    @Nested
    inner class createUrl {
        @Test
        fun `Should throw Exception if no remote url found`() {
            every { gitUrlService.findUpstreamUrl() } returns null
            every { gitUrlService.findOriginUrl() } returns null
            assertThrows<IllegalStateException> {
                service.createUrl(pr)
            }
        }

        @Test
        fun `Should create PR URL`() {
            every { gitUrlService.findUpstreamUrl() } returns "git@github.com:shiraji/find-pull-request.git"
            every { gitUrlService.toURLPath(any()) } returns "example.com"
            every { config.getProtocol() } returns "https://"
            every { pr.createPath() } returns "path"
            every { pr.hostingService } returns HostingService.GitHub
            every { config.isJumpToFile() } returns false

            val result = service.createUrl(pr)
            assertEquals("https://example.com/path", result)
        }

        @Test
        fun `Should create PR URL with file anchor if isJumpToFile is true`() {
            every { gitUrlService.findUpstreamUrl() } returns "git@github.com:shiraji/find-pull-request.git"
            every { gitUrlService.toURLPath(any()) } returns "example.com"
            every { config.getProtocol() } returns "https://"
            every { pr.createPath() } returns "path"
            every { pr.hostingService } returns hostingService
            every { config.isJumpToFile() } returns true
            mockkStatic(LocalFileSystem::class)
            every { LocalFileSystem.getInstance().findFileByPath(any()) } returns null
            every { project.basePath } returns ""
            every { hostingService.createFileAnchorValue(any(), any()) } returns "#file-anchor"

            val result = service.createUrl(pr)
            assertEquals("https://example.com/path#file-anchor", result)
        }

        @Test
        fun `Should create PR URL without file anchor if createFileAnchorValue returns null`() {
            every { gitUrlService.findUpstreamUrl() } returns null
            every { gitUrlService.findOriginUrl() } returns "git@github.com:shiraji/find-pull-request.git"
            every { gitUrlService.toURLPath(any()) } returns "example.com"
            every { config.getProtocol() } returns "https://"
            every { pr.createPath() } returns "path"
            every { pr.hostingService } returns hostingService
            every { config.isJumpToFile() } returns true
            mockkStatic(LocalFileSystem::class)
            every { LocalFileSystem.getInstance().findFileByPath(any()) } returns null
            every { project.basePath } returns ""
            every { hostingService.createFileAnchorValue(any(), any()) } returns null

            val result = service.createUrl(pr)
            assertEquals("https://example.com/path", result)
        }
    }
}