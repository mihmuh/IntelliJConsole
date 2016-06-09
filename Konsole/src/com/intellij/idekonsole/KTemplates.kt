package com.intellij.idekonsole

object KTemplates {
    private val CARET = "{CARET}"
    private val FOLDING_START = "{FOLDING_START}"
    private val FOLDING_END = "{FOLDING_END}"

    private val CONSOLE_CONTENT =
            """
            package com.intellij.idekonsole.runtime;

            import com.intellij.idekonsole.results.*
            import com.intellij.idekonsole.runtime.*

            ${FOLDING_START}fun main_exec(): KResult {
                ${CARET}return KStdoutResult("Hello World")
            }${FOLDING_END}

            """.trimIndent()


    fun getConsoleContent() = CONSOLE_CONTENT.replace(CARET, "").replace(FOLDING_START, "").replace(FOLDING_END, "")
    fun getConsoleCaretOffset() = CONSOLE_CONTENT.replace(FOLDING_START, "").replace(FOLDING_END, "").indexOf(CARET)
    fun getConsoleFoldingStart() = CONSOLE_CONTENT.replace(CARET, "").replace(FOLDING_END, "").indexOf(FOLDING_START)
    fun getConsoleFoldingEnd() = CONSOLE_CONTENT.replace(CARET, "").replace(FOLDING_START, "").indexOf(FOLDING_END)

}