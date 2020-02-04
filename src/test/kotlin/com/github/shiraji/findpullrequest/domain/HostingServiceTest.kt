package com.github.shiraji.findpullrequest.domain

import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.subtract
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class HostingServiceTest {

    @Nested
    inner class createFileAnchorValue {
        private val project: Project = mockk()

        @BeforeEach
        fun setUp() {
            mockkStatic(LocalFileSystem::class)
            every { LocalFileSystem.getInstance().findFileByPath(any()) } returns null
            every { project.basePath } returns ""
            every { project.root?.canonicalPath } returns ""
        }

        @Test
        fun `Should create GitHub style of file anchor`() {
            val virtualFile: VirtualFile = mockk()
            every { virtualFile.canonicalPath?.subtract("") } returns "aaa"

            val got = HostingService.GitHub.createFileAnchorValue(project.root, virtualFile)

            assertEquals("#diff-47bce5c74f589f4867dbd57e9ca9f808", got)
        }

        @Test
        fun `Should create GitLab style of file anchor`() {
            val virtualFile: VirtualFile = mockk()
            every { virtualFile.canonicalPath?.subtract("") } returns "aaa"

            val got = HostingService.GitLab.createFileAnchorValue(project.root, virtualFile)

            assertEquals("#7e240de74fb1ed08fa08d38063f6a6a91462a815", got)
        }

        @Test
        fun `Should create Bitbucket style of file anchor`() {
            val virtualFile: VirtualFile = mockk()
            every { virtualFile.canonicalPath?.subtract("") } returns "aaa"

            val got = HostingService.Bitbucket.createFileAnchorValue(project.root, virtualFile)

            assertEquals("#chg-aaa", got)
        }
    }

    @Nested
    inner class findBy {
        @Test
        fun `Should find GitHub if arg is GitHub`() {
            val text = "GitHub"
            assertEquals(HostingService.GitHub, HostingService.findBy(text))
        }

        @Test
        fun `Should find GitLab if arg is GitLab`() {
            val text = "GitLab"
            assertEquals(HostingService.GitLab, HostingService.findBy(text))
        }

        @Test
        fun `Should find Bitbucket if arg is Bitbucket`() {
            val text = "Bitbucket"
            assertEquals(HostingService.Bitbucket, HostingService.findBy(text))
        }

        @Test
        fun `Should find GitHub if arg is invalid`() {
            val text = ""
            assertEquals(HostingService.GitHub, HostingService.findBy(text))
        }
    }

    @Nested
    inner class findFromMergeCommitMessage {
        @Test
        fun `Should return null if message is empty`() {
            val hostingService = HostingService.findFromMergeCommitMessage("")
            assertNull(hostingService)
        }

        @Test
        fun `Should return null if message is not following the format`() {
            val hostingService = HostingService.findFromMergeCommitMessage("Pull Request #111")
            assertNull(hostingService)
        }

        @Test
        fun `Should return GitHub if message is github pr`() {
            val hostingService = HostingService.findFromMergeCommitMessage("Merge pull request #111")
            assertEquals(hostingService, HostingService.GitHub)
        }

        @Test
        fun `Should return GitLab if message is gitlab pr`() {
            val hostingService = HostingService.findFromMergeCommitMessage("See merge request !(111)")
            assertEquals(hostingService, HostingService.GitLab)
        }

        @Test
        fun `Should return Bitbucket if message is bitbucket pr`() {
            val hostingService = HostingService.findFromMergeCommitMessage("(pull request #111)")
            assertEquals(hostingService, HostingService.Bitbucket)
        }
    }

    @Nested
    inner class findFromSquashCommitMessage {
        @Test
        fun `Should return null if message is empty`() {
            val hostingService = HostingService.findFromSquashCommitMessage("")
            assertNull(hostingService)
        }

        @Test
        fun `Should return null if message is not following the format`() {
            val hostingService = HostingService.findFromSquashCommitMessage("Pull Request 111")
            assertNull(hostingService)
        }

        @Test
        fun `Should return GitHub if message is github pr`() {
            val hostingService = HostingService.findFromSquashCommitMessage("Pull Request (#111)")
            assertEquals(hostingService, HostingService.GitHub)
        }

        @Test
        fun `Should return GitLab if message is gitlab pr`() {
            val hostingService = HostingService.findFromSquashCommitMessage("See merge request !(111)")
            assertEquals(hostingService, HostingService.GitLab)
        }

        @Test
        fun `Should return Bitbucket if message is bitbucket pr`() {
            val hostingService = HostingService.findFromSquashCommitMessage("(pull request #111)")
            assertEquals(hostingService, HostingService.Bitbucket)
        }
    }

    @Nested
    inner class getPrNumberFromMergeCommit {

        @Nested
        inner class GitHub {
            @Test
            fun `Should return null if no PR number format`() {
                val prNumber = HostingService.GitHub.getPrNumberFromMergeCommit("pull request #111")
                assertNull(prNumber)
            }

            @Test
            fun `Should return null if empty message`() {
                val prNumber = HostingService.GitHub.getPrNumberFromMergeCommit("")
                assertNull(prNumber)
            }

            @Test
            fun `Should return pr number for GitHub`() {
                val prNumber = HostingService.GitHub.getPrNumberFromMergeCommit("Merge pull request #111")
                assertEquals(prNumber, 111)
            }
        }

        @Nested
        inner class GitLab {
            @Test
            fun `Should return null if no PR number format`() {
                val prNumber = HostingService.GitLab.getPrNumberFromMergeCommit("pull request #111")
                assertNull(prNumber)
            }

            @Test
            fun `Should return null if empty message`() {
                val prNumber = HostingService.GitLab.getPrNumberFromMergeCommit("")
                assertNull(prNumber)
            }

            @Test
            fun `Should return pr number for GitLab`() {
                val prNumber = HostingService.GitLab.getPrNumberFromMergeCommit("See merge request !112")
                assertEquals(prNumber, 112)
            }
        }

        @Nested
        inner class Bitbucket {
            @Test
            fun `Should return null if no PR number format`() {
                val prNumber = HostingService.Bitbucket.getPrNumberFromMergeCommit("pull request #111")
                assertNull(prNumber)
            }

            @Test
            fun `Should return null if empty message`() {
                val prNumber = HostingService.Bitbucket.getPrNumberFromMergeCommit("")
                assertNull(prNumber)
            }

            @Test
            fun `Should return pr number for Bitbucket`() {
                val prNumber = HostingService.Bitbucket.getPrNumberFromMergeCommit("(pull request #113)")
                assertEquals(prNumber, 113)
            }
        }
    }

    @Nested
    inner class getPrNumberFromSquashCommit {

        @Nested
        inner class GitHub {
            @Test
            fun `Should return null if no PR number format`() {
                val prNumber = HostingService.GitHub.getPrNumberFromSquashCommit("pull request #111")
                assertNull(prNumber)
            }

            @Test
            fun `Should return null if empty message`() {
                val prNumber = HostingService.GitHub.getPrNumberFromSquashCommit("")
                assertNull(prNumber)
            }

            @Test
            fun `Should return pr number for GitHub`() {
                val prNumber = HostingService.GitHub.getPrNumberFromSquashCommit("(#111)")
                assertEquals(prNumber, 111)
            }
        }

        @Nested
        inner class GitLab {
            @Test
            fun `Should return null if no PR number format`() {
                val prNumber = HostingService.GitLab.getPrNumberFromSquashCommit("pull request #111")
                assertNull(prNumber)
            }

            @Test
            fun `Should return null if empty message`() {
                val prNumber = HostingService.GitLab.getPrNumberFromSquashCommit("")
                assertNull(prNumber)
            }

            @Test
            fun `Should return pr number for GitLab`() {
                val prNumber = HostingService.GitLab.getPrNumberFromSquashCommit("See merge request !112")
                assertEquals(prNumber, 112)
            }
        }

        @Nested
        inner class Bitbucket {
            @Test
            fun `Should return null if no PR number format`() {
                val prNumber = HostingService.Bitbucket.getPrNumberFromSquashCommit("pull request #111")
                assertNull(prNumber)
            }

            @Test
            fun `Should return null if empty message`() {
                val prNumber = HostingService.Bitbucket.getPrNumberFromSquashCommit("")
                assertNull(prNumber)
            }

            @Test
            fun `Should return pr number for Bitbucket`() {
                val prNumber = HostingService.Bitbucket.getPrNumberFromSquashCommit("(pull request #113)")
                assertEquals(prNumber, 113)
            }
        }
    }
}