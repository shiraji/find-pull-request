package com.github.shiraji.findpullrequest.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import git4idea.annotate.GitFileAnnotation
import git4idea.changes.GitChangeUtils
import git4idea.history.GitHistoryUtils
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
        val lineNumber = editor?.document?.getLineNumber(editor.selectionModel.selectionStart) ?: return
        val revisionHash = annotate.originalRevision(lineNumber) ?: return
        val histories = GitHistoryUtils.history(project, repository.root, "$revisionHash..master", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse")
        val pullRequestCommit = histories.first()
        val mergedCommits = GitHistoryUtils.history(project, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}")

        if(mergedCommits.filter { it.id.asString() == revisionHash.asString() }.size == 0) {
            System.out.println("No hash!!! $revisionHash")
        }else {
            System.out.println("Found!!! $revisionHash")
        }

    }
}
