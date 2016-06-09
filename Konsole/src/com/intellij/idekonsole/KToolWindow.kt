package com.intellij.idekonsole

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import javax.swing.JPanel

class KToolWindow(val project: Project) : JPanel(), Disposable {
    val LOG = Logger.getInstance(KToolWindow::class.java)

    init {
        layout = BorderLayout()

        val actionGroup = ActionManager.getInstance().getAction("KToolWindow.Toolbar") as ActionGroup
        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        add(toolbar.component, BorderLayout.NORTH)

        try {
            val editor = KEditor(project)

            DataManager.registerDataProvider(this, object : DataProvider {
                override fun getData(key: String?): Any? {
                    if (KDataKeys.K_TOOL_WINDOW.`is`(key)) {
                        return this@KToolWindow
                    }
                    if (KDataKeys.K_EDITOR.`is`(key)) {
                        return editor
                    }
                    return null
                }
            })

            Disposer.register(this, editor)

            add(editor.splitter, BorderLayout.CENTER)
        } catch(e: Exception) {
            LOG.error(e)
        }
    }

    override fun dispose() {
    }
}
