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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import git4idea.GitRevisionNumber
import git4idea.GitUtil
import git4idea.repo.GitRepository

class ListPullRequestToggleAction : ToggleAction() {

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
            // IntelliJ's limitation. I mean, I think I can close only this plugin's annotation but let's wait they support closing each annotation.
            editor.gutter.closeAllAnnotations()
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
        val model = FindPullRequestModel(project, editor, virtualFile, gitRepositoryService, gitUrlService, gitHistoryService)
        val map = hashMapOf<String, GitPullRequestInfo>()
        val fileAnnotation = GitConfService().getFileAnnotation(repository, virtualFile) ?: return
        fileAnnotation.setCloser {
            UIUtil.invokeLaterIfNeeded {
                if (!project.isDisposed) editor.gutter.closeAllAnnotations()
            }
        }
        fileAnnotation.setReloader { newFileAnnotation ->
            println(newFileAnnotation)
        }

        if (fileAnnotation.isClosed) return

        val disposable = Disposable { fileAnnotation.dispose() }

        if (fileAnnotation.file != null && fileAnnotation.file!!.isInLocalFileSystem) {
            val changesListener = ProjectLevelVcsManager.getInstance(project).annotationLocalChangesListener
            changesListener.registerAnnotation(fileAnnotation.file, fileAnnotation)
            Disposer.register(disposable, Disposable {
                    changesListener.unregisterAnnotation(fileAnnotation.file, fileAnnotation)
            })
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
                    Pair(commit.getNumberFromCommitMessage(hostingServices.squashCommitMessage), hostingServices)
                } else {
                    Pair(null, FindPullRequestHostingServices.findBy(config.getHosting()))
                }
            }

            map[hash] = GitPullRequestInfo(prNumber = pair.first, revisionNumber = revisionNumber, hostingServices = pair.second)
        }

        val provider = ListPullRequestTextAnnotationGutterProvider(map, virtualFile, fileAnnotation, model, repository)
        editor.gutter.registerTextAnnotation(provider, provider)
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