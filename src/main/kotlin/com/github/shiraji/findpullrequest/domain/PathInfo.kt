package com.github.shiraji.findpullrequest.domain

import com.intellij.openapi.vcs.history.VcsRevisionNumber

sealed class PathInfo(open val hostingService: HostingService) {
    abstract fun createPath(): String

    data class Pr(val prNumber: Int, override val hostingService: HostingService) : PathInfo(hostingService) {
        override fun createPath(): String {
            return hostingService.urlPathFormat.format(prNumber)
        }
    }

    data class Commit(val revisionNumber: VcsRevisionNumber, override val hostingService: HostingService) :
        PathInfo(hostingService) {
        override fun createPath(): String {
            return hostingService.commitPathFormat.format(revisionNumber.asString())
        }
    }
}