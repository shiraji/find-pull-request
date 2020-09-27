package com.github.shiraji.findpullrequest.service

import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository

class GitRepositoryService(private val gitRepository: GitRepository) {
    fun getFileAnnotation(virtualFile: VirtualFile): FileAnnotation {
        return gitRepository.vcs.annotationProvider.annotate(virtualFile)
    }
}