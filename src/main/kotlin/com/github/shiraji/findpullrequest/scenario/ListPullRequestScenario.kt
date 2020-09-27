package com.github.shiraji.findpullrequest.scenario

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.config.isDisable
import com.github.shiraji.findpullrequest.domain.HostingService
import com.github.shiraji.findpullrequest.domain.PathInfo
import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.exceptions.UnsupportedHostingServiceException
import com.github.shiraji.findpullrequest.presentation.annotation.ListPullRequestTextAnnotationGutterProvider
import com.github.shiraji.findpullrequest.service.CreateUrlService
import com.github.shiraji.findpullrequest.service.FindPrInfoService
import com.github.shiraji.findpullrequest.service.GitHistoryService
import com.github.shiraji.findpullrequest.service.GitRepositoryService
import com.github.shiraji.findpullrequest.service.UpToDateLineNumberProviderService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.changes.VcsAnnotationLocalChangesListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import org.koin.core.KoinComponent
import org.koin.core.inject

class ListPullRequestScenario : KoinComponent {
    private val project: Project by inject()
    private val config: PropertiesComponent by inject()
    private val editor: Editor by inject()
    private val virtualFile: VirtualFile by inject()
    private val selectedFiles: Array<VirtualFile> by inject()
    private val gitService: GitHistoryService by inject()
    private val gitRepositoryService: GitRepositoryService by inject()
    private val vcsAnnotationLocalChangesListener: VcsAnnotationLocalChangesListener by inject()
    private val hashMap: HashMap<String, PathInfo> by inject()
    private val findPrInfoService: FindPrInfoService by inject()
    private val upToDateLineNumberProviderService: UpToDateLineNumberProviderService by inject()
    private val createUrlService: CreateUrlService by inject()

    fun annotate() {
        val fileAnnotation = gitRepositoryService.getFileAnnotation(virtualFile)
        registerFileAnnotate(fileAnnotation)
        gitService.findRevisionHashes(virtualFile).distinct().forEach { hash ->
            if (hashMap.containsKey(hash)) return@forEach
            val revisionNumber = try {
                gitService.toVcsRevisionNumber(hash)
            } catch (e: VcsException) {
                null
            } ?: return@forEach
            val mergeCommit = gitService.findMergedCommit(revisionNumber)
            hashMap[hash] = try {
                findPrInfoService.findPrInfo(revisionNumber, mergeCommit)
            } catch (e: NoPullRequestFoundException) {
                PathInfo.Commit(
                    revisionNumber = revisionNumber,
                    hostingService = HostingService.findBy(config.getHosting())
                )
            } catch (e: UnsupportedHostingServiceException) {
                PathInfo.Commit(
                    revisionNumber = revisionNumber,
                    hostingService = HostingService.findBy(config.getHosting())
                )
            }
        }

        ApplicationManager.getApplication().invokeLater {
            val provider =
                ListPullRequestTextAnnotationGutterProvider(
                    project,
                    hashMap,
                    virtualFile,
                    fileAnnotation,
                    upToDateLineNumberProviderService,
                    createUrlService
                )
            editor.gutter.registerTextAnnotation(provider, provider)
        }
    }

    private fun registerFileAnnotate(fileAnnotation: FileAnnotation) {
        fileAnnotation.setCloser {
            UIUtil.invokeLaterIfNeeded {
                if (!project.isDisposed) {
                    closeListPullRequestTextAnnotationGutterProvider()
                }
            }
        }

        fileAnnotation.setReloader { _ ->
            // println(newFileAnnotation)
        }

        if (fileAnnotation.isClosed) return

        val disposable = Disposable { fileAnnotation.dispose() }

        if (fileAnnotation.file != null && fileAnnotation.file!!.isInLocalFileSystem) {
            vcsAnnotationLocalChangesListener.registerAnnotation(fileAnnotation.file, fileAnnotation)
            Disposer.register(disposable, Disposable {
                vcsAnnotationLocalChangesListener.unregisterAnnotation(fileAnnotation.file, fileAnnotation)
            })
        }
    }

    fun closeListPullRequestTextAnnotationGutterProvider() {
        val closeProviders =
            editor.gutter.textAnnotations.filterIsInstance(ListPullRequestTextAnnotationGutterProvider::class.java)
                .filter { it.virtualFile == virtualFile }
        if (closeProviders.isNotEmpty()) {
            editor.gutter.closeTextAnnotations(closeProviders)
        }
    }

    fun isSelectedListPullRequestAction(): Boolean {
        val gutter = editor.gutter as? EditorGutterComponentEx ?: return false
        return gutter.textAnnotations.filterIsInstance(ListPullRequestTextAnnotationGutterProvider::class.java).firstOrNull {
            it.virtualFile.path == virtualFile.path
        } != null
    }

    fun isEnabledListPullRequestAction(): Boolean {
        if (project.isDisposed) return false
        if (config.isDisable()) return false
        if (selectedFiles.size != 1) return false
        val file = selectedFiles[0]
        return !file.isDirectory && !file.fileType.isBinary && file.isInLocalFileSystem
    }
}