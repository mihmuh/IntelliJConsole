package com.intellij.idekonsole

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import javax.swing.JPanel

class KToolWindow(val project: Project) : JPanel(), Disposable {
    private val toolbar: ActionToolbar
    private val editor: KEditor

    init {
        layout = BorderLayout()

        val actionGroup = ActionManager.getInstance().getAction("KToolWindow.Toolbar") as ActionGroup
        toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)

        editor = KEditor(project)

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

        add(toolbar.component, BorderLayout.NORTH)
        add(editor.component, BorderLayout.CENTER)
    }

    override fun dispose() {
    }
}
