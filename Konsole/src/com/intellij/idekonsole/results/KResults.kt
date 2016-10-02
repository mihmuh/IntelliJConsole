package com.intellij.idekonsole.results

import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.idekonsole.scripting.Refactoring
import com.intellij.idekonsole.scripting.collections.SequenceLike
import com.intellij.idekonsole.scripting.collections.asSequenceLike
import com.intellij.idekonsole.scripting.collections.map
import com.intellij.idekonsole.scripting.collections.toList
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.impl.UsageAdapter
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
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

class KUsagesResult<T : PsiElement>(val elements: SequenceLike<T>, val searchQuery: String, val project: Project, val output: ConsoleOutput?, val refactoring: ((T) -> Unit)? = null): KResult {
    val usagesLabel = InteractiveLabel(Context.wrapCallback({
        openUsagesView()
    }))
    val statusLabel = JBLabel()
    val panel = JBUI.Panels.simplePanel(usagesLabel.myLabel).addToLeft(statusLabel)

    val usagesList = ArrayList<T>()
    @Volatile var myStopped = false
    var myFinished = false
    @Volatile var myUsageViewListener: UsagesViewUsagesListener? = null

    init {
        panel.background = null

        val collectorListener = object: UsagesListener<T> {
            override fun processFirstUsage(usage: T) {
                if (myUsageViewListener != null) {
                    myUsageViewListener!!.processFirstUsage(wrapUsage(usage))
                }
                ApplicationManager.getApplication().invokeLater {
                    usagesList.add(usage)
                    usagesLabel.text = "1 element found."
                }
            }
            override fun processOthers(usage: T) {
                if (myUsageViewListener != null) {
                    myUsageViewListener!!.processOthers(wrapUsage(usage))
                }
                ApplicationManager.getApplication().invokeLater {
                    usagesList.add(usage)
                    usagesLabel.text = "${usagesList.size} elements found."
                }
            }
            override fun finished() {
                myStopped = true
                ApplicationManager.getApplication().invokeLater {
                    //todo one node should be shown as a ref
                    statusLabel.text = "Finished."
                    myFinished = true
                    if (refactoring != null) {
                        usagesLabel.text = "Refactor ${usagesList.size} elements."
                        showFromList()
                    }
                }
            }
            override fun empty() {
                myStopped = true
                ApplicationManager.getApplication().invokeLater {
                    usagesLabel.text = "Nothing found."
                    usagesLabel.deactivate()
                }
            }
            override fun askTooManyUsagesContinue(): Boolean {
                if (myUsageViewListener == null) {
                    myStopped = true
                    return false
                } else {
                    return myUsageViewListener!!.askTooManyUsagesContinue()
                }
            }
            override fun cancelled() {
                myStopped = true
                ApplicationManager.getApplication().invokeLater {
                    statusLabel.text = "Cancelled."
                }
            }
        }

        collectorListener.showUsages(project, elements)
    }

    private fun refactor(r: (T) -> Unit) {
        val elementsList = elements.toList()
        for (it in elementsList) {
            try {
                r.invoke(it)
            } catch (e: Exception) {
                val failedIndex = elementsList.indexOf(it)
                usagesLabel.text = usagesLabel.text.replace("" + elementsList.size + " element", "" + failedIndex + " element") + "successfully"
                val exception = KExceptionResult(project, e)
                output?.addResultAfter(exception, this)
                val remaining = usagesResult(elementsList.subList(failedIndex + 1, elementsList.size).asSequence(), searchQuery, project, output, refactoring)
                remaining.usagesLabel.text = remaining.usagesLabel.text.replace("Refactor", "Refactor remaining")
                output?.addResultAfter(remaining, exception)
                break
            }
        }
        usagesLabel.deactivate()
        usagesLabel.text = usagesLabel.text.replace("Refactor", "Refactored")
    }

    private fun openUsagesView() {
        if (!myStopped && myUsageViewListener == null) {
            myUsageViewListener = createUsageViewListener()
            initListenerFromList()
        } else if (myFinished) {
            showFromList()
        } else {
            createUsageViewListener().showUsages(project, elements.map { wrapUsage(it) })
        }
    }

    fun initListenerFromList() {
        if (usagesList.isNotEmpty()) {
            myUsageViewListener!!.processFirstUsage(wrapUsage(usagesList.first()))
        }
        usagesList.asSequence().drop(1).forEach {
            myUsageViewListener!!.processOthers(wrapUsage(it))
        }
    }

    fun wrapUsage(usage: T): Usage {
        return if (usage.isValid) UsageInfo2UsageAdapter(UsageInfo(usage)) else UsageAdapter()
    }

    private fun showFromList() {
        myUsageViewListener = createUsageViewListener()
        initListenerFromList()
        myUsageViewListener!!.finished()
    }

    fun createUsageViewListener(): UsagesViewUsagesListener {
        return UsagesViewUsagesListener(project, searchQuery, if (refactoring != null) {
            Runnable { refactor(refactoring) }
        } else {
            null
        })
    }

    override fun getPresentation() = panel
}

class InteractiveLabel(private val action: () -> Unit) {
    val myLabel = JBLabel()
    val mouseListener: MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = action.invoke()
    }
    var text: String
        get() = myLabel.text.substringAfter("<a>").substringBeforeLast("</a>")
        set(value) {
            myLabel.text = "<html><a>$value</a></html>"
        }

    init {
        myLabel.foreground = Color.BLUE
        myLabel.addMouseListener(mouseListener)
    }

    fun deactivate() {
        myLabel.removeMouseListener(mouseListener)
        myLabel.foreground = Color.GRAY
        myLabel.font = Font(myLabel.font.name, Font.ITALIC, myLabel.font.size)
    }
}

fun <T : PsiElement> usagesResult(elements: SequenceLike<T>, searchQuery: String, project: Project, output: ConsoleOutput?, refactoring: ((T) -> Unit)? = null): KUsagesResult<T> {
    return KUsagesResult(elements, searchQuery, project, output, refactoring)
}

fun <T : PsiElement> usagesResult(refactoring: Refactoring<T>, searchQuery: String, project: Project, output: ConsoleOutput?): KUsagesResult<T> {
    return usagesResult(refactoring.elements, searchQuery, project, output, refactoring.refactoring)
}

fun <T : PsiElement> usagesResult(elements: Sequence<T>, searchQuery: String, project: Project, output: ConsoleOutput?, refactoring: ((T) -> Unit)? = null): KUsagesResult<T> {
    return KUsagesResult(elements.asSequenceLike(), searchQuery, project, output, refactoring)
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
