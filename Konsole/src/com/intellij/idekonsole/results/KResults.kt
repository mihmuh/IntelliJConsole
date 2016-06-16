package com.intellij.idekonsole.results

import com.intellij.idekonsole.KSettings
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.idekonsole.scripting.Refactoring
import com.intellij.idekonsole.scripting.collections.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.impl.UsageAdapter
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

class KCommandResult(text: String) : KResult {
    val panel: JComponent

    init {
        val label = createLabel("> " + if (text.contains('\n')) (text + "\n") else text)
        panel = JBUI.Panels.simplePanel(label)
        panel.background = null
    }

    override fun getPresentation(): JComponent = panel
}

class KStdoutResult(text: String) : KResult {
    val panel: JComponent

    init {
        val label = createLabel(text)
        label.foreground = JBColor.RED

        panel = label
    }

    override fun getPresentation(): JComponent = panel
}

class KHelpResult(text: String) : KResult {
    val panel: JComponent

    init {
        val label = createLabel(text)
        label.foreground = JBColor.GRAY

        panel = label
    }

    override fun getPresentation(): JComponent = panel
}

class KErrorResult(error: String) : KResult {
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

open class KUsagesResult<T : PsiElement>(val elements: SequenceLike<T>, elementsString: String, val searchQuery: String, val project: Project, val output: ConsoleOutput?, val refactoring: ((T) -> Unit)? = null) : KResult {
    val panel: JComponent
    val label: JBLabel
    val mouseAdapter: MouseListener

    init {
        label = JBLabel("<html><a>$elementsString</a></html>")
        label.foreground = Color.BLUE

        val wrappedOpenUsagesView = Context.wrapCallback({ openUsagesView() })
        mouseAdapter = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) = wrappedOpenUsagesView.invoke()
        }
        label.addMouseListener(mouseAdapter)
        panel = JBUI.Panels.simplePanel(label)
        panel.background = null
    }

    fun openUsagesView() {
        val usages = elements.map { if (it.isValid) UsageInfo2UsageAdapter(UsageInfo(it)) else UsageAdapter() }
        if (refactoring != null) {
            val dialogAnswer = MessagesEx.showYesNoDialog("Operating with so many results can take more time.\nDo you want to continue?", "Too Many Results", null)
            if (dialogAnswer != MessagesEx.YES) {
                return
            }
            val elementsList = elements.toList()
            val usagesList = usages.toList()
            KUsagesPresentation(project).showUsages(usagesList.asSequence(), searchQuery, Runnable {
                for (it in elementsList) {
                    try {
                        refactoring.invoke(it)
                    } catch (e: Exception) {
                        val failedIndex = elementsList.indexOf(it)
                        label.text = label.text.replace("" + elementsList.size + " element", "" + failedIndex + " element") + "successfully"
                        val exception = KExceptionResult(project, e)
                        output?.addResultAfter(exception, this)
                        val remaining = usagesResult(elementsList.subList(failedIndex + 1, elementsList.size).asSequence(), searchQuery, project, output, refactoring)
                        remaining.label.text = remaining.label.text.replace("Refactor", "Refactor remaining")
                        output?.addResultAfter(remaining, exception)
                        break
                    }
                }
                label.removeMouseListener(mouseAdapter)
                label.text = label.text.replace("Refactor", "Refactored")
                label.foreground = Color.GRAY
                label.font = Font(label.font.name, Font.ITALIC, label.font.size)
            })
        } else {
            KUsagesPresentation(project).showUsages(usages, searchQuery)
        }
    }

    override fun getPresentation(): JComponent = panel
}

fun <T: PsiElement> usagesResult(elements: SequenceLike<T>, searchQuery: String, project: Project, output: ConsoleOutput?, refactoring: ((T) -> Unit)? = null): KUsagesResult<T> {
    return KUsagesResult(elements, "Search query", searchQuery, project, output, refactoring)
}

fun <T: PsiElement> usagesResult(refactoring: Refactoring<T>, searchQuery: String, project: Project, output: ConsoleOutput?): KUsagesResult<T> {
    return usagesResult(refactoring.elements.asSequence(), searchQuery, project, output, refactoring.refactoring)
}

fun <T: PsiElement> usagesResult(elements: Sequence<T>, searchQuery: String, project: Project, output: ConsoleOutput?, refactoring: ((T) -> Unit)? = null): KUsagesResult<T> {
    val elementsEvaluated = elements.cacheHead(KSettings.TIME_LIMIT)
    var elementsString = "" + elementsEvaluated.evaluated.size + " element"
    if (elementsEvaluated.evaluated.size != 1) {
        elementsString += "s"
    }
    if (!elementsEvaluated.remaining.isEmpty()) {
        elementsString = "More than " + elementsString
    }
    if (refactoring != null) {
        elementsString = "Refactor " + elementsString
    }
    //todo one node should be shown as a ref
    return KUsagesResult(elements.asSequenceLike(), elementsString, searchQuery, project, output, refactoring)
}

class KExceptionResult(val project: Project, t: Throwable) : KResult {
    val panel: JComponent
    val DARK_BLUE = Color(0, 0, 128)

    init {
        val prefix = JBLabel("Exception: ")
        val classLabel = JBLabel(underlineAndHighlight(shortPackageName(t.javaClass.name), DARK_BLUE, Color.PINK))
        val messageLabel = JBLabel(": " + t.message.toString())
        messageLabel.background = Color.PINK
        prefix.background = Color.PINK
        classLabel.background = Color.PINK
        val writer: StringWriter = StringWriter()
        t.printStackTrace(PrintWriter(writer))
        classLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val dialog: KAnalyzeStacktraceDialog = KAnalyzeStacktraceDialog(project, writer.toString())
                dialog.show()
            }
        })

        panel = JBUI.Panels.simplePanel(messageLabel).addToLeft(JBUI.Panels.simplePanel(classLabel).addToLeft(prefix))
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
