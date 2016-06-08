package com.intellij.idekonsole

import com.intellij.idekonsole.results.KCommandResult
import com.intellij.idekonsole.results.KResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane

class KEditor(val project: Project) : Disposable {
    val component: JPanel

    private val viewer = Viewer()
    private val input: EditorTextField
    private val scrollPane: JScrollPane

    init {
        input = EditorTextField()

        scrollPane = ScrollPaneFactory.createScrollPane(viewer)
        component = JPanel(BorderLayout())
        component.add(scrollPane, BorderLayout.CENTER)
        component.add(input, BorderLayout.SOUTH)

        object : AnAction() {
            override fun actionPerformed(event: AnActionEvent?) {
                handleCommand(input.text)
                input.text = ""

                scrollPane.validate()
                scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
            }
        }.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, input)
    }

    fun handleCommand(text: String) {
        viewer.add(KCommandResult(text))
        viewer.add(KCommandHandler.handle(text))
    }

    override fun dispose() {
    }

    private class Viewer() : JPanel() {
        val V_GAP = 5

        val results = ArrayList<KResult>()

        init {
            background = JBColor.WHITE
        }

        fun add(result: KResult) {
            results.add(result)
            add(result.getPresentation())
        }

        fun clear() {
            results.clear()
            removeAll()
        }

        override fun getPreferredSize(): Dimension? {
            var width = 0
            var height = 0

            for (result in results) {
                val presentation = result.getPresentation()
                val size = presentation.preferredSize
                width = Math.max(width, size.width)
                height += size.height + V_GAP
            }


            return Dimension(width, height)
        }

        override fun doLayout() {
            var width = width
            var y = 0

            for (result in results) {
                val presentation = result.getPresentation()
                val size = presentation.preferredSize

                presentation.setBounds(0, y, width, size.height)
                y += size.height + V_GAP
            }
        }
    }
}