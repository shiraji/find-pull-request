package com.github.shiraji.findpullrequest.annotation

import com.github.shiraji.findpullrequest.domain.GitPullRequestInfo
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorGutterAction
import com.intellij.openapi.editor.TextAnnotationGutterProvider
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository
import java.awt.Color
import java.awt.Cursor

class ListPullRequestTextAnnotationGutterProvider(
    private val gitHashesMap: HashMap<String, GitPullRequestInfo>,
    val virtualFile: VirtualFile,
    private val fileAnnotation: FileAnnotation,
    private val model: FindPullRequestModel,
    private val repository: GitRepository,
    private val upToDateLineNumberProvider: UpToDateLineNumberProviderImpl,
) : TextAnnotationGutterProvider, EditorGutterAction {

    override fun getPopupActions(line: Int, editor: Editor?): MutableList<AnAction> {
        return mutableListOf()
    }

    override fun getColor(line: Int, editor: Editor?): ColorKey? {
        return EditorColors.ANNOTATIONS_COLOR
    }

    override fun getLineText(line: Int, editor: Editor?): String? {
        val currentLine = upToDateLineNumberProvider.getLineNumber(line)
        if (currentLine < 0) return ""
        val hash = fileAnnotation.getLineRevisionNumber(currentLine)?.asString() ?: ""
        val prNumber = gitHashesMap[hash]?.prNumber
        return if (prNumber != null) "#$prNumber" else gitHashesMap[hash]?.revisionNumber?.shortRev
    }

    override fun getToolTip(line: Int, editor: Editor?): String? {
        return null
    }

    override fun getStyle(line: Int, editor: Editor?): EditorFontType {
        return EditorFontType.PLAIN
    }

    override fun getBgColor(line: Int, editor: Editor?): Color? {
        return null
    }

    override fun gutterClosed() {
    }

    override fun doAction(lineNum: Int) {
        object : Task.Backgroundable(fileAnnotation.project, "Opening Pull Request...") {
            override fun run(indicator: ProgressIndicator) {
                val currentLine = upToDateLineNumberProvider.getLineNumber(lineNum)
                val hash = fileAnnotation.getLineRevisionNumber(currentLine)?.asString() ?: ""
                val gitPullRequestInfo = gitHashesMap[hash] ?: return
                val hostingService = gitPullRequestInfo.hostingServices ?: return
                val webRepoUrl = model.createWebRepoUrl(repository) ?: return

                if (gitPullRequestInfo.prNumber == null) {
                    val url =
                        model.createCommitUrl(repository, hostingService, webRepoUrl, gitPullRequestInfo.revisionNumber)

                    BrowserUtil.open(url)
                } else {
                    val path = hostingService.urlPathFormat.format(gitPullRequestInfo.prNumber)
                    val url = model.createPRUrl(repository, hostingService, path)

                    BrowserUtil.open("$webRepoUrl/$url")
                }
            }
        }.queue()
    }

    override fun getCursor(lineNum: Int): Cursor {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
}