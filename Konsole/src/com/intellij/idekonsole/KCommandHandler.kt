package com.intellij.idekonsole

import com.intellij.idekonsole.results.KErrorResult
import com.intellij.idekonsole.results.KExceptionResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
import com.intellij.openapi.project.Project
import com.intellij.scriptengine.kotlin.KotlinScriptEngine

object KCommandHandler {
    fun handle(text: String, project:Project): KResult {
        try {
            return KStdoutResult(KotlinScriptEngine.evaluateScript(text))
        } catch(e: Exception) {
            return KExceptionResult(e, project)
        }
    }
}
