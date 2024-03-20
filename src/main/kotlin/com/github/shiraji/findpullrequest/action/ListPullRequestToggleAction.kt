package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.annotation.ListPullRequestTextAnnotationGutterProvider
import com.github.shiraji.findpullrequest.domain.GitPullRequestInfo
import com.github.shiraji.findpullrequest.helper.root
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.github.shiraji.findpullrequest.model.GitConfService
import com.github.shiraji.findpullrequest.model.GitHistoryService
import com.github.shiraji.findpullrequest.model.GitRepositoryUrlService
import com.github.shiraji.findpullrequest.model.getHosting
import com.github.shiraji.getNumberFromCommitMessage
import com.github.shiraji.isSquashPullRequestCommit
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import git4idea.GitRevisionNumber
import git4idea.GitUtil
import git4idea.repo.GitRepository

class ListPullRequestToggleAction : ToggleAction() {

    private fun isEnabled(e: AnActionEvent, project: Project): Boolean {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selectedFiles == null || selectedFiles.size != 1) return false

        val file = selectedFiles[0]
        return !file.isDirectory && !file.fileType.isBinary && file.isInLocalFileSystem
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        if (project.isDisposed) return
        val config = PropertiesComponent.getInstance(project) ?: return
        e.presentation.isEnabled = isEnabled(e, project)
        e.presentation.text = "List ${FindPullRequestHostingServices.findBy(config.getHosting()).pullRequestName}"
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return false
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return false
        val gutter = editor.gutter as? EditorGutterComponentEx ?: return false
        return gutter.textAnnotations.filterIsInstance(ListPullRequestTextAnnotationGutterProvider::class.java).firstOrNull {
            it.virtualFile.path == virtualFile.path
        } != null
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
            listPRs(e)
        } else {
            val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            closeListPullRequestTextAnnotationGutterProvider(editor, virtualFile)
        }
    }

    private fun closeListPullRequestTextAnnotationGutterProvider(editor: Editor, virtualFile: VirtualFile) {
        val closeProviders =
            editor.gutter.textAnnotations.filterIsInstance(ListPullRequestTextAnnotationGutterProvider::class.java)
                .filter { it.virtualFile == virtualFile }
        if (closeProviders.isNotEmpty()) {
            editor.gutter.closeTextAnnotations(closeProviders)
        }
    }

    private fun listPRs(e: AnActionEvent) {
        val project: Project = e.getData(CommonDataKeys.PROJECT) ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val config: PropertiesComponent = PropertiesComponent.getInstance(project)
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val repository = getGitRepository(project, virtualFile) ?: return
        val gitHistoryService = GitHistoryService()
        val revisionHashes = gitHistoryService.findRevisionHashes(project, repository, virtualFile)
        val gitRepositoryService = GitConfService()
        val gitUrlService = GitRepositoryUrlService()
        val model =
            FindPullRequestModel(project, editor, virtualFile, gitRepositoryService, gitUrlService, gitHistoryService)
        val map = hashMapOf<String, GitPullRequestInfo>()

        object : Task.Backgroundable(project, "Listing Pull Request...") {
            override fun run(indicator: ProgressIndicator) {
                val fileAnnotation = GitConfService().getFileAnnotation(repository, virtualFile) ?: return
                fileAnnotation.setCloser {
                    UIUtil.invokeLaterIfNeeded {
                        if (!project.isDisposed) {
                            closeListPullRequestTextAnnotationGutterProvider(editor, virtualFile)
                        }
                    }
                }

                if (fileAnnotation.isClosed) return

                val disposable = Disposable { fileAnnotation.dispose() }

                if (fileAnnotation.file != null && fileAnnotation.file!!.isInLocalFileSystem) {
                    val changesListener = ProjectLevelVcsManager.getInstance(project).annotationLocalChangesListener
                    changesListener.registerAnnotation(fileAnnotation)
                    Disposer.register(disposable) {
                        changesListener.unregisterAnnotation(fileAnnotation)
                    }
                }

                // Not sure why but fileAnnotation.revisions does not return local commits.
                // To resolve that, this plugin uses gitHistoryService.findRevisionHashes() to list all hashes of current file
                revisionHashes.distinct().forEach { hash ->
                    if (map.containsKey(hash)) return@forEach
                    val revisionNumber = try {
                        GitRevisionNumber.resolve(project, repository.root, hash)
                    } catch (e: VcsException) {
                        return@forEach
                    }
                    val pullRequestCommit = gitHistoryService.findMergedCommit(project, repository, revisionNumber)
                    val pair = if (pullRequestCommit != null && gitHistoryService.hasCommitsFromRevisionNumber(
                            gitHistoryService.listCommitsFromMergedCommit(
                                project,
                                repository,
                                pullRequestCommit
                            ), revisionNumber
                        )
                    ) {
                        val hosting = FindPullRequestHostingServices.findBy(config.getHosting())
                        val prNumberUsingConfig =
                            pullRequestCommit.getNumberFromCommitMessage(hosting.defaultMergeCommitMessage)

                        if (prNumberUsingConfig == null) {
                            // Check if the merge commit message comes from other supporting hosting service
                            gitHistoryService.findPrNumberAndHostingService(pullRequestCommit)
                        } else {
                            Pair(prNumberUsingConfig, hosting)
                        }
                    } else {
                        val commit = gitHistoryService.findCommitLog(project, repository, revisionNumber)
                        val hostingServices = FindPullRequestHostingServices.values().firstOrNull {
                            commit.isSquashPullRequestCommit(it)
                        }

                        if (hostingServices != null) {
                            Pair(
                                commit.getNumberFromCommitMessage(hostingServices.squashCommitMessage),
                                hostingServices
                            )
                        } else {
                            Pair(null, FindPullRequestHostingServices.findBy(config.getHosting()))
                        }
                    }

                    map[hash] = GitPullRequestInfo(
                        prNumber = pair.first,
                        revisionNumber = revisionNumber,
                        hostingServices = pair.second
                    )
                }

                ApplicationManager.getApplication().invokeLater {
                    val upToDateLineNumberProvider = UpToDateLineNumberProviderImpl(editor.document, project)
                    val provider = ListPullRequestTextAnnotationGutterProvider(
                        map,
                        virtualFile,
                        fileAnnotation,
                        model,
                        repository,
                        upToDateLineNumberProvider
                    )
                    editor.gutter.registerTextAnnotation(provider, provider)
                }
            }
        }.queue()
    }

    private fun getGitRepository(project: Project, file: VirtualFile?): GitRepository? {
        val manager = GitUtil.getRepositoryManager(project)
        val repositories = manager.repositories
        return when (repositories.size) {
            0 -> null
            1 -> repositories[0]
            else -> {
                if (file != null) {
                    val repository = manager.getRepositoryForFile(file)
                    if (repository != null) return repository
                }
                val root = project.root ?: return null
                manager.getRepositoryForFile(root)
            }
        }
    }
}