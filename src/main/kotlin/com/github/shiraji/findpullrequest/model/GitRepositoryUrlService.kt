package com.github.shiraji.findpullrequest.model

class GitRepositoryUrlService {

    /**
     * The expected remote url format is
     * * https://HOST/USER/REPO.git
     * * git@HOST:USER/REPO.git
     */
    private val pathRegex = Regex(".*[/:](.*).git")

    fun toURLPath(gitRemoteUrl: String): String {
        val withoutProtocol = removeProtocolPrefix(gitRemoteUrl).replace(':', '/')
        return withoutProtocol.removeSuffix(".git")
    }

    private fun removeProtocolPrefix(url: String): String {
        return if (url.contains("@")) url.substringAfter("@") else url.substringAfter("://")
    }
}