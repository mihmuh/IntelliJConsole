package com.intellij.idekonsole

import com.intellij.ide.ui.AntialiasingType
import com.intellij.idekonsole.results.KCommandResult
import com.intellij.idekonsole.results.KExceptionResult
import com.intellij.idekonsole.results.KResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import sun.swing.SwingUtilities2
import java.awt.Dimension
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane

class KEditor(val project: Project) : Disposable {
    private val splitter: JBSplitter

    val module: Module
    val inputFile: VirtualFile
    val inputDocument: Document
    val inputPsiFile: PsiFile

    private val viewer = Viewer()
    private val editor: EditorEx
    private val scrollPane: JScrollPane

    init {
        module = KIdeaModuleBuilder.createModule(project)

        inputFile = KIdeaModuleBuilder.createConsoleFile(module)
        inputDocument = FileDocumentManager.getInstance().getDocument(inputFile)!!

        KTemplates.initHelperClasses(module)

        inputPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(inputDocument)!!

        editor = EditorFactory.getInstance().createEditor(inputDocument, project, inputFile, false) as EditorEx
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(inputDocument, {})

        resetInputContent()

        scrollPane = ScrollPaneFactory.createScrollPane(viewer)
        splitter = JBSplitter(true)
        splitter.setHonorComponentsMinimumSize(false)
        splitter.firstComponent = scrollPane
        splitter.secondComponent = editor.component

        object : DumbAwareAction() {
            override fun actionPerformed(event: AnActionEvent?) {
                handleCommand(inputDocument.text)
            }
        }.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor.component)
    }

    private fun resetInputContent() {
        write {
            val content = KTemplates.getConsoleContent()
            val caretOffset = KTemplates.getConsoleCaretOffset()
            val foldingStart = KTemplates.getConsoleFoldingStart()
            val foldingEnd = KTemplates.getConsoleFoldingEnd()


            inputDocument.setText(content)

            //            inputDocument.createGuardedBlock(0, offset)
            //            inputDocument.createGuardedBlock(offset + 1, inputDocument.textLength)

            editor.foldingModel.runBatchFoldingOperation({
                val regions = editor.foldingModel.allFoldRegions
                for (region in regions) {
                    editor.foldingModel.removeFoldRegion(region)
                }

                val region1 = editor.foldingModel.addFoldRegion(0, foldingStart, "")
                val region2 = editor.foldingModel.addFoldRegion(foldingEnd, inputDocument.textLength, "")
                region1?.isExpanded = false
                region2?.isExpanded = false
            })

            editor.caretModel.moveToOffset(caretOffset)
        }
    }

    fun handleCommand(text: String) {
        viewer.add(KCommandResult(text))
        val callback = KCommandHandler.compile(module, this)
        callback.doWhenDone(Runnable {
            ApplicationManager.getApplication().invokeLater {
                try {
                    viewer.add(callback.result.compute())
                } catch (e: Exception) {
                    viewer.add(KExceptionResult(e))
                }

                scrollPane.validate()
                scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
            }
        })
    }

    override fun dispose() {
        ApplicationManager.getApplication().invokeLater {
            write {
                inputFile.delete(this)
            }
        }
        EditorFactory.getInstance().releaseEditor(editor)
    }

    fun getComponent() = splitter

    private class Viewer() : JPanel() {
        val V_GAP = 5

        val results = ArrayList<KResult>()

        init {
            putClientProperty(SwingUtilities2.AA_TEXT_PROPERTY_KEY, AntialiasingType.getAAHintForSwingComponent())
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
        ApplicationManager.getApplication().runWriteAction(task)
    }

    fun show(result : KResult) : KResult {
        viewer.add(result)
        return result
    }
}