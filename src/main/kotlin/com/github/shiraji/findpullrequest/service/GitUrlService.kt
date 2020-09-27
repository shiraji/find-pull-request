package com.github.shiraji.findpullrequest.service

import git4idea.repo.GitRepository

class GitUrlService(private val repository: GitRepository) {

    fun hasOriginOrUpstreamRepository(): Boolean {
        return findOriginUrl() != null || findUpstreamUrl() != null
    }

    fun findOriginUrl(): String? {
        return findRemoteUrl("origin")
    }

    fun findUpstreamUrl(): String? {
        return findRemoteUrl("upstream")
    }

    private fun findRemoteUrl(targetRemoteName: String): String? {
        return repository.remotes.firstOrNull { it.name == targetRemoteName }?.firstUrl
    }

    /**
     * The expected remote url format is
     * * https://HOST/USER/REPO.git
     * * git@HOST:USER/REPO.git
     */
    fun toURLPath(gitRemoteUrl: String): String {
        val withoutProtocol = removeProtocolPrefix(gitRemoteUrl).replace(':', '/')
        return withoutProtocol.removeSuffix(".git")
    }

    private fun removeProtocolPrefix(url: String): String {
        return if (url.contains("@")) url.substringAfter("@") else url.substringAfter("://")
    }
}