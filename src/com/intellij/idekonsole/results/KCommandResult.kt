package com.intellij.idekonsole.results

import com.intellij.ui.components.JBLabel
import javax.swing.JComponent


class KCommandResult(val text: String) : KResult {
    val panel: JComponent

    init {
        panel = JBLabel("> " + text).setCopyable(true)
    }

    override fun getPresentation(): JComponent = panel
}
