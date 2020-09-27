package com.github.shiraji.findpullrequest.service

import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl

class UpToDateLineNumberProviderService(private val upToDateLineNumberProvider: UpToDateLineNumberProviderImpl) {
    fun getLineRevision(fileAnnotation: FileAnnotation, currentLineNumber: Int): VcsRevisionNumber? {
        val lineNumber = getLineNumber(currentLineNumber)
        return fileAnnotation.getLineRevisionNumber(lineNumber)
    }

    fun getLineNumber(currentLineNumber: Int): Int {
        return upToDateLineNumberProvider.getLineNumber(currentLineNumber)
    }
}