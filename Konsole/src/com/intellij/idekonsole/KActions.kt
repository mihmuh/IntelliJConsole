package com.intellij.idekonsole

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ClearOutputAction : DumbAwareAction("Clear Output", null, AllIcons.General.Remove) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(KDataKeys.K_EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(KDataKeys.K_EDITOR)
        editor.clearOutput()
    }
}

class ExecuteAction : DumbAwareAction("Execute", null, AllIcons.Actions.Rerun) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(KDataKeys.K_EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(KDataKeys.K_EDITOR)
        editor.handleCommand()
    }
}