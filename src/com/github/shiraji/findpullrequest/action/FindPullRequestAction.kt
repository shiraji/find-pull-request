package com.github.shiraji.findpullrequest.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
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
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import org.jetbrains.plugins.github.util.GithubUtil

class FindPullRequestAction : AnAction() {

//    override fun actionPerformed(e: AnActionEvent) {
//        val eventData = calcData(e)
//
//        val foo = eventData?.repository?.remotes?.joinToString {
//            it.pushUrls.toString() + "\n"
//        }
//
//        Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
//                "Plugin Importer+Exporter",
//                "EventData: " + foo,
//                NotificationType.INFORMATION))
//    }
//
//    private fun calcData(e : AnActionEvent): EventData? {
//        val project = e.getData(CommonDataKeys.PROJECT)
//        project ?: return null
//
//        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
//        virtualFile ?: return null
//
//        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
//        document ?: return null
//
//        val repository = GithubUtil.getGitRepository(project, virtualFile)
//        repository ?: return null
//
//        return EventData(project, repository)
//    }
//
//    private data class EventData(val project: Project, val repository: GitRepository) {
//    }

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        val project = e.getData(CommonDataKeys.PROJECT);
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        val editor = e.getData(CommonDataKeys.EDITOR);
        if (virtualFile == null || project == null || project.isDisposed) {
            return;
        }

        val foo = getCurrentFileRevisionHash(project, virtualFile)
        Notifications.Bus.notify(Notification("Plugin Importer+Exporter",
                "Plugin Importer+Exporter",
                "EventData: " + foo,
                NotificationType.INFORMATION))
    }

//    @Nullable
//    private static String getGithubUrl(@NotNull Project project, @NotNull VirtualFile virtualFile, @Nullable Editor editor) {
//        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
//        final GitRepository repository = manager.getRepositoryForFile(virtualFile);
//        if (repository == null) {
//            StringBuilder details = new StringBuilder("file: " + virtualFile.getPresentableUrl() + "; Git repositories: ");
//            for (GitRepository repo : manager.getRepositories()) {
//                details.append(repo.getPresentableUrl()).append("; ");
//            }
//            GithubNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't find git repository", details.toString());
//            return null;
//        }
//
//        final String githubRemoteUrl = GithubUtil.findGithubRemoteUrl(repository);
//        if (githubRemoteUrl == null) {
//            GithubNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't find github remote");
//            return null;
//        }
//
//        String relativePath = VfsUtilCore.getRelativePath(virtualFile, repository.getRoot());
//        if (relativePath == null) {
//            GithubNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "File is not under repository root",
//                    "Root: " + repository.getRoot().getPresentableUrl() + ", file: " + virtualFile.getPresentableUrl());
//            return null;
//        }
//
//        String hash = getCurrentFileRevisionHash(project, virtualFile);
//        if (hash != null) {
//            return makeUrlToOpen(editor, relativePath, hash, githubRemoteUrl);
//        }
//
//        GithubNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't get last revision.");
//        return null;
//    }
//
//    @Nullable
//    private static String makeUrlToOpen(@Nullable Editor editor,
//    @NotNull String relativePath,
//    @NotNull String branch,
//    @NotNull String githubRemoteUrl) {
//        final StringBuilder builder = new StringBuilder();
//        final String githubRepoUrl = GithubUrlUtil.makeGithubRepoUrlFromRemoteUrl(githubRemoteUrl);
//        if (githubRepoUrl == null) {
//            return null;
//        }
//        if (StringUtil.isEmptyOrSpaces(relativePath)) {
//            builder.append(githubRepoUrl).append("/tree/").append(branch);
//        }
//        else {
//            builder.append(githubRepoUrl).append("/blob/").append(branch).append('/').append(relativePath);
//        }
//
//        if (editor != null && editor.getDocument().getLineCount() >= 1) {
//            // lines are counted internally from 0, but from 1 on github
//            SelectionModel selectionModel = editor.getSelectionModel();
//            final int begin = editor.getDocument().getLineNumber(selectionModel.getSelectionStart()) + 1;
//            final int selectionEnd = selectionModel.getSelectionEnd();
//            int end = editor.getDocument().getLineNumber(selectionEnd) + 1;
//            if (editor.getDocument().getLineStartOffset(end - 1) == selectionEnd) {
//                end -= 1;
//            }
//            builder.append("#L").append(begin).append("-L").append(end);
//        }
//
//        return builder.toString();
//    }

    fun getCurrentFileRevisionHash(project: Project, virtualFile: VirtualFile): String? {
        val ref = Ref<GitRevisionNumber>()
        ProgressManager.getInstance().run(object : Task.Modal(project, "Getting last revision", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    ref.set(GitHistoryUtils.getCurrentRevision(project, VcsUtil.getFilePath(virtualFile), "HEAD") as GitRevisionNumber?)
                } catch (e: VcsException) {
                }
            }

            override fun onCancel() {
                throw ProcessCanceledException()
            }
        })

        if (ref.isNull) return null
        return ref.get().rev
    }
}
