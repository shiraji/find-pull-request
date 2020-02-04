package com.github.shiraji.findpullrequest.ext

import com.github.shiraji.findpullrequest.helper.root
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

fun getGitRepository(project: Project, file: VirtualFile?, manager: GitRepositoryManager): GitRepository? {
    val targetFile = file ?: project.root ?: return null
    @Suppress("DEPRECATION") // will remove deprecated annotation in 2020
    return manager.getRepositoryForFileQuick(targetFile)
}