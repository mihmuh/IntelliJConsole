package com.intellij.idekonsole.results

import com.intellij.find.FindBundle
import com.intellij.idekonsole.KSettings
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.context.runReadAndWait
import com.intellij.idekonsole.context.runReadLater
import com.intellij.idekonsole.scripting.IteratorSequence
import com.intellij.idekonsole.scripting.cacheHead
import com.intellij.idekonsole.scripting.collections.SequenceLike
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usages.*

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

    fun <T : Usage> showUsages(usages: Sequence<T>, searchQuery: String, refactoring: Runnable? = null) {
        myUsageViewSettings.loadState(usageViewSettings);
        val usagesViewManager = UsageViewManager.getInstance(project)

        val presentation = createPresentation(searchQuery, false)
        val usagesEvaluated = usages.cacheHead(KSettings.TIME_LIMIT)
        val usagesView = usagesViewManager.showUsages(emptyArray(), usagesEvaluated.evaluated.toTypedArray<Usage>(), presentation)

        val context = Context.instance()
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Searching") {
            override fun run(indicator: ProgressIndicator) {
                var remainingUsages: IteratorSequence<T> = usagesEvaluated.remaining
                while (!indicator.isCanceled) {
                    runReadAndWait(context) {
                        val evaluated = remainingUsages.asSequence().cacheHead(KSettings.TIME_LIMIT)
                        evaluated.evaluated.forEach {
                            usagesView.appendUsage(it)
                        }
                        remainingUsages = evaluated.remaining
                    }
                    if (remainingUsages.isEmpty()) {
                        break;
                    }
                }
                runReadLater(context) {
                    if (remainingUsages.isEmpty() && refactoring != null) {
                        val canNotMakeString = RefactoringBundle.message("usageView.need.reRun")
                        //todo deal with checkonly status and write action
                        val wrappedRefactoring = Context.wrapCallback { ApplicationManager.getApplication().runWriteAction(refactoring) }
                        usagesView.addPerformOperationAction(wrappedRefactoring, "", canNotMakeString, RefactoringBundle.message("usageView.doAction"), false)
                    }
                }
            }
        })
    }


    fun <T : Usage> showUsages(usages: SequenceLike<T>, searchQuery: String, refactoring: Runnable? = null) {
        myUsageViewSettings.loadState(usageViewSettings);
        val presentation = createPresentation(searchQuery, false)
        ProgressManager.getInstance().run(object: Task.Backgroundable(project, "Searching") {
            override fun run(progressIndicator: ProgressIndicator) {
                var usagesView: UsageView? = null
                var initiated = false
                usages.forEach {
                    if (usagesView == null) {
                        if (!initiated) {
                            initiated = true
                            ApplicationManager.getApplication().invokeLater {
                                ApplicationManager.getApplication().runWriteAction {
                                    usagesView = UsageViewManager.getInstance(project).showUsages(emptyArray(), arrayOf<Usage>(it), presentation)
                                }
                            }
                        } else {
                            ApplicationManager.getApplication().invokeLater {
                                ApplicationManager.getApplication().runReadAction {
                                    usagesView!!.appendUsage(it)
                                }
                            }
                        }
                    } else {
                         usagesView
                         usagesView!!.appendUsage(it)
                    }
                }
                if (usagesView == null) {
                    //todo: no usages found
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