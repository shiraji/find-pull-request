package com.github.shiraji.model

import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.Hash
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(GitHistoryUtils::class, GitCommit::class)
class FindPullRequestModelTest {

    lateinit var model: FindPullRequestModel

    @Mock lateinit var event: AnActionEvent
    @Mock lateinit var project: Project
    @Mock lateinit var virtualFile: VirtualFile
    @Mock lateinit var editor: Editor
    @Mock lateinit var repository: GitRepository
    @Mock lateinit var vcsRevisionNumber: VcsRevisionNumber
    @Mock lateinit var virtualRoot: VirtualFile

    private val prNumber = 10
    private val hashCode = "123"
    private val diffHashCode = "abc"

    private fun mockFindPullRequestCommit(results: List<GitCommit?>) {
        PowerMockito.`when`(GitHistoryUtils.history(
                Matchers.eq(project),
                Matchers.eq(virtualRoot),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(results)
    }

    private fun mockHistory(results: List<GitCommit>) {
        PowerMockito.`when`(GitHistoryUtils.history(
                Matchers.eq(project),
                Matchers.eq(virtualRoot),
                Matchers.anyString())
        ).thenReturn(results)
    }

    private fun generateMockGitCommit(hashCode: String = "", fullMessage: String = ""): GitCommit {
        val hash = generateMockHash(hashCode)
        return PowerMockito.mock(GitCommit::class.java).also {
            `when`(it.fullMessage).thenReturn(fullMessage)
            `when`(it.id).thenReturn(hash)
        }
    }

    private fun generateMockHash(hashCode: String): Hash {
        return mock(Hash::class.java).also {
            `when`(it.asString()).thenReturn(hashCode)
        }
    }

    @Before
    fun setup() {
        `when`(event.getData(CommonDataKeys.PROJECT)).thenReturn(project)
        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(virtualFile)
        `when`(event.getData(CommonDataKeys.EDITOR)).thenReturn(editor)
        model = FindPullRequestModel(event)

        PowerMockito.mockStatic(GitHistoryUtils::class.java)

        `when`(repository.root).thenReturn(virtualRoot)
        `when`(vcsRevisionNumber.asString()).thenReturn(hashCode)
    }

    @Test
    fun `Finding pull request`() {
        val prCommit = generateMockGitCommit(fullMessage = "Merge pull request #$prNumber from")
        mockFindPullRequestCommit(listOf(prCommit))

        val mergeCommit = generateMockGitCommit(hashCode = hashCode)
        mockHistory(listOf(mergeCommit))

        val path = model.createPullRequestPath(repository, vcsRevisionNumber)
        assertEquals("pull/$prNumber/files", path)
    }

    @Test
    fun `Finding squash commit`() {
        val prCommit = null
        mockFindPullRequestCommit(listOf<GitCommit?>(prCommit))

        val listOfCommits = generateMockGitCommit(fullMessage = "Foo (#$prNumber)")
        // for findCommitLog
        mockHistory(listOf(listOfCommits))

        val path = model.createPullRequestPath(repository, vcsRevisionNumber)
        assertEquals("pull/$prNumber/files", path)
    }
}