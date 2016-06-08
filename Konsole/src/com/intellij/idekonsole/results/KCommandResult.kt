package com.intellij.idekonsole.results

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import javax.swing.JComponent

class KCommandResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val label = JBLabel(text).setCopyable(true)
        label.foreground = JBColor.GREEN

        panel = label
    }

    override fun getPresentation(): JComponent = panel
}
