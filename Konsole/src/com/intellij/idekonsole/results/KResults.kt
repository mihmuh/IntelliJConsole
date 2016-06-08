package com.intellij.idekonsole.results

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

private val LOG = Logger.getInstance(KResult::class.java)

class KCommandResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val label = JBLabel(text).setCopyable(true)
        label.foreground = JBColor.GREEN

        panel = label
    }

    override fun getPresentation(): JComponent = panel
}

class KStdoutResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val prefix = JBLabel("> ")
        val label = JBLabel(text).setCopyable(true)
        label.foreground = JBColor.BLACK

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}

class KErrorResult(val err: Exception) : KResult {
    val panel: JComponent

    init {
        LOG.warn(err)

        val prefix = JBLabel("ERROR: ")

        val message = err.message.toString()
        val classname = err.javaClass.name

        val label = JBLabel("[$classname] $message").setCopyable(true)
        label.foreground = JBColor.RED

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}
