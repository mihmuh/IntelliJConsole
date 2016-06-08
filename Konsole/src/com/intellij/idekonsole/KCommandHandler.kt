package com.intellij.idekonsole

import com.intellij.idekonsole.results.KErrorResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
import com.intellij.scriptengine.kotlin.KotlinScriptEngine
import javax.script.ScriptEngineManager

object KCommandHandler {
    fun handle(text: String): KResult {
        // FIXME

        if (text.startsWith("sout: ")) {
            return KStdoutResult(text.removePrefix("sout: "))
        }
        if (text.startsWith("error: ")) {
            return KErrorResult(text.removePrefix("error: "))
        }
        val evaluated = KotlinScriptEngine.evaluateScript(text);
        return KStdoutResult(evaluated)
    }
}
