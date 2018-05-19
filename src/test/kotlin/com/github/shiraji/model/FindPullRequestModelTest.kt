package com.github.shiraji.model

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.Hash
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import junit.framework.TestCase.*
import org.jetbrains.plugins.github.util.GithubUtil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(GitHistoryUtils::class, GitCommit::class, GithubUtil::class, ChangeListManager::class)
class FindPullRequestModelTest {

    lateinit var model: FindPullRequestModel

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var project: Project

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var virtualFile: VirtualFile

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var editor: Editor

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var repository: GitRepository

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var vcsRevisionNumber: VcsRevisionNumber

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var virtualRoot: VirtualFile

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var gitRepository: GitRepository

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var changeListManager: ChangeListManager

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var change: Change

    private val prNumber = 10
    private val hashCode = "123"
    private val diffHashCode = "abc"
    private val selectedLine = 10
    private val diffSelectedLine = 100
    private val selectedOffline = 111

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

    private fun mockGetGitRepository(result: GitRepository? = gitRepository) {
        PowerMockito.`when`(GithubUtil.getGitRepository(project, virtualFile)).thenReturn(result)
    }

    private fun mockIsRepositoryOnGitHub(result: Boolean) {
        PowerMockito.`when`(GithubUtil.isRepositoryOnGitHub(gitRepository)).thenReturn(result)
    }

    private fun mockIsUnversioned(result: Boolean) {
        `when`(changeListManager.isUnversioned(virtualFile)).thenReturn(result)
    }

    private fun mockChangeType(result: Change.Type, ch: Change? = change) {
        `when`(changeListManager.getChange(virtualFile)).thenReturn(ch)
        ch?.let { `when`(it.type).thenReturn(result) }
    }

    private fun mockLineNumber(startLine: Int, endLine: Int) {
        val selectionModel = mock(SelectionModel::class.java)
        `when`(editor.selectionModel).thenReturn(selectionModel)
        `when`(selectionModel.selectionStart).thenReturn(selectedOffline)
        `when`(selectionModel.selectionEnd).thenReturn(selectedOffline)

        val document = mock(Document::class.java)
        `when`(editor.document).thenReturn(document)
        `when`(document.getLineNumber(anyInt()))
                .thenReturn(startLine)
                .thenReturn(endLine)
    }

    private fun generateMockGitCommit(hashCode: String = "", fullMessage: String = ""): GitCommit {
        val hash = generateMockHash(hashCode)
        return PowerMockito.mock(GitCommit::class.java).also {
            `when`(it.fullMessage).thenReturn(fullMessage)
            `when`(it.commitTime).thenReturn(Date().time)
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
        model = FindPullRequestModel(project, editor, virtualFile)

        setUpForCreatePullRequestPath()
        setUpForIsEnable()
    }

    private fun setUpForCreatePullRequestPath() {
        PowerMockito.mockStatic(GitHistoryUtils::class.java)

        `when`(repository.root).thenReturn(virtualRoot)
        `when`(vcsRevisionNumber.asString()).thenReturn(hashCode)
    }

    private fun setUpForIsEnable() {
        PowerMockito.mockStatic(GithubUtil::class.java)
        PowerMockito.mockStatic(ChangeListManager::class.java)

        `when`(project.isDisposed).thenReturn(false)
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
        val prCommit = generateMockGitCommit()
        mockFindPullRequestCommit(listOf(prCommit))

        val listOfCommits = generateMockGitCommit(fullMessage = "Foo (#$prNumber)")
        // for findCommitLog
        mockHistory(listOf(listOfCommits))

        val path = model.createPullRequestPath(repository, vcsRevisionNumber)
        assertEquals("pull/$prNumber/files", path)
    }

    @Test(expected = NoPullRequestFoundException::class)
    fun `No PR found if no PR and the commit is not squash commit`() {
        val prCommit = generateMockGitCommit()
        mockFindPullRequestCommit(listOf(prCommit))

        val listOfCommits = generateMockGitCommit()
        // for findCommitLog
        mockHistory(listOf(listOfCommits))

        model.createPullRequestPath(repository, vcsRevisionNumber)
        fail()
    }

    @Test
    fun `Found PR has different hash code but the commit is squash commit`() {
        val prCommit = generateMockGitCommit(fullMessage = "Merge pull request #$prNumber from")
        mockFindPullRequestCommit(listOf(prCommit))

        val mergeCommit = generateMockGitCommit(hashCode = diffHashCode)
        mockHistory(listOf(mergeCommit))

        val listOfCommits = generateMockGitCommit(fullMessage = "Foo (#$prNumber)")
        // for findCommitLog
        mockHistory(listOf(listOfCommits))

        val path = model.createPullRequestPath(repository, vcsRevisionNumber)
        assertEquals("pull/$prNumber/files", path)
    }

    @Test(expected = NoPullRequestFoundException::class)
    fun `Found PR has different hash code and its commit message is not squash commit`() {
        val prCommit = generateMockGitCommit(fullMessage = "Merge pull request #$prNumber from")
        mockFindPullRequestCommit(listOf(prCommit))

        val mergeCommit = generateMockGitCommit(hashCode = diffHashCode)
        mockHistory(listOf(mergeCommit))

        val listOfCommits = generateMockGitCommit()
        // for findCommitLog
        mockHistory(listOf(listOfCommits))

        model.createPullRequestPath(repository, vcsRevisionNumber)
        fail()
    }

    @Test
    fun `isEnable true`() {
        mockGetGitRepository()
        mockIsRepositoryOnGitHub(true)
        mockIsUnversioned(false)
        mockChangeType(Change.Type.MODIFICATION)
        mockLineNumber(startLine = selectedLine, endLine = selectedLine)

        assertTrue(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable true even the change is null`() {
        mockGetGitRepository()
        mockIsRepositoryOnGitHub(true)
        mockIsUnversioned(false)
        mockChangeType(Change.Type.MODIFICATION, null)
        mockLineNumber(startLine = selectedLine, endLine = selectedLine)

        assertTrue(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if no git repository`() {
        mockGetGitRepository(null)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if repository is not github one`() {
        mockGetGitRepository()
        mockIsRepositoryOnGitHub(false)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if the file is not versioned`() {
        mockGetGitRepository()
        mockIsRepositoryOnGitHub(true)
        mockIsUnversioned(true)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if the change type is New`() {
        mockGetGitRepository()
        mockIsRepositoryOnGitHub(true)
        mockIsUnversioned(false)
        mockChangeType(Change.Type.NEW)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if multiple line is selected`() {
        mockGetGitRepository()
        mockIsRepositoryOnGitHub(true)
        mockIsUnversioned(false)
        mockChangeType(Change.Type.MODIFICATION)
        mockLineNumber(startLine = selectedLine, endLine = diffSelectedLine)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

}