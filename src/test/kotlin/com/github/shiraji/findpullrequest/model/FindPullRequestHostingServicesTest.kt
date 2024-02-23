package com.github.shiraji.findpullrequest.model

import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.subtract
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FindPullRequestHostingServicesTest {

    @Nested
    inner class createFileAnchorValue {
        val project: Project = mockk()
        val repository: GitRepository = mockk()

        @BeforeEach
        fun setUp() {
            mockkStatic(LocalFileSystem::class)
            every { LocalFileSystem.getInstance().findFileByPath(any()) } returns null
            every { project.basePath } returns ""
            every { project.root?.canonicalPath } returns ""
            every { repository.project } returns project
        }

        @Test
        fun `Should create GitHub style of file anchor`() {
            val annotate: FileAnnotation = mockk()
            every { annotate.file?.canonicalPath?.subtract("") } returns "aaa"

            val got = FindPullRequestHostingServices.GitHub.createFileAnchorValue(repository, annotate)

            assertEquals("#diff-9834876dcfb05cb167a5c24953eba58c4ac89b1adf57f28f2f9d09af107ee8f0", got)
        }

        @Test
        fun `Should create GitLab style of file anchor`() {
            val annotate: FileAnnotation = mockk()
            every { annotate.file?.canonicalPath?.subtract("") } returns "aaa"

            val got = FindPullRequestHostingServices.GitLab.createFileAnchorValue(repository, annotate)

            assertEquals("#7e240de74fb1ed08fa08d38063f6a6a91462a815", got)
        }

        @Test
        fun `Should create Bitbucket style of file anchor`() {
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