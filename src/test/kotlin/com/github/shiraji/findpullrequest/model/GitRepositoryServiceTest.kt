package com.github.shiraji.findpullrequest.model

import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested

class GitRepositoryServiceTest {

    private val gitRepository: GitRepository = mockk()

    private val target: GitRepositoryService = GitRepositoryService()

    @Nested
    inner class hasOriginOrUpstreamRepository {
        @Test
        fun `Should return true if repo contains origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin"
            every { gitRemote.firstUrl } returns "firstURL"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = target.hasOriginOrUpstreamRepository(gitRepository)

            assertTrue(got)
        }

        @Test
        fun `Should return true if repo contains upstream`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "upstream"
            every { gitRemote.firstUrl } returns "firstURL"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = target.hasOriginOrUpstreamRepository(gitRepository)

            assertTrue(got)
        }

        @Test
        fun `Should return false if no repo contains upstream or origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "foo"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = target.hasOriginOrUpstreamRepository(gitRepository)

            assertFalse(got)
        }

        @Test
        fun `Should return false if remotes are empty`() {
            every { gitRepository.remotes } returns emptyList()

            val got = target.findUpstreamUrl(gitRepository)

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

            val got = target.findOriginUrl(gitRepository)

            assertEquals(expect, got)
        }

        @Test
        fun `Should return null if repo contains no origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin2"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = target.findOriginUrl(gitRepository)

            assertNull(got)
        }

        @Test
        fun `Should return null if remotes are empty`() {
            every { gitRepository.remotes } returns emptyList()

            val got = target.findOriginUrl(gitRepository)

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

            val got = target.findUpstreamUrl(gitRepository)

            assertEquals(expect, got)
        }

        @Test
        fun `Should return null if repo contains no origin`() {
            val gitRemote: GitRemote = mockk()
            every { gitRemote.name } returns "origin"
            every { gitRepository.remotes } returns listOf(gitRemote)

            val got = target.findUpstreamUrl(gitRepository)

            assertNull(got)
        }

        @Test
        fun `Should return null if remotes are empty`() {
            every { gitRepository.remotes } returns emptyList()

            val got = target.findUpstreamUrl(gitRepository)

            assertNull(got)
        }

    }
}