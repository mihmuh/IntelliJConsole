package com.intellij.idekonsole

import com.intellij.ide.ui.AntialiasingType
import com.intellij.idekonsole.results.KCommandResult
import com.intellij.idekonsole.results.KExceptionResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.scripting.show
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
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

    private var textMarker: RangeMarker? = null

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
        splitter.proportion = 0.7f
    }

    private fun resetInputContent() {
        write {
            val content = KTemplates.getConsoleContent()
            val caretOffset = KTemplates.getConsoleCaretOffset()
            val foldingStart = KTemplates.getConsoleFoldingStart()
            val foldingEnd = KTemplates.getConsoleFoldingEnd()


            inputDocument.setText(content)

            textMarker = inputDocument.createRangeMarker(foldingStart, foldingEnd)

            for (b in KTemplates.getConsoleBlocks()) {
                inputDocument.createGuardedBlock(b - 1, b)
            }

            val fModel = editor.foldingModel
            fModel.runBatchFoldingOperation({
                val regions = fModel.allFoldRegions
                for (region in regions) {
                    fModel.removeFoldRegion(region)
                }

                val fGroup = FoldingGroup.newGroup("one")
                val region1 = fModel.createFoldRegion(0, foldingStart, "> ", fGroup, false)
                fModel.addFoldRegion(region1!!)
                val region2 = fModel.createFoldRegion(foldingEnd, inputDocument.textLength, "", fGroup, false)
                fModel.addFoldRegion(region2!!)

                region1.isExpanded = false
                region2.isExpanded = false
            })

            editor.caretModel.moveToOffset(caretOffset)
        }
    }

    fun handleCommand() {
        if (containsErrors()) return


        val text = inputDocument.text
        KSettings.instance.appendConsoleHistory(text)

        val marker = textMarker
        val commandText = if (marker != null && marker.isValid) {
            text.substring(marker.startOffset, marker.endOffset)
        } else {
            text
        }
        viewer.add(KCommandResult(commandText))
        val callback = KCommandHandler.compile(module, this)
        callback.doWhenDone(Runnable {
            ApplicationManager.getApplication().invokeLater {
                try {
                    callback.result.compute()
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

    fun addResult(result: KResult): KResult {
        viewer.add(result)
        return result
    }

    fun clearOutput() {
        viewer.clear()
        resetInputContent()
    }

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

    fun containsErrors(): Boolean {
        return KErrorUtil.containsError(project, editor)
    }

    fun setText(selectedText: String) {
        write {
            inputDocument.setText(selectedText)
        }
    }
}