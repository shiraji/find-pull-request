package com.github.shiraji.model

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.model.*
import com.intellij.ide.util.PropertiesComponent
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
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import junit.framework.TestCase.*
import org.junit.Before
import org.junit.Ignore
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
@PrepareForTest(
        GitHistoryUtils::class,
        GitCommit::class,
        ChangeListManager::class
)
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

    @Suppress("MemberVisibilityCanBePrivate")
    @Mock
    lateinit var conf: PropertiesComponent

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

    private fun mockGetGitRepository(result: GitRepository = gitRepository) {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("git@github.com:shiraji/find-pull-request.git")
        )
        `when`(result.remotes).thenReturn(listOf(remote))
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

    private fun generateGitRemote(
            name: String = "origin",
            urls: List<String> = listOf(""),
            pushUrls: Collection<String> = listOf(""),
            fetchRefSpecs: List<String> = listOf(""),
            pushRefSpecs: List<String> = listOf("")
    ) = GitRemote(name, urls, pushUrls, fetchRefSpecs, pushRefSpecs)

    @Before
    fun setup() {
        model = FindPullRequestModel(project, editor, virtualFile, conf)

        setUpForCreatePullRequestPath()
        setUpForIsEnable()
    }

    private fun mockConfig(isDisable: Boolean = false, isDebugMode: Boolean = false, isJumpToFile: Boolean = true, protocol: String = "https://") {
        doReturn(isDisable).`when`(conf).isDisable()
        doReturn(isDebugMode).`when`(conf).isDebugMode()
        doReturn(isJumpToFile).`when`(conf).isJumpToFile()
        doReturn(protocol).`when`(conf).getProtocol()
    }

    private fun setUpForCreatePullRequestPath() {
        PowerMockito.mockStatic(GitHistoryUtils::class.java)

        `when`(repository.root).thenReturn(virtualRoot)
        `when`(vcsRevisionNumber.asString()).thenReturn(hashCode)
    }

    private fun setUpForIsEnable() {
        PowerMockito.mockStatic(ChangeListManager::class.java)

        `when`(project.isDisposed).thenReturn(false)

        mockConfig()
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
        mockIsUnversioned(false)
        mockChangeType(Change.Type.MODIFICATION)
        mockLineNumber(startLine = selectedLine, endLine = selectedLine)

        assertTrue(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if the plugin is disabled`() {
        mockConfig(isDisable = true)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable true even the change is null`() {
        mockGetGitRepository()
        mockIsUnversioned(false)
        mockChangeType(Change.Type.MODIFICATION, null)
        mockLineNumber(startLine = selectedLine, endLine = selectedLine)

        assertTrue(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if no git repository`() {
        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if the file is not versioned`() {
        mockGetGitRepository()
        mockIsUnversioned(true)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if the change type is New`() {
        mockGetGitRepository()
        mockIsUnversioned(false)
        mockChangeType(Change.Type.NEW)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable false if multiple line is selected`() {
        mockGetGitRepository()
        mockIsUnversioned(false)
        mockChangeType(Change.Type.MODIFICATION)
        mockLineNumber(startLine = selectedLine, endLine = diffSelectedLine)

        assertFalse(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `createWebRepoUrl returns correct https url if there is upstream url`() {
        val remote = generateGitRemote(
                name = "upstream",
                urls = listOf("git@github.com:shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.com/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if there is origin url`() {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("git@github.com:shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.com/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is https format`() {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("https://github.com/shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.com/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is custom domain`() {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("https://github.enterprise.local/shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.enterprise.local/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns null if there is no remote for origin or upstream`() {
        val remote = generateGitRemote(
                name = "shiraji",
                urls = listOf("https://github.enterprise.local/shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertNull(result)
    }

    @Test
    fun `createWebRepoUrl returns null if the format is no user name`() {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("https://github.enterprise.local/shiraji/find-pull-request")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertNull(result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is https format for bitbucket`() {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("https://shiraji@bitbucket.org/shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://bitbucket.org/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is https format for gitlab`() {
        val remote = generateGitRemote(
                name = "origin",
                urls = listOf("https://gitlab.com/shiraji/find-pull-request.git")
        )
        `when`(gitRepository.remotes).thenReturn(listOf(remote))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://gitlab.com/shiraji/find-pull-request", result)
    }

}