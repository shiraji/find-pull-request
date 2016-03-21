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
        val repository = GithubUtil.getGitRepository(project, virtualFile) ?: return
        val annotate = repository.vcs?.annotationProvider?.annotate(virtualFile) as GitFileAnnotation? ?: return
        val lineNumber = editor?.document?.getLineNumber(editor.selectionModel.selectionStart)?.plus(1) ?: return
        val revisionHash = annotate.originalRevision(lineNumber)
        val revisions = annotate.revisions ?: return

        val pullRequestRev = revisions.subList(0, revisions.indexOfFirst { it.revisionNumber == revisionHash }).findLast {
            val commitMessage = it.commitMessage
            commitMessage != null && commitMessage.indexOf("Merge pull request #") > 0
        }

        Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
                "Plugin Importer+Exporter",
                "hash: $revisionHash currentRev: ${annotate?.currentRevision} pullRequestRev: $pullRequestRev",
                NotificationType.INFORMATION))
    }
}
