package com.github.shiraji

import com.intellij.openapi.editor.Editor
import java.math.BigInteger
import java.security.MessageDigest


fun String.toMd5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(this.toByteArray())).toString(16)

fun String.toSHA1() = BigInteger(1, MessageDigest.getInstance("SHA-1").digest(this.toByteArray())).toString(16)

fun String.subtract(text: String) = this.replace(text, "")

fun Editor.isPointSingleLine() = getLine(selectionModel.selectionStart) == getLine(selectionModel.selectionEnd)

fun Editor.getLine(offset: Int) = document.getLineNumber(offset)