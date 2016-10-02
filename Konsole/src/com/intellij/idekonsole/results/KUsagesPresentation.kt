package com.intellij.idekonsole.results

import com.intellij.find.FindBundle
import com.intellij.idekonsole.KSettings
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.scripting.collections.SequenceLike
import com.intellij.idekonsole.usages.UsagesViewHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.TooManyUsagesStatus
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usages.*
import com.intellij.usages.impl.UsageViewImpl

/**
 * @author simon
 */

class UsagesViewUsagesListener(val project: Project, searchQuery: String, val refactoring: Runnable? = null): UsagesListener<Usage> {
    var usageViewSettings: UsageViewSettings = UsageViewSettings.getInstance();
    val myUsageViewSettings = UsageViewSettings();
    init {
        myUsageViewSettings.loadState(usageViewSettings)
    }

    val presentation = createPresentation(searchQuery, false)
    var usagesView: UsageView? = null

    override fun processFirstUsage(usage: Usage) {
        usagesView = UsageViewManager.getInstance(project).createUsageView(emptyArray(), arrayOf<Usage>(usage), presentation, null)
        ApplicationManager.getApplication().invokeLater {
            UsagesViewHelper.addContent(project, usagesView as UsageViewImpl, presentation)
            com.intellij.usageView.UsageViewManager.getInstance(project)
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.FIND)
            toolWindow.show(null)
            toolWindow.activate(null)
        }
    }
    override fun processOthers(usage: Usage) {
        usagesView!!.appendUsage(usage)
    }
    override fun finished() {
        if (refactoring != null) {
            val canNotMakeString = RefactoringBundle.message("usageView.need.reRun")
            //todo deal with checkonly status and write action
            val wrappedRefactoring = Context.wrapCallback { ApplicationManager.getApplication().runWriteAction(refactoring) }
            usagesView!!.addPerformOperationAction(wrappedRefactoring, "", canNotMakeString, RefactoringBundle.message("usageView.doAction"), false)
        }
    }
    override fun empty() {
        //todo: if no usages found
    }
    override fun cancelled() {
        //nothing to do
    }
    override fun askTooManyUsagesContinue(): Boolean {
        return MessagesEx.showYesNoDialog("More than ${KSettings.MAX_USAGES} usages found.\nDo you want to continue?", "Too Many Results", null) == MessagesEx.YES
    }

    private fun createPresentation(searchQuery: String, isShowCancelButton: Boolean): UsageViewPresentation {
        val presentation = UsageViewPresentation()
        presentation.targetsNodeText = "Searched items"

        presentation.isShowCancelButton = isShowCancelButton

        //todo.....
        presentation.usagesString = searchQuery
        presentation.tabText = FindBundle.message("find.usages.ambiguous.title")

        return presentation
    }
}

interface UsagesListener<T> {
    fun processFirstUsage(usage: T)
    fun processOthers(usage: T)
    fun finished()
    fun empty()
    fun cancelled()
    //this method is called in EDT
    fun askTooManyUsagesContinue(): Boolean
}

fun <T> UsagesListener<T>.showUsages(project: Project, usages: SequenceLike<T>) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Searching") {
        override fun run(progressIndicator: ProgressIndicator) {
            val tooManyUsagesStatus = TooManyUsagesStatus.createFor(progressIndicator)
            var usagesCount = 0
            usages.forEach {
                tooManyUsagesStatus.pauseProcessingIfTooManyUsages()
                if (usagesCount == 0) {
                    processFirstUsage(it)
                } else {
                    processOthers(it)
                }
                if (usagesCount == KSettings.MAX_USAGES) {
                    ApplicationManager.getApplication().invokeLater {
                        TooManyUsagesStatus.getFrom(progressIndicator).switchTooManyUsagesStatus()
                        if (!askTooManyUsagesContinue()) {
                            progressIndicator.cancel()
                        }
                        tooManyUsagesStatus.userResponded()
                    }
                }
                usagesCount++;
            }

            if (!progressIndicator.isCanceled) {
                if (usagesCount > 0) {
                    finished()
                } else {
                    empty()
                }
            } else {
                cancelled()
            }
        }
    })
}