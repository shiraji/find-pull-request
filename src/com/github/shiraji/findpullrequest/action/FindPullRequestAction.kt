package com.github.shiraji.findpullrequest.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitRevisionNumber
import git4idea.GitVcs
import git4idea.annotate.GitFileAnnotation
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import org.jetbrains.plugins.github.util.GithubUtil

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT);
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        val editor = e.getData(CommonDataKeys.EDITOR);
        if (virtualFile == null || project == null || project.isDisposed) {
            return;
        }

        val eventData = calcData(e)

        val foo = eventData?.repository?.remotes?.joinToString {
            it.pushUrls.toString() + "\n"
        }

        val vcs = eventData?.repository?.vcs as GitVcs?
        val annotate = vcs?.annotationProvider?.annotate(virtualFile) as GitFileAnnotation?
        val lineNumber = editor?.document?.getLineNumber(editor.selectionModel.selectionStart)

        if(lineNumber == null) {
            Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
                    "Plugin Importer+Exporter",
                    "EventData: $foo hash: ${annotate?.currentRevision} Annotate: $annotate",
                    NotificationType.INFORMATION))
        } else {
            lineNumber.plus(1)
            val revisionHash = annotate?.originalRevision(lineNumber)

            val revNum = annotate?.revisions?.indexOfFirst {
                it.revisionNumber == revisionHash
            }

            Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
                    "Plugin Importer+Exporter",
                    "hash: $revisionHash currentRev: ${annotate?.currentRevision} revNum: $revNum count: ${annotate?.revisions?.count()}",
                    NotificationType.INFORMATION))
        }
    }

    private fun calcData(e : AnActionEvent): EventData? {
        val project = e.getData(CommonDataKeys.PROJECT)
        project ?: return null

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        virtualFile ?: return null

        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: return null

        val repository = GithubUtil.getGitRepository(project, virtualFile)
        repository ?: return null

        return EventData(project, repository)
    }

    private data class EventData(val project: Project, val repository: GitRepository) {
    }

}
