package com.github.shiraji.findpullrequest.model

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FindPullRequestModel {
    val project : Project?
    val editor : Editor?
    val virtualFile: VirtualFile?

    constructor(e: AnActionEvent) {
        project = e.getData(CommonDataKeys.PROJECT)
        editor = e.getData(CommonDataKeys.EDITOR)
        virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
    }

    fun isValid(): Boolean {
        return project != null
                && !project.isDisposed
                && editor != null
                && virtualFile != null
    }
}