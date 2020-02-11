package com.github.shiraji.findpullrequest.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class GitRepositoryUrlServiceTest {

    private val gitRepositoryUrlService = GitRepositoryUrlService()

    @Nested
    inner class toURLPath {
        @Test
        fun `Should get host from url`() {
            val got = gitRepositoryUrlService.toURLPath("https://github.com/shiraji/find-pull-request.git")

            assertEquals("github.com/shiraji/find-pull-request", got)
        }

        @Test
        fun `Should work with gitlab like format`() {
            val got = gitRepositoryUrlService.toURLPath("https://gitlab.com/shiraji/group/subgroup/find-pull-request.git")

            assertEquals("gitlab.com/shiraji/group/subgroup/find-pull-request", got)
        }

        @Test
        fun `Should get host from git protocol url`() {
            val got = gitRepositoryUrlService.toURLPath("git@github.com:shiraji/find-pull-request.git")

            assertEquals("github.com/shiraji/find-pull-request", got)
        }

        @Test
        fun `Should get custom domain host from url`() {
            val got = gitRepositoryUrlService.toURLPath("https://example.com/shiraji/find-pull-request.git")

            assertEquals("example.com/shiraji/find-pull-request", got)
        }
    }
}