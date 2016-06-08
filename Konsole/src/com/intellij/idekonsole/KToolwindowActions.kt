package com.intellij.idekonsole

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ContentsUtil

class KNewTabAction : DumbAwareAction("New Konsole Tab", null, AllIcons.General.Add) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolwindow = getToolwindow(project) ?: return
        KToolWindowFactory.createWindow(project, toolwindow, true)
    }
}

class KCloseTabAction : DumbAwareAction("Close Konsole Tab", null, AllIcons.Actions.Delete) {
    override fun actionPerformed(e: AnActionEvent) {
        val toolwindow = getToolwindow(e.project) ?: return
        val contentManager = toolwindow.contentManager
        val selectedContent = contentManager.selectedContent
        if (selectedContent != null && contentManager.contentCount > 1) {
            ContentsUtil.closeContentTab(contentManager, selectedContent)
        }
    }
}

private fun getToolwindow(project: Project?): ToolWindow? {
    if (project == null) return null
    return ToolWindowManager.getInstance(project).getToolWindow(KToolWindowFactory.ID)
}