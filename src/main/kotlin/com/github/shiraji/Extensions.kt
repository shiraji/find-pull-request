package com.github.shiraji

import git4idea.GitCommit
import java.math.BigInteger
import java.security.MessageDigest


fun String.toMd5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(this.toByteArray())).toString(16)

fun String.toSHA1() = BigInteger(1, MessageDigest.getInstance("SHA-1").digest(this.toByteArray())).toString(16)

fun String.subtract(text: String) = this.replace(text, "")