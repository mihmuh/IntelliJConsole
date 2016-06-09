package com.intellij.idekonsole.results

import com.intellij.ide.IdeBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.unscramble.AnalyzeStacktraceDialog
import com.intellij.unscramble.AnalyzeStacktraceUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author simon
 */
class MyAnalyzeStacktraceDialog : DialogWrapper {
    val project: Project
    val text : String
    lateinit var myEditorPanel: AnalyzeStacktraceUtil.StacktraceEditorPanel

    constructor(project: Project, text:String) : super(project, true) {
        this.project = project
        this.text = text
        this.title = IdeBundle.message("unscramble.dialog.title", *arrayOfNulls<Any>(0))
        this.init()
    }

    override fun createCenterPanel(): JComponent? {
        val var1 = JPanel(BorderLayout())
        var1.add(JLabel("Put a stacktrace here:"), "North")
        this.myEditorPanel = AnalyzeStacktraceUtil.createEditorPanel(this.project, this.myDisposable)
        this.myEditorPanel.text = text
        var1.add(this.myEditorPanel, "Center")
        return var1
    }

    override fun doOKAction() {
        AnalyzeStacktraceUtil.addConsole(this.project, null, "<Stacktrace>", this.myEditorPanel.text)
        super.doOKAction()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return this.myEditorPanel.editorComponent
    }
}