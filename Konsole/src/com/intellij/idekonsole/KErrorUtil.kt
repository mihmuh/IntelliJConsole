package com.intellij.idekonsole

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project

object KErrorUtil {
    fun containsError(project: Project, editor: EditorEx): Boolean {
        var errorFound = false
        DaemonCodeAnalyzerEx.processHighlights(editor.document, project, HighlightSeverity.ERROR, 0, editor.document.textLength, { info ->
            errorFound = true
            return@processHighlights false
        })
        return errorFound
    }
}