package com.github.shiraji.findpullrequest.model

import com.github.shiraji.subtract
import com.intellij.openapi.vcs.annotate.FileAnnotation
import git4idea.repo.GitRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FindPullRequestHostingServicesTest {

    @Nested
    inner class createFileAnchorValue {
        @Test
        fun `Should create GitHub style of file anchor`() {

            val repository: GitRepository = mockk()
            every { repository.project.baseDir.canonicalPath } returns ""

            val annotate: FileAnnotation = mockk()
            every { annotate.file?.canonicalPath?.subtract("") } returns "aaa"

            val got = FindPullRequestHostingServices.GitHub.createFileAnchorValue(repository, annotate)

            assertEquals("#diff-47bce5c74f589f4867dbd57e9ca9f808", got)
        }

        @Test
        fun `Should create GitLab style of file anchor`() {

            val repository: GitRepository = mockk()
            every { repository.project.baseDir.canonicalPath } returns ""

            val annotate: FileAnnotation = mockk()
            every { annotate.file?.canonicalPath?.subtract("") } returns "aaa"

            val got = FindPullRequestHostingServices.GitLab.createFileAnchorValue(repository, annotate)

            assertEquals("#7e240de74fb1ed08fa08d38063f6a6a91462a815", got)
        }

        @Test
        fun `Should create Bitbucket style of file anchor`() {

            val repository: GitRepository = mockk()
            every { repository.project.baseDir.canonicalPath } returns ""

            val annotate: FileAnnotation = mockk()
            every { annotate.file?.canonicalPath?.subtract("") } returns "aaa"

            val got = FindPullRequestHostingServices.Bitbucket.createFileAnchorValue(repository, annotate)

            assertEquals("#chg-aaa", got)
        }
    }

    @Nested
    inner class findBy {
        @Test
        fun `Should find GitHub if arg is GitHub`() {
            val text = "GitHub"
            assertEquals(FindPullRequestHostingServices.GitHub, FindPullRequestHostingServices.findBy(text))
        }

        @Test
        fun `Should find GitLab if arg is GitLab`() {
            val text = "GitLab"
            assertEquals(FindPullRequestHostingServices.GitLab, FindPullRequestHostingServices.findBy(text))
        }

        @Test
        fun `Should find Bitbucket if arg is Bitbucket`() {
            val text = "Bitbucket"
            assertEquals(FindPullRequestHostingServices.Bitbucket, FindPullRequestHostingServices.findBy(text))
        }

        @Test
        fun `Should find GitHub if arg is invalid`() {
            val text = ""
            assertEquals(FindPullRequestHostingServices.GitHub, FindPullRequestHostingServices.findBy(text))
        }
    }
}