package com.intellij.idekonsole

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.actions.ContentChooser
import com.intellij.openapi.project.DumbAwareAction
import java.util.*

class ClearOutputAction : DumbAwareAction("Clear Output", null, AllIcons.Actions.Clean) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(KDataKeys.K_EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(KDataKeys.K_EDITOR)
        editor.clearAll()
    }
}

class ExecuteAction : DumbAwareAction("Run", null, AllIcons.General.Run) {
    override fun update(e: AnActionEvent) {
        val editor = e.getData(KDataKeys.K_EDITOR)
        e.presentation.isEnabled = e.place == ActionPlaces.MAIN_MENU ||
                editor != null && !editor.containsErrors()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(KDataKeys.K_EDITOR)
        editor.handleCommand()
    }
}

class ConsoleHistoryAction : DumbAwareAction("Show History", null, AllIcons.General.MessageHistory) {
    override fun update(e: AnActionEvent) {
        val editor = e.getData(KDataKeys.K_EDITOR)
        e.presentation.isEnabled = editor != null && e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(KDataKeys.K_EDITOR)
        val project = e.project!!

        val contentChooser = object : ContentChooser<String>(project, "Console History", false) {
            override fun removeContentAt(content: String) {
                KSettings.instance.removeConsoleHistory(content)
            }

            override fun getStringRepresentationFor(content: String): String {
                return content
            }

            override fun getContents(): List<String> {
                val recentMessages = KSettings.instance.getConsoleHistory()
                Collections.reverse(recentMessages)
                return recentMessages
            }
        }

        if (contentChooser.showAndGet()) {
            val selectedIndex = contentChooser.selectedIndex

            if (selectedIndex >= 0) {
                val selectedText = contentChooser.allContents[selectedIndex]
                editor.setText(selectedText)
            }
        }
    }
}

class PreviousCommandAction : DumbAwareAction("Previous command", null, AllIcons.Actions.PreviousOccurence){
    override fun actionPerformed(e: AnActionEvent?) {
        e?.getData(KDataKeys.K_EDITOR)?.prevCmd()
    }
}

class NextCommandAction : DumbAwareAction("Next command", null, AllIcons.Actions.NextOccurence){
    override fun actionPerformed(e: AnActionEvent?) {
        e?.getData(KDataKeys.K_EDITOR)?.nextCmd()
    }
}