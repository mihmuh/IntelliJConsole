package com.intellij.idekonsole

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.ide.ui.UISettings
import com.intellij.idekonsole.results.KCommandResult
import com.intellij.idekonsole.results.KResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane

class KEditor(val project: Project) : Disposable {
    val component: JPanel

    private val inputFile: VirtualFile
    private val inputDocument: Document

    private val viewer = Viewer()
    private val editor: EditorEx
    private val scrollPane: JScrollPane

    init {
        val kotlinType = FileTypeManagerEx.getInstance().getFileTypeByExtension("kt") as LanguageFileType
        val kotlinLanguage = kotlinType.language

        inputFile = ScratchRootType.getInstance().createScratchFile(project, "Konsole", kotlinLanguage, "",
                ScratchFileService.Option.create_if_missing)!!
        inputDocument = FileDocumentManager.getInstance().getDocument(inputFile)!!

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(inputDocument)!!
        psiFile.putUserData(ModuleUtilCore.KEY_MODULE, KSettings.instance.getModule(project))

        editor = EditorFactory.getInstance().createEditor(inputDocument, project, inputFile, false) as EditorEx
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(inputDocument, {})

        resetInputContent()

        scrollPane = ScrollPaneFactory.createScrollPane(viewer)
        component = JPanel(BorderLayout())
        component.add(scrollPane, BorderLayout.CENTER)
        component.add(editor.component, BorderLayout.SOUTH)

        object : AnAction() {
            override fun actionPerformed(event: AnActionEvent?) {
                handleCommand(inputDocument.text)

                resetInputContent()

                scrollPane.validate()
                scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
            }
        }.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor.component)
    }

    private fun resetInputContent() {
        write {
            val offset = KSettings.INITIAL_CONTENT.indexOf(KSettings.PLACEHOLDER)
            val content = KSettings.INITIAL_CONTENT.replace(KSettings.PLACEHOLDER, "\n")

            inputDocument.setText(content)

            inputDocument.createGuardedBlock(0, offset)
            inputDocument.createGuardedBlock(offset + 1, inputDocument.textLength)

            editor.foldingModel.runBatchFoldingOperation({
                val regions = editor.foldingModel.allFoldRegions
                for (region in regions) {
                    editor.foldingModel.removeFoldRegion(region)
                }

                val region1 = editor.foldingModel.addFoldRegion(0, offset, "")
                val region2 = editor.foldingModel.addFoldRegion(offset + 1, inputDocument.textLength, "")
                region1?.isExpanded = false
                region2?.isExpanded = false
            })

            editor.caretModel.moveToOffset(offset)
        }
    }

    fun handleCommand(text: String) {
        viewer.add(KCommandResult(text))
        viewer.add(KCommandHandler.handle(text))
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }

    private class Viewer() : JPanel() {
        val V_GAP = 5

        val results = ArrayList<KResult>()

        init {
            UISettings.setupComponentAntialiasing(this)

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

    private fun write(task: () -> Unit) {
        WriteAction.run<RuntimeException> {
            task()
        }
    }
}