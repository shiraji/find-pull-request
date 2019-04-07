package com.github.shiraji.findpullrequest.model

import git4idea.repo.GitRepository

class GitRepositoryService {

    fun hasOriginOrUpstreamRepository(repository: GitRepository): Boolean {
        return findOriginUrl(repository) != null || findUpstreamUrl(repository) != null
    }

    fun findOriginUrl(repository: GitRepository): String? {
        return findRemoteUrl(repository, "origin")
    }

    fun findUpstreamUrl(repository: GitRepository): String? {
        return findRemoteUrl(repository, "upstream")
    }

    private fun findRemoteUrl(repository: GitRepository, targetRemoteName: String): String? {
        return repository.remotes.firstOrNull { it.name == targetRemoteName }?.firstUrl
    }

}