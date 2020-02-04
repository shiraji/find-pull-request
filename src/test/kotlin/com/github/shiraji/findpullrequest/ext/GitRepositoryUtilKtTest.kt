package com.github.shiraji.findpullrequest.ext

import com.github.shiraji.findpullrequest.helper.root
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepositoryManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitRepositoryUtilKtTest {
    @Nested
    inner class getGitRepository {
        private val project: Project = mockk()
        private val file: VirtualFile = mockk()
        private val manager: GitRepositoryManager = mockk()

        @Test
        fun `Should use file if file is not null`() {
            val fileSlot = slot<VirtualFile>()
            every { manager.getRepositoryForFileQuick(capture(fileSlot)) } returns mockk()

            val got = getGitRepository(project, file, manager)

            assertNotNull(got)
            assertEquals(fileSlot.captured, file)
        }

        @Test
        fun `Should use root if file is null`() {
            val fileSlot = slot<VirtualFile>()
            every { manager.getRepositoryForFileQuick(capture(fileSlot)) } returns mockk()

            val root = mockk<VirtualFile>()
            every { project.basePath } returns ""
            mockkStatic(LocalFileSystem::class)
            every { LocalFileSystem.getInstance().findFileByPath(any()) } returns root

            val got = getGitRepository(project, null, manager)

            assertNotNull(got)
            assertEquals(fileSlot.captured, root)
        }

        @Test
        fun `Should return null if file and root is null`() {
            val root = mockk<VirtualFile>()
            every { project.basePath } returns ""
            mockkStatic(LocalFileSystem::class)
            every { LocalFileSystem.getInstance().findFileByPath(any()) } returns null

            val got = getGitRepository(project, null, manager)

            assertNull(got)
        }
    }
}