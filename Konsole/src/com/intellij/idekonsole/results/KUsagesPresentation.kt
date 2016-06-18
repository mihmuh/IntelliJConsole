package com.intellij.idekonsole.results

import com.intellij.find.FindBundle
import com.intellij.idekonsole.KSettings
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.context.runReadAndWait
import com.intellij.idekonsole.context.runReadLater
import com.intellij.idekonsole.scripting.collections.IteratorSequence
import com.intellij.idekonsole.scripting.collections.SequenceLike
import com.intellij.idekonsole.scripting.collections.cacheHead
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
import javax.swing.SwingUtilities

/**
 * @author simon
 */

class KUsagesPresentation {
    var usageViewSettings: UsageViewSettings = UsageViewSettings.getInstance();
    val myUsageViewSettings = UsageViewSettings();
    val project: Project

    constructor(project: Project) {
        this.project = project
    }


    fun <T : Usage> showUsages(usages: SequenceLike<T>, searchQuery: String, refactoring: Runnable? = null) {
        myUsageViewSettings.loadState(usageViewSettings);
        val presentation = createPresentation(searchQuery, false)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Searching") {
            override fun run(progressIndicator: ProgressIndicator) {
                val tooManyUsagesStatus = TooManyUsagesStatus.createFor(progressIndicator)
                var usagesCount = 0
                var usagesView: UsageView? = null
                usages.forEach {
                    tooManyUsagesStatus.pauseProcessingIfTooManyUsages()
                    if (usagesCount == 0) {
                        usagesView = UsageViewManager.getInstance(project).createUsageView(emptyArray(), arrayOf<Usage>(it), presentation, null)
                        SwingUtilities.invokeLater {
                            UsagesViewHelper.addContent(project, usagesView as UsageViewImpl, presentation)
                            com.intellij.usageView.UsageViewManager.getInstance(project)
                            val toolWindow = ToolWindowManager.getInstance(this.myProject).getToolWindow(ToolWindowId.FIND)
                            toolWindow.show(null)
                            toolWindow.activate(null)

                        }
                    } else if (usagesCount == KSettings.MAX_USAGES) {
                        ApplicationManager.getApplication().invokeLater {
                            TooManyUsagesStatus.getFrom(progressIndicator).switchTooManyUsagesStatus()
                            val dialogAnswer = MessagesEx.showYesNoDialog("More than ${KSettings.MAX_USAGES} usages found.\nDo you want to continue?", "Too Many Results", null)
                            if (dialogAnswer != MessagesEx.YES) {
                                progressIndicator.cancel()
                            }
                            tooManyUsagesStatus.userResponded()
                        }
                    }
                    usagesView!!.appendUsage(it)
                    usagesCount++;
                }

                if (usagesView == null) {
                    //todo: if no usages found
                } else if (!progressIndicator.isCanceled && refactoring != null) {
                    val canNotMakeString = RefactoringBundle.message("usageView.need.reRun")
                    //todo deal with checkonly status and write action
                    val wrappedRefactoring = Context.wrapCallback { ApplicationManager.getApplication().runWriteAction(refactoring) }
                    usagesView!!.addPerformOperationAction(wrappedRefactoring, "", canNotMakeString, RefactoringBundle.message("usageView.doAction"), false)
                }
            }
        })
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