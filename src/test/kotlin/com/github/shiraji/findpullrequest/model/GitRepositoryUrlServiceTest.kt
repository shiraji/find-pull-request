package com.github.shiraji.findpullrequest.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class GitRepositoryUrlServiceTest {

    private val gitRepositoryUrlService = GitRepositoryUrlService()

    @Nested
    inner class getUserFromRemoteUrl {

        @Test
        fun `Should get user from url`() {
            val got = gitRepositoryUrlService.getUserFromRemoteUrl("https://github.com/shiraji/find-pull-request.git")

            assertEquals("shiraji", got)
        }

        @Test
        fun `Should get user from git protocol url`() {
            val got = gitRepositoryUrlService.getUserFromRemoteUrl("git@github.com:shiraji/find-pull-request.git")

            assertEquals("shiraji", got)
        }

        @Test
        fun `Should get null if url is invalid`() {
            val got = gitRepositoryUrlService.getUserFromRemoteUrl("git@github.com:shiraji/find-pull-request")

            assertNull(got)
        }
    }

    @Nested
    inner class getRepositoryFromRemoteUrl {
        @Test
        fun `Should get repo from url`() {
            val got = gitRepositoryUrlService.getRepositoryFromRemoteUrl("https://github.com/shiraji/find-pull-request.git")

            assertEquals("find-pull-request", got)
        }

        @Test
        fun `Should get repo from git protocol url`() {
            val got = gitRepositoryUrlService.getRepositoryFromRemoteUrl("git@github.com:shiraji/find-pull-request.git")

            assertEquals("find-pull-request", got)
        }

        @Test
        fun `Should get null if url is invalid`() {
            val got = gitRepositoryUrlService.getRepositoryFromRemoteUrl("git@github.com:shiraji/find-pull-request")

            assertNull(got)
        }
    }

    @Nested
    inner class getHostFromUrl {
        @Test
        fun `Should get host from url`() {
            val got = gitRepositoryUrlService.getHostFromUrl("https://github.com/shiraji/find-pull-request.git")

            assertEquals("github.com", got)
        }

        @Test
        fun `Should get host from git protocol url`() {
            val got = gitRepositoryUrlService.getHostFromUrl("git@github.com:shiraji/find-pull-request.git")

            assertEquals("github.com", got)
        }

        @Test
        fun `Should get custom domain host from url`() {
            val got = gitRepositoryUrlService.getHostFromUrl("https://example.com/shiraji/find-pull-request.git")

            assertEquals("example.com", got)
        }
    }
}