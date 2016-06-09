package com.intellij.idekonsole

import com.intellij.ide.DataManager
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
        toolbar.setTargetComponent(this)
        add(toolbar.component, BorderLayout.NORTH)

        for (action in actionGroup.getChildren(null)) {
            action.registerCustomShortcutSet(action.shortcutSet, this)
        }

        try {
            val editor = KEditor(project)
            Disposer.register(this, editor)

            DataManager.registerDataProvider(this, { key ->
                if (KDataKeys.K_TOOL_WINDOW.`is`(key)) {
                    this@KToolWindow
                } else if (KDataKeys.K_EDITOR.`is`(key)) {
                    editor
                } else {
                    null
                }
            })

            add(editor.getComponent(), BorderLayout.CENTER)
        } catch(e: Exception) {
            LOG.error(e)
        }
    }

    override fun dispose() {
    }
}
