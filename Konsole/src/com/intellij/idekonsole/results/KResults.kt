package com.intellij.idekonsole.results

import com.intellij.idekonsole.scripting.project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JComponent
import javax.swing.JOptionPane

private val LOG = Logger.getInstance(KResult::class.java)

class KCommandResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val label = JBLabel(text).setCopyable(true)
        label.foreground = JBColor.GREEN.darker()

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

class KErrorResult(val error: String) : KResult {
    val panel: JComponent

    init {
        LOG.warn(error)

        val prefix = JBLabel("ERROR: ")

        val label = JBLabel(error).setCopyable(true)
        label.foreground = JBColor.RED

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}

class KPsiElementsResult(val elements: List<PsiElement?>) : KResult {
    val panel: JComponent


    init {
        val prefix = JBLabel("")
        val label = JBLabel("Show Usages")
        label.foreground = Color.BLUE;
        val project = project()
        //todo one node should be shown as a ref
        if (project != null) {
            label.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    UsagesPresentation(project).showUsages(elements.filterNotNull())
                }
            })
        }
        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
    }

    constructor (element: PsiElement?):this(listOf(element))
    constructor (vararg element: PsiElement?):this(element.toList())

    override fun getPresentation(): JComponent = panel
}

class KExceptionResult(val t: Throwable) : KResult {
    val panel: JComponent

    init {
        val prefix = JBLabel("Exception: ")
        val label = JBLabel(t.javaClass.name)
        label.background = Color.PINK
        label.foreground = Color.BLUE
        val writer: StringWriter = StringWriter();
        t.printStackTrace(PrintWriter(writer));
        val project = project()
        if (project != null) {
            label.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    val dialog: MyAnalyzeStacktraceDialog = MyAnalyzeStacktraceDialog(project, writer.toString())
                    dialog.show()
                }
            })
        }

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
    }

    override fun getPresentation(): JComponent = panel

}
