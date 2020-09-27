package com.github.shiraji.findpullrequest.infra.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.TimedVcsCommit
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitCommit
import git4idea.GitRevisionNumber
import git4idea.commands.GitBinaryHandler
import git4idea.commands.GitCommand
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import java.nio.charset.StandardCharsets

class GitHistory(private val project: Project, private val gitRepository: GitRepository) {

    fun findAncestryPathCommits(revisionHash: VcsRevisionNumber): MutableList<out TimedVcsCommit> {
        return GitHistoryUtils.collectTimedCommits(
            project,
            gitRepository.root,
            "$revisionHash..HEAD",
            "--merges",
            "--ancestry-path"
        ) ?: mutableListOf()
    }

    fun findFirstParentsCommits(revisionHash: VcsRevisionNumber): MutableList<out TimedVcsCommit> {
        return GitHistoryUtils.collectTimedCommits(
            project,
            gitRepository.root,
            "$revisionHash..HEAD",
            "--merges",
            "--first-parent"
        ) ?: mutableListOf()
    }

    fun findLatestMergeCommit(project: Project, root: VirtualFile): TimedVcsCommit? {
        return GitHistoryUtils.collectTimedCommits(project, root, "--merges", "-1").firstOrNull()
    }

    private fun toGitCommit(hash: String): GitCommit? {
        return GitHistoryUtils.history(project, gitRepository.root, hash, "-1").firstOrNull()
    }

    fun toGitCommit(revisionHash: VcsRevisionNumber): GitCommit? {
        return toGitCommit(revisionHash.asString())
    }

    fun toGitCommit(commit: TimedVcsCommit): GitCommit? {
        return toGitCommit(commit.id.asString())
    }

    fun listCommitsFromMergedCommit(mergeCommit: GitCommit): List<GitCommit> {
        return GitHistoryUtils.history(project, gitRepository.root, "${mergeCommit.id}^..${mergeCommit.id}")
    }

    @Throws(VcsException::class)
    fun toVcsRevisionNumber(hash: String): VcsRevisionNumber? {
        return GitRevisionNumber.resolve(project, gitRepository.root, hash)
    }

    @Throws(VcsException::class)
    fun findRevisionHashes(virtualFile: VirtualFile): List<String> {
        val filePath = VcsUtil.getLastCommitPath(project, VcsUtil.getFilePath(virtualFile))
        val handler = GitBinaryHandler(project, gitRepository.root, GitCommand.BLAME)
        handler.setStdoutSuppressed(true)
        handler.addParameters("-l", "-t", "-c", "--encoding=UTF-8")
        handler.endOptions()
        handler.addRelativePaths(filePath)
        val output = String(handler.run(), StandardCharsets.UTF_8)
        return output.split("\n").map { it.split(Regex("\\s"))[0] }
    }
}