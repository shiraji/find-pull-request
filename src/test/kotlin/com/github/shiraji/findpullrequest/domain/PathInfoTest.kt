package com.github.shiraji.findpullrequest.domain

import com.intellij.openapi.vcs.history.VcsRevisionNumber
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PathInfoTest {
    @Nested
    inner class Pr {
        @Nested
        inner class createPath {
            private val prNumber: Int = 100

            @Test
            fun `Should create path for GitHub`() {
                val prInfo = PathInfo.Pr(prNumber, HostingService.GitHub)
                val path = prInfo.createPath()
                assertEquals(path, "pull/100/files")
            }

            @Test
            fun `Should create path for GitLab`() {
                val prInfo = PathInfo.Pr(prNumber, HostingService.GitLab)
                val path = prInfo.createPath()
                assertEquals(path, "merge_requests/100/diffs")
            }

            @Test
            fun `Should create path for Bitbucket`() {
                val prInfo = PathInfo.Pr(prNumber, HostingService.Bitbucket)
                val path = prInfo.createPath()
                assertEquals(path, "pull-requests/100/diff")
            }
        }
    }

    @Nested
    inner class Commit {
        @Nested
        inner class createPath {
            private val revisionNumber: VcsRevisionNumber = mockk()

            @BeforeEach
            fun setUp() {
                every { revisionNumber.asString() } returns "123456"
            }

            @Test
            fun `Should create path for GitHub`() {
                val commitInfo = PathInfo.Commit(revisionNumber, HostingService.GitHub)
                val path = commitInfo.createPath()
                assertEquals(path, "commit/123456")
            }

            @Test
            fun `Should create path for GitLab`() {
                val commitInfo = PathInfo.Commit(revisionNumber, HostingService.GitLab)
                val path = commitInfo.createPath()
                assertEquals(path, "commit/123456")
            }

            @Test
            fun `Should create path for Bitbucket`() {
                val commitInfo = PathInfo.Commit(revisionNumber, HostingService.Bitbucket)
                val path = commitInfo.createPath()
                assertEquals(path, "commits/123456")
            }
        }
    }
}
