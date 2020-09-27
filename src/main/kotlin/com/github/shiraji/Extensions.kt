package com.github.shiraji

import com.github.shiraji.findpullrequest.domain.HostingService
import com.intellij.openapi.editor.Editor
import git4idea.GitCommit
import java.math.BigInteger
import java.security.MessageDigest

fun String.toMd5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(this.toByteArray())).toString(16)

fun String.toSHA1() = BigInteger(1, MessageDigest.getInstance("SHA-1").digest(this.toByteArray())).toString(16)

fun String.subtract(text: String) = this.replace(text, "")

fun Editor.isPointSingleLine() = getLine(selectionModel.selectionStart) == getLine(selectionModel.selectionEnd)

fun Editor.getLine(offset: Int) = document.getLineNumber(offset)

fun GitCommit.isSquashPullRequestCommit(hostingServices: HostingService): Boolean {
    return hostingServices.squashCommitMessage.containsMatchIn(this.fullMessage)
}

fun GitCommit.getNumberFromCommitMessage(commitMessageTemplate: Regex): Int? {
    return commitMessageTemplate.find(this.fullMessage)?.groups?.get(1)?.value?.toInt()
}