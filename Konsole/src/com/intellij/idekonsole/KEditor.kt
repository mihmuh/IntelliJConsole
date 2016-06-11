package com.intellij.idekonsole

import com.intellij.ide.ui.AntialiasingType
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.results.KCommandResult
import com.intellij.idekonsole.results.KExceptionResult
import com.intellij.idekonsole.results.KHelpResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.DocumentEx
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
import com.intellij.util.ui.JBEmptyBorder
import sun.swing.SwingUtilities2
import java.awt.Color
import java.awt.Dimension
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class KEditor(val project: Project) : Disposable, ConsoleOutput {
    private val splitter: JBSplitter

    val module: Module
    val inputFile: VirtualFile
    val inputDocument: Document
    val inputPsiFile: PsiFile
    val context = Context(project = project, output = this)

    private val history: KSettings.ConsoleHistory
        get() = KSettings.instance.getConsoleHistory()
    private var histIndex = -1;

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

        setText(KTemplates.consoleContent)

        scrollPane = ScrollPaneFactory.createScrollPane(viewer)
        scrollPane.viewportBorder = JBEmptyBorder(5, 5, 3, 3)
        scrollPane.background = Color.LIGHT_GRAY

        splitter = JBSplitter(true)
        splitter.firstComponent = scrollPane
        //cannot set visible=false since it causes lags in highlighting
        //editor.gutterComponentEx.parent.isVisible = false
        splitter.secondComponent = editor.component
        splitter.proportion = 0.7f
    }

    private fun updateFoldingAndBlocks() {
        val blocks = KTemplates.getConsoleBlocks(inputDocument.text)
        for (b in ArrayList((inputDocument as DocumentEx).guardedBlocks)) {
            inputDocument.removeGuardedBlock(b)
        }

        inputDocument.createGuardedBlock(0, blocks[0])
        inputDocument.createGuardedBlock(blocks[1], inputDocument.text.length)

        val fModel = editor.foldingModel
        fModel.runBatchFoldingOperation({
            val regions = fModel.allFoldRegions
            for (region in regions) {
                fModel.removeFoldRegion(region)
            }

            val fGroup = FoldingGroup.newGroup("one")
            val region1 = fModel.createFoldRegion(0, KTemplates.getConsoleFolding1End(inputDocument.text), "-> ", fGroup, true)
            fModel.addFoldRegion(region1!!)
            val region2 = fModel.createFoldRegion(KTemplates.getConsoleFolding2Start(inputDocument.text), inputDocument.textLength, " ", fGroup, true)
            fModel.addFoldRegion(region2!!)

            region1.isExpanded = false
            region2.isExpanded = false
        })

        editor.caretModel.moveToOffset(KTemplates.getConsoleCaretOffset(inputDocument.text))
    }

    fun handleCommand() {
        if (containsErrors()) return

        write {
            val callback = KCommandHandler.compile(module, context)
            callback.doWhenDone(Runnable {
                ApplicationManager.getApplication().invokeLater {
                    val text = inputDocument.text
                    val commandText = trimCommand(text)
                    addResult(KCommandResult(commandText))
                    if (histIndex != -1) {
                        history.removeAt(history.size-1)
                        histIndex = -1
                    }
                    history.smartAppend(text)
                    setText(KTemplates.consoleContent)
                    editor.scrollingModel.scrollVertically(0)

                    Thread({
                        Thread.sleep(1000)
                        SwingUtilities.invokeLater {
                            try {
                                callback.result.compute()
                            } catch (e: Exception) {
                                viewer.add(KExceptionResult(project, e))
                            }
                        }
                    }).start()
                }
            })
        }
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

    override fun addResult(result: KResult) {
        viewer.add(result)
        scrollToEnd()
    }

    fun scrollToEnd() {
        scrollPane.validate()
        scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
    }

    override fun addResultAfter(result: KResult, anchor: KResult) {
        viewer.addAfter(result, anchor)
        if (viewer.results.last() == result) {
            scrollToEnd()
        }
    }

    fun clearAll() {
        viewer.clear()
        setText(KTemplates.consoleContent)
        history.clear()
        histIndex = -1;
    }

    fun nextCmd() {
        write {
            if (history.size == 0) return@write
            if (histIndex == -1) return@write
            if (histIndex == history.size - 2) {
                histIndex = -1;
                setText(history.last());
                history.remove(history.last())
                return@write
            }
            history.set(histIndex, inputDocument.text)
            histIndex++
            setText(history.get(histIndex));
        }
    }

    fun prevCmd() {
        write {
            if (history.size == 0) return@write
            if (histIndex == -1) {
                histIndex = history.size - 1;
                history.add(inputDocument.text)
            } else if (histIndex > 0) {
                history.set(histIndex, inputDocument.text)
                histIndex--
            }
            if (histIndex == -1) return@write;
            setText(history.get(histIndex));
        }
    }

    private class Viewer() : JPanel() {
        val V_GAP = 5

        val results = ArrayList<KResult>()

        init {
            putClientProperty(SwingUtilities2.AA_TEXT_PROPERTY_KEY, AntialiasingType.getAAHintForSwingComponent())
            background = JBColor.WHITE
            init()
        }

        fun add(result: KResult) {
            results.add(result)
            add(result.getPresentation())
            invalidate()
            validate()
        }

        fun addAfter(result: KResult, anchor: KResult) {
            val indexOfAnchor = results.indexOf(anchor)
            if (indexOfAnchor < 0) {
                add(result)
            } else {
                val indexToInsert = indexOfAnchor + 1
                results.add(indexToInsert, result)
                add(result.getPresentation(), indexToInsert)
            }
            invalidate()
            validate()
        }

        fun clear() {
            results.clear()
            removeAll()
            init()
            invalidate()
            validate()
        }

        private fun init() {
            background = KSettings.BACKGROUND_COLOR
            add(KHelpResult("" +
                    "Type an expression or statements to execute.\n" +
                    "Type \"help\" for a list of commands.\n" +
                    "Press Cmd/Ctrl+Enter to execute command."))
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
            val width = width
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
            updateFoldingAndBlocks()
        }
    }
}

fun trimCommand(text: String) = text.substring(KTemplates.getConsoleFolding1End(text), KTemplates.getConsoleFolding2Start(text)).trim()