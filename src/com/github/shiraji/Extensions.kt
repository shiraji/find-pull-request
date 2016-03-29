package com.github.shiraji

import git4idea.GitCommit
import java.math.BigInteger
import java.security.MessageDigest


fun String.toMd5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(this.toByteArray())).toString(16)

fun String.subtract(text: String) = this.replace(text, "")

fun GitCommit.getPullRequestNumber() = this.fullMessage.replace("Merge pull request #", "").split(" ").first().toInt()