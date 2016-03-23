package com.github.shiraji.findpullrequest.action

import com.github.shiraji.findpullrequest.model.FindPullRequestModel
import com.github.shiraji.getPullRequestNumber
import com.github.shiraji.subtract
import com.github.shiraji.toMd5
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import git4idea.GitFileRevision
import git4idea.history.GitHistoryUtils
import org.jetbrains.plugins.github.util.GithubUrlUtil
import org.jetbrains.plugins.github.util.GithubUtil

class FindPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val model = FindPullRequestModel(e)
        if (!model.isValid()) return

        val project = e.getData(CommonDataKeys.PROJECT);
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        val editor = e.getData(CommonDataKeys.EDITOR);
        if (virtualFile == null || project == null || project.isDisposed || editor == null) {
            return;
        }

        val repository = GithubUtil.getGitRepository(project, virtualFile) ?: return
        val annotate = repository.vcs?.annotationProvider?.annotate(virtualFile) ?: return
        val lineNumber = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        val revisionHash = annotate.originalRevision(lineNumber) ?: return
        val histories = GitHistoryUtils.history(project, repository.root, "$revisionHash..master", "--grep=Merge pull request", "--merges", "--ancestry-path", "--reverse")
        val pullRequestCommit = histories.first()
        val mergedCommits = GitHistoryUtils.history(project, repository.root, "${pullRequestCommit.id}^..${pullRequestCommit.id}")
        val pullRequestUrl = GithubUtil.findUpstreamRemote(repository) ?: GithubUtil.findGithubRemoteUrl(repository) ?: return
        val revision = annotate.revisions?.single { it.revisionNumber == revisionHash } as GitFileRevision
        val md5 = revision.path.path.subtract("${repository.gitDir.parent.presentableUrl.toString()}/").toMd5()
        var url: String?
        if(mergedCommits.filter { it.id.asString() == revisionHash.asString() }.size == 0) {
            val originUrl = repository.remotes.singleOrNull { it.name == "origin" }?.firstUrl ?: return
            val githubUrl = GithubUrlUtil.makeGithubRepoUrlFromRemoteUrl(originUrl, "https://" + GithubUrlUtil.getHostFromUrl(originUrl))
            url = "$githubUrl/commit/$revisionHash#diff-$md5"
        }else {
            val pullRequestNumber = pullRequestCommit.getPullRequestNumber()
            val githubUrl = GithubUrlUtil.makeGithubRepoUrlFromRemoteUrl(pullRequestUrl, "https://" + GithubUrlUtil.getHostFromUrl(pullRequestUrl))
            url = "$githubUrl/pull/$pullRequestNumber/files#diff-$md5"
        }
        BrowserUtil.open(url)
    }
}
