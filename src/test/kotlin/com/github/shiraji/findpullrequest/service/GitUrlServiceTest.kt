package com.github.shiraji.findpullrequest.service

import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GitUrlServiceTest {

    private val gitRepository: GitRepository = mockk()

    private val gitUrlService: GitUrlService = GitUrlService(gitRepository)

    @Nested
    inner class hasOriginOrUpstreamRepository {
        @Test
        fun `Should return true if repo contains origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin"
            every { gitRemote.firstUrl } returns "firstURL"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.hasOriginOrUpstreamRepository()

            assertTrue(got)
        }

        @Test
        fun `Should return true if repo contains upstream`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "upstream"
            every { gitRemote.firstUrl } returns "firstURL"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.hasOriginOrUpstreamRepository()

            assertTrue(got)
        }

        @Test
        fun `Should return false if no repo contains upstream or origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "foo"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.hasOriginOrUpstreamRepository()

            assertFalse(got)
        }

        @Test
        fun `Should return false if remotes are empty`() {
            every { gitRepository.remotes } returns emptyList()

            val got = gitUrlService.findUpstreamUrl()

            assertNull(got)
        }
    }

    @Nested
    inner class findOriginUrl {

        @Test
        fun `Should return URL if repo contains origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin"
            val expect = "firstURL"
            every { gitRemote.firstUrl } returns expect
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.findOriginUrl()

            assertEquals(expect, got)
        }

        @Test
        fun `Should return null if repo contains no origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin2"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.findOriginUrl()

            assertNull(got)
        }

        @Test
        fun `Should return null if remotes are empty`() {
            every { gitRepository.remotes } returns emptyList()

            val got = gitUrlService.findOriginUrl()

            assertNull(got)
        }
    }

    @Nested
    inner class findUpstreamUrl {
        @Test
        fun `Should return URL if repo contains upstream`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "upstream"
            val expect = "firstURL"
            every { gitRemote.firstUrl } returns expect
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.findUpstreamUrl()

            assertEquals(expect, got)
        }

        @Test
        fun `Should return null if repo contains no origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = gitUrlService.findUpstreamUrl()

            assertNull(got)
        }

        @Test
        fun `Should return null if remotes are empty`() {
            every { gitRepository.remotes } returns emptyList()

            val got = gitUrlService.findUpstreamUrl()

            assertNull(got)
        }
    }

    @Nested
    inner class toURLPath {
        @Test
        fun `Should get host from url`() {
            val got = gitUrlService.toURLPath("https://github.com/shiraji/find-pull-request.git")

            assertEquals("github.com/shiraji/find-pull-request", got)
        }

        @Test
        fun `Should work with gitlab like format`() {
            val got = gitUrlService.toURLPath("https://gitlab.com/shiraji/group/subgroup/find-pull-request.git")

            assertEquals("gitlab.com/shiraji/group/subgroup/find-pull-request", got)
        }

        @Test
        fun `Should get host from git protocol url`() {
            val got = gitUrlService.toURLPath("git@github.com:shiraji/find-pull-request.git")

            assertEquals("github.com/shiraji/find-pull-request", got)
        }

        @Test
        fun `Should get custom domain host from url`() {
            val got = gitUrlService.toURLPath("https://example.com/shiraji/find-pull-request.git")

            assertEquals("example.com/shiraji/find-pull-request", got)
        }
    }
}