package com.github.shiraji.findpullrequest.model

class GitRepositoryUrlService {

    /**
     * The expected remote url format is
     * * https://HOST/USER/REPO.git
     * * git@HOST:USER/REPO.git
     */
    private val userRegex = Regex(".*[/:](.*)/.*.git")
    private val repoRegex = Regex(".*[/:].*/(.*).git")

    fun getUserFromRemoteUrl(gitRemoteUrl: String): String? {
        val (username) = userRegex.find(gitRemoteUrl)?.destructured ?: return null
        return username
    }

    fun getRepositoryFromRemoteUrl(gitRemoteUrl: String): String? {
        val (repository) = repoRegex.find(gitRemoteUrl)?.destructured ?: return null
        return repository
    }

    fun getHostFromUrl(url: String): String {
        val path = removeProtocolPrefix(url).replace(':', '/')
        return path.substringBefore("/")
    }

    private fun removeProtocolPrefix(url: String): String {
        return if (url.contains("@")) url.substringAfter("@") else url.substringAfter("://")
    }
}