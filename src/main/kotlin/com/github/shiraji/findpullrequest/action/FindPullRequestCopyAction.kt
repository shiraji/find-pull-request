package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.helper.showInfoNotification
import com.github.shiraji.findpullrequest.model.isPopupAfterCopy
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.util.ui.TextTransferable

class FindPullRequestCopyAction : BaseFindPullRequestAction() {

    override fun actionPerform(e: AnActionEvent, url: String) {
        CopyPasteManager.getInstance().setContents(TextTransferable(url))

        val config = PropertiesComponent.getInstance(e.project)
        if (config.isPopupAfterCopy())
            showInfoNotification("Copied!")
    }

}
