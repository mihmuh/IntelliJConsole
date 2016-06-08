package com.intellij.idekonsole.results

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class KErrorResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val prefix = JBLabel("ERROR: ")
        val label = JBLabel(text).setCopyable(true)
        label.foreground = JBColor.RED

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}
