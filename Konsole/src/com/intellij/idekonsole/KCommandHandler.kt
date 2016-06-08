package com.intellij.idekonsole

import com.intellij.idekonsole.results.KErrorResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
import com.intellij.scriptengine.kotlin.KotlinScriptEngine

object KCommandHandler {
    fun handle(text: String): KResult {
        try {
            return KStdoutResult(KotlinScriptEngine.evaluateScript(text))
        } catch(e: Exception) {
            return KErrorResult(e)
        }
    }
}
