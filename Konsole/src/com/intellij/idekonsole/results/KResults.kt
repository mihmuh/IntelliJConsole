package com.intellij.idekonsole.results

import com.intellij.idekonsole.scripting.project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JComponent

private val LOG = Logger.getInstance(KResult::class.java)

private fun createLabel(text: String): JBLabel {
    val content = StringUtil.escapeXml(text).replace("\n", "<br>").replace(" ", "&nbsp;")
    return JBLabel(content).setCopyable(true)
}

class KCommandResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val label = createLabel("> " + if (text.contains('\n')) (text + "\n") else text)
        panel = JBUI.Panels.simplePanel(label)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}

class KStdoutResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val label = createLabel(text)
        label.foreground = JBColor.RED

        panel = label
    }

    override fun getPresentation(): JComponent = panel
}

class KHelpResult(val text: String) : KResult {
    val panel: JComponent

    init {
        val label = createLabel(text)
        label.foreground = JBColor.GRAY

        panel = label
    }

    override fun getPresentation(): JComponent = panel
}

class KErrorResult(val error: String) : KResult {
    val panel: JComponent

    init {
        LOG.warn(error)

        val prefix = JBLabel("ERROR: ")

        val label = createLabel(error)
        label.foreground = JBColor.RED

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}

class KUsagesResult<T : PsiElement>(val elements: List<T?>, val searchQuery: String, r: ((T) -> Unit)? = null) : KResult {
    val panel: JComponent
    val refactoring: ((T) -> Unit)? = r
    lateinit var label: JBLabel
    lateinit var mouseAdapter: MouseListener

    init {
        var elementsString = "" + elements.size + " element"
        if (elements.size > 1) elementsString += "s"
        if (refactoring != null) {
            elementsString = "Refactor " + elementsString
        }
        label = JBLabel("<html><a>" + elementsString + "</a></html>")
        label.foreground = Color.BLUE;

        //todo one node should be shown as a ref
        mouseAdapter = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                openUsagesView()
            }
        }
        label.addMouseListener(mouseAdapter)
        panel = JBUI.Panels.simplePanel(label)
        panel.background = null
    }

    fun openUsagesView() {
        val project = project()
        if (project != null) {
            if (refactoring != null) {
                KUsagesPresentation(project).showUsages(elements.filterNotNull(), searchQuery, refactoring, Runnable {
                    label.removeMouseListener(mouseAdapter)
                    label.text = label.text.replace("Refactor", "Refactored")
                    label.foreground = Color.GRAY
                    label.font = Font(label.font.name, Font.ITALIC, label.font.size)
                })
            } else {
                KUsagesPresentation(project).showUsages(elements.filterNotNull(), searchQuery)

            }
        }
    }

    constructor (element: T?, searchQuery: String, refactoring: ((T) -> Unit)? = null) : this(listOf(element), searchQuery, refactoring)

    constructor (vararg element: T?, searchQuery: String, refactoring: ((T) -> Unit)? = null) : this(element.toList(), searchQuery, refactoring)

    override fun getPresentation(): JComponent = panel
}

class KExceptionResult(val t: Throwable) : KResult {
    val panel: JComponent
    val DARK_BLUE = Color(0, 0, 128)

    init {
        val prefix = JBLabel("Exception: ")
        val label = JBLabel(underlineAndHighlight(shortPackageName(t.javaClass.name), DARK_BLUE, Color.PINK))
        label.background = Color.PINK
        val writer: StringWriter = StringWriter();
        t.printStackTrace(PrintWriter(writer));
        val project = project()
        if (project != null) {
            label.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    val dialog: KAnalyzeStacktraceDialog = KAnalyzeStacktraceDialog(project, writer.toString())
                    dialog.show()
                }
            })
        }

        panel = JBUI.Panels.simplePanel(label).addToLeft(prefix)
        panel.background = null
    }

    fun underlineAndHighlight(s: String?, foreground: Color, background: Color): String {
        if (s == null) {
            return ""
        }
        val htmlForeground = colorToHtml(foreground)
        val htmlBackground = colorToHtml(background)

        return "<HTML><U style=\"color:$htmlForeground ;background-color:$htmlBackground\">$s</U></HTML>"
    }

    private fun colorToHtml(color: Color): String {
        val rgb = Integer.toHexString(color.rgb)
        return rgb.substring(2, rgb.length)
    }

    fun shortPackageName(fqName: String?): String? {
        if (fqName == null) {
            return fqName
        }
        var start = 0
        var dotIndex = fqName.indexOf('.', start)
        val stringBuilder = StringBuilder()
        while (dotIndex > 0) {
            stringBuilder.append(fqName[start])
            stringBuilder.append('.')
            start = dotIndex + 1
            dotIndex = fqName.indexOf('.', start)
        }
        stringBuilder.append(fqName.substring(start, fqName.length))
        return stringBuilder.toString()
    }

    override fun getPresentation(): JComponent = panel

}
