package com.intellij.idekonsole

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class KToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        val ID = "Konsole"

        fun createWindow(project: Project, window: ToolWindow, closeable: Boolean) {
            val panel = KToolWindow(project)
            val content = ContentFactory.SERVICE.getInstance().createContent(panel, "Main", false)
            content.isCloseable = closeable
            window.contentManager.addContent(content)
            window.contentManager.requestFocus(content, false)
        }
    }

    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        createWindow(project, window, false)
    }
}