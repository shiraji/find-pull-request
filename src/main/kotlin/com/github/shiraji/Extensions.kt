package com.github.shiraji

import git4idea.GitCommit
import java.math.BigInteger
import java.security.MessageDigest


fun String.toMd5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(this.toByteArray())).toString(16)

fun String.subtract(text: String) = this.replace(text, "")

fun GitCommit.getPullRequestNumber() = "Merge pull request #(\\d*)".toRegex().find(this.fullMessage)!!.groups[1]!!.value.toInt()

fun GitCommit.getPullRequestNumberFromSquashCommit() = ".*\\(#(\\d*)\\)".toRegex().find(this.fullMessage)!!.groups[1]!!.value.toInt()

fun GitCommit.isSquashPullRequestCommit() = ".*\\(#(\\d*)\\)".toRegex().containsMatchIn(this.fullMessage)