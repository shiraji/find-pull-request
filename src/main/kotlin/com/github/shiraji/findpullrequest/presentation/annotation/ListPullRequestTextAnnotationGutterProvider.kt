package com.github.shiraji.findpullrequest.presentation.annotation

import com.github.shiraji.findpullrequest.domain.PathInfo
import com.github.shiraji.findpullrequest.service.CreateUrlService
import com.github.shiraji.findpullrequest.service.UpToDateLineNumberProviderService
import com.intellij.dvcs.DvcsUtil
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import java.awt.Cursor

class ListPullRequestTextAnnotationGutterProvider(
    private val project: Project,
    private val gitHashesMap: HashMap<String, PathInfo>,
    val virtualFile: VirtualFile,
    private val fileAnnotation: FileAnnotation,
    private val upToDateLineNumberProviderService: UpToDateLineNumberProviderService,
    private val createUrlService: CreateUrlService
) : TextAnnotationGutterProvider, EditorGutterAction {

    override fun getPopupActions(line: Int, editor: Editor?): MutableList<AnAction> {
        return mutableListOf()
    }

    override fun getColor(line: Int, editor: Editor?): ColorKey? {
        return EditorColors.ANNOTATIONS_COLOR
    }

    override fun getLineText(line: Int, editor: Editor?): String? {
        // TODO move to scenario
        val currentLine = upToDateLineNumberProviderService.getLineNumber(line)
        if (currentLine < 0) return null
        val hash = fileAnnotation.getLineRevisionNumber(currentLine)?.asString() ?: ""
        return when (val pathInfo = gitHashesMap[hash]) {
            is PathInfo.Pr -> "#${pathInfo.prNumber}"
            is PathInfo.Commit -> DvcsUtil.getShortHash(pathInfo.revisionNumber.asString())
            else -> null
        }
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
        object : Task.Backgroundable(project, "Opening Pull Requests...") {
            override fun run(indicator: ProgressIndicator) {
                // TODO move to scenario
                val hash = fileAnnotation.getLineRevisionNumber(lineNum)?.asString() ?: ""
                val pathInfo = gitHashesMap[hash] ?: return
                val url = createUrlService.createUrl(pathInfo)
                BrowserUtil.open(url)
            }
        }.queue()
    }

    override fun getCursor(lineNum: Int): Cursor {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
}