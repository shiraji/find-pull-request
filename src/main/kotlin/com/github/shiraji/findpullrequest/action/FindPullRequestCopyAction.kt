package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.exceptions.NoPullRequestFoundException
import com.github.shiraji.findpullrequest.helper.showErrorNotification
import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.github.shiraji.findpullrequest.model.isDebugMode
import com.github.shiraji.findpullrequest.model.isJumpToFile
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.TextTransferable
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.net.URLEncoder

class FindPullRequestCopyAction : BaseFindPullRequestAction() {

    override fun actionPerform(url: String) {
        CopyPasteManager.getInstance().setContents(TextTransferable(url))
        showInfoNotification("Copied!")
    }

}
