package com.intellij.idekonsole

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import javax.swing.JPanel

class KToolWindow(val project: Project) : JPanel(), Disposable {
    companion object {
        val LOG = Logger.getInstance(KToolWindow::class.java)
    }

    init {
        layout = BorderLayout()

        val actionGroup = ActionManager.getInstance().getAction("KToolWindow.Toolbar") as ActionGroup
        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        add(toolbar.component, BorderLayout.NORTH)

        try {
            val editor = KEditor(project)
            Disposer.register(this, editor)

            add(editor.getComponent(), BorderLayout.CENTER)
        } catch(e: Exception) {
            LOG.error(e)
        }
    }

    override fun dispose() {
    }
}
