package com.github.shiraji.model

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.model.*
import com.github.shiraji.subtract
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.Hash
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FindPullRequestModelTest {

    companion object {
        private const val SELECTED_LINE = 100
        private const val SELECTED_START_OFFSET = 100
        private const val SELECTED_LINE_DIFF = 111
        private const val SELECTED_END_OFFSET = 111
        private const val HASH = "123"
        private const val HASH_DIFF = "abc"
        private const val PR_NUMBER = 10
    }

    lateinit var model: FindPullRequestModel

    @MockK
    lateinit var gitRepository: GitRepository

    @MockK
    lateinit var changeListManager: ChangeListManager

    @MockK
    lateinit var conf: PropertiesComponent

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var editor: Editor

    @MockK
    lateinit var virtualFile: VirtualFile

    @MockK
    lateinit var change: Change

    @MockK
    lateinit var selectionModel: SelectionModel

    @MockK
    lateinit var document: Document

    @MockK
    lateinit var vcsRevisionNumber: VcsRevisionNumber

    @MockK
    lateinit var virtualRoot: VirtualFile

    @MockK
    lateinit var fileAnnotation: FileAnnotation

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        model = FindPullRequestModel(project, editor, virtualFile, GitRepositoryService(), GitRepositoryUrlService(), conf)
    }

    private fun mockGitRepository(remotes: List<GitRemote>, root: VirtualFile = virtualRoot) {
        every { gitRepository.remotes } returns remotes
        every { gitRepository.root } returns root
    }

    private fun generateGitRemote(
            name: String = "origin",
            urls: List<String> = listOf("git@github.com:shiraji/find-pull-request.git"),
            pushUrls: Collection<String> = listOf(""),
            fetchRefSpecs: List<String> = listOf(""),
            pushRefSpecs: List<String> = listOf("")
    ) = GitRemote(name, urls, pushUrls, fetchRefSpecs, pushRefSpecs)


    private fun mockIsUnversioned(result: Boolean = false) {
        every { changeListManager.isUnversioned(virtualFile) } returns result
    }

    private fun mockChangeType(result: Change.Type, ch: Change? = change) {
        every { changeListManager.getChange(virtualFile) } returns ch
        ch?.let { every { it.type } returns result }
    }

    private fun mockLineNumber(startLine: Int, endLine: Int) {
        every { editor.selectionModel } returns selectionModel
        every { selectionModel.selectionStart } returns SELECTED_START_OFFSET
        every { selectionModel.selectionEnd } returns SELECTED_END_OFFSET
        every { editor.document } returns document
        every { document.getLineNumber(SELECTED_START_OFFSET) } returns startLine
        every { document.getLineNumber(SELECTED_END_OFFSET) } returns endLine
    }

    private fun generateCommitId(hashCode: String): Hash {
        return mockk {
            every { this@mockk.asString() } returns hashCode
        }
    }

    private fun generateGitCommit(hashCode: String = "", fullMessage: String = ""): GitCommit {
        val hash = generateCommitId(hashCode)
        return mockk {
            every { this@mockk.fullMessage } returns fullMessage
            every { this@mockk.commitTime } returns java.util.Date().time
            every { this@mockk.id } returns hash
        }
    }

    private fun mockConfig(isDisable: Boolean = false, isDebugMode: Boolean = false, isJumpToFile: Boolean = true, protocol: String = "https://", hostingServices: String = "GitHub") {
        every { conf.isDisable() } returns isDisable
        every { conf.isDebugMode() } returns isDebugMode
        every { conf.isJumpToFile() } returns isJumpToFile
        every { conf.getProtocol() } returns protocol
        every { conf.getHosting() } returns hostingServices
    }

    private fun mockProject(isDisposed: Boolean = false) {
        every { project.isDisposed } returns isDisposed
    }

    private fun mockRevisionNumber(hashCode: String) {
        every { vcsRevisionNumber.asString() } returns hashCode
    }

    private fun mockHistory(closestPRCommits: List<GitCommit>, mergeCommits: List<GitCommit>) {
        mockkStatic(GitHistoryUtils::class)

        every {
            GitHistoryUtils.history(
                    project,
                    virtualRoot,
                    any(),
                    any(),
                    any(),
                    any())
        } returns closestPRCommits

        every {
            GitHistoryUtils.history(project, virtualRoot, any())
        } returns mergeCommits
    }

    private fun mockFileAnnotation(filePath: String = "README.md") {
        val baseDir = ""
        every { gitRepository.project.baseDir.canonicalPath?.plus("/") } returns baseDir
        every { gitRepository.vcs?.annotationProvider?.annotate(virtualFile) } returns fileAnnotation
        every { fileAnnotation.file?.canonicalPath?.subtract(baseDir) } returns filePath
    }

    @Test
    fun `Finding pull request`() {
        val prCommit1 = generateGitCommit(hashCode = HASH, fullMessage = "Merge pull request #$PR_NUMBER from")
        val prCommit2 = generateGitCommit(hashCode = HASH_DIFF, fullMessage = "Merge pull request #${PR_NUMBER + 1} from")
        val prCommit3 = generateGitCommit(hashCode = HASH_DIFF, fullMessage = "Merge pull request #${PR_NUMBER + 2} from")

        mockConfig()
        mockGitRepository(listOf(generateGitRemote()))
        mockHistory(closestPRCommits = listOf(prCommit1), mergeCommits = listOf(prCommit1, prCommit2, prCommit3))
        mockRevisionNumber(HASH)
        mockFileAnnotation()

        val path = model.createPullRequestPath(gitRepository, vcsRevisionNumber)
        assertEquals("pull/$PR_NUMBER/files#diff-4c6e90faac2675aa89e2176d2eec7d8", path)
    }

    @Test
    fun `Finding squash commit`() {
        val prCommit1 = generateGitCommit(hashCode = HASH, fullMessage = "Foo (#$PR_NUMBER)")

        mockConfig()
        mockGitRepository(listOf(generateGitRemote()))
        mockHistory(closestPRCommits = emptyList(), mergeCommits = listOf(prCommit1))
        mockFileAnnotation()

        val path = model.createPullRequestPath(gitRepository, vcsRevisionNumber)
        assertEquals("pull/$PR_NUMBER/files#diff-4c6e90faac2675aa89e2176d2eec7d8", path)
    }

    @Test
    fun `No PR found if no PR and the commit is not squash commit`() {
        val prCommit1 = generateGitCommit(hashCode = HASH, fullMessage = "Merge pull request #$PR_NUMBER from")

        mockConfig()
        mockGitRepository(listOf(generateGitRemote()))
        mockHistory(closestPRCommits = emptyList(), mergeCommits = listOf(prCommit1))

        assertThrows<NoPullRequestFoundException> {
            model.createPullRequestPath(gitRepository, vcsRevisionNumber)
        }
    }

    @Test
    fun `Found PR has different hash code but the commit is squash commit`() {
        val prCommit1 = generateGitCommit(hashCode = HASH_DIFF, fullMessage = "Merge pull request #${PR_NUMBER + 1} from")
        val prCommit2 = generateGitCommit(hashCode = HASH_DIFF, fullMessage = "Foo (#${PR_NUMBER})")
        val prCommit3 = generateGitCommit(hashCode = HASH, fullMessage = "Foo (#${PR_NUMBER})")

        mockConfig()
        mockGitRepository(listOf(generateGitRemote()))
        mockkStatic(GitHistoryUtils::class)
        mockRevisionNumber(HASH)
        mockFileAnnotation()

        every {
            GitHistoryUtils.history(
                    project,
                    virtualRoot,
                    any(),
                    any(),
                    any(),
                    any())
        } returns listOf(prCommit1)

        every {
            GitHistoryUtils.history(project, virtualRoot, any())
        } returnsMany listOf(listOf(prCommit2), listOf(prCommit3))

        val path = model.createPullRequestPath(gitRepository, vcsRevisionNumber)
        assertEquals("pull/$PR_NUMBER/files#diff-4c6e90faac2675aa89e2176d2eec7d8", path)
        verify(exactly = 1) { GitHistoryUtils.history(project, virtualRoot, any(), any(), any(), any()) }
        verify(exactly = 2) { GitHistoryUtils.history(project, virtualRoot, any()) }
    }

    @Test
    fun `Found PR has different hash code and its commit message is not squash commit`() {
        val prCommit1 = generateGitCommit(hashCode = HASH_DIFF, fullMessage = "Merge pull request #${PR_NUMBER} from")
        val prCommit2 = generateGitCommit(hashCode = HASH_DIFF, fullMessage = "Foo (#${PR_NUMBER})")
        val prCommit3 = generateGitCommit(hashCode = HASH, fullMessage = "Merge pull request #${PR_NUMBER} from")

        mockConfig()
        mockGitRepository(listOf(generateGitRemote()))
        mockkStatic(GitHistoryUtils::class)
        mockRevisionNumber(HASH)

        every {
            GitHistoryUtils.history(
                    project,
                    virtualRoot,
                    any(),
                    any(),
                    any(),
                    any())
        } returns listOf(prCommit1)

        every {
            GitHistoryUtils.history(project, virtualRoot, any())
        } returnsMany listOf(listOf(prCommit2), listOf(prCommit3))

        assertThrows<NoPullRequestFoundException> {
            model.createPullRequestPath(gitRepository, vcsRevisionNumber)
        }
    }

    @Test
    fun `isEnable false if the plugin is disabled`() {
        mockConfig(isDisable = true)

        assertFalse(model.isEnable(gitRepository, changeListManager))

        verify(exactly = 1) { conf.isDisable() }
    }

    @Test
    fun `isEnable false if project is disposed`() {
        mockConfig()
        mockProject(isDisposed = true)

        assertFalse(model.isEnable(gitRepository, changeListManager))

        verify(exactly = 1) { project.isDisposed }
    }

    @Test
    fun `isEnable false if no git repository`() {
        mockConfig()
        mockProject()
        mockGitRepository(emptyList())

        assertFalse(model.isEnable(gitRepository, changeListManager))

        verify { gitRepository.remotes }
    }

    @Test
    fun `isEnable false if the file is not versioned`() {
        mockConfig()
        mockProject()
        mockGitRepository(listOf(generateGitRemote()))
        mockIsUnversioned(true)

        assertFalse(model.isEnable(gitRepository, changeListManager))

        verify(exactly = 1) { changeListManager.isUnversioned(virtualFile) }
    }

    @Test
    fun `isEnable false if the change type is New`() {
        mockConfig()
        mockProject()
        mockGitRepository(listOf(generateGitRemote()))
        mockIsUnversioned()
        mockChangeType(Change.Type.NEW)

        assertFalse(model.isEnable(gitRepository, changeListManager))

        verify(exactly = 1) { changeListManager.getChange(virtualFile) }
    }

    @Test
    fun `isEnable false if multiple line is selected`() {
        mockConfig()
        mockProject()
        mockGitRepository(listOf(generateGitRemote()))
        mockIsUnversioned()
        mockChangeType(Change.Type.MODIFICATION)
        mockLineNumber(startLine = SELECTED_LINE, endLine = SELECTED_LINE_DIFF)

        assertFalse(model.isEnable(gitRepository, changeListManager))

        verify { document.getLineNumber(any()) }
    }

    @Test
    fun `isEnable true`() {
        mockConfig()
        mockProject()
        mockGitRepository(listOf(generateGitRemote()))
        mockIsUnversioned()
        mockChangeType(Change.Type.MODIFICATION)
        mockLineNumber(startLine = SELECTED_LINE, endLine = SELECTED_LINE)

        assertTrue(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `isEnable true even the change is null`() {
        mockConfig()
        mockProject()
        mockGitRepository(listOf(generateGitRemote()))
        mockIsUnversioned()
        mockChangeType(Change.Type.MODIFICATION, null)
        mockLineNumber(startLine = SELECTED_LINE, endLine = SELECTED_LINE)

        assertTrue(model.isEnable(gitRepository, changeListManager))
    }

    @Test
    fun `createWebRepoUrl returns correct https url if there is upstream url`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(name = "upstream")))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.com/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if there is origin url`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote()))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.com/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is https format`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(
                urls = listOf("https://github.com/shiraji/find-pull-request.git")
        )))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.com/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is custom domain`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(
                urls = listOf("https://github.enterprise.local/shiraji/find-pull-request.git")
        )))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://github.enterprise.local/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns null if there is no remote for origin or upstream`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(
                name = "shiraji",
                urls = listOf("https://github.enterprise.local/shiraji/find-pull-request.git")
        )))

        val result = model.createWebRepoUrl(gitRepository)

        assertNull(result)
    }

    @Test
    fun `createWebRepoUrl returns null if the format is no user name`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(
                urls = listOf("https://github.enterprise.local/shiraji/find-pull-request")
        )))

        val result = model.createWebRepoUrl(gitRepository)

        assertNull(result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is https format for bitbucket`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(
                urls = listOf("https://shiraji@bitbucket.org/shiraji/find-pull-request.git")
        )))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://bitbucket.org/shiraji/find-pull-request", result)
    }

    @Test
    fun `createWebRepoUrl returns correct https url if url is https format for gitlab`() {
        mockConfig()
        mockGitRepository(listOf(generateGitRemote(
                urls = listOf("https://gitlab.com/shiraji/find-pull-request.git")
        )))

        val result = model.createWebRepoUrl(gitRepository)

        assertEquals("https://gitlab.com/shiraji/find-pull-request", result)
    }

}