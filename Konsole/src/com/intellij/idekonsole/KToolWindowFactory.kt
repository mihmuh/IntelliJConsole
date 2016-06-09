package com.intellij.idekonsole

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class KToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        val ID = "Konsole"
    }

    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        val panel = KToolWindow(project)
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, "Main", false)
        content.isCloseable = false
        window.contentManager.addContent(content)
        window.contentManager.requestFocus(content, false)
    }
}