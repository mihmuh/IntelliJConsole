package com.intellij.idekonsole.results

import com.intellij.find.FindBundle
import com.intellij.idekonsole.KSettings
import com.intellij.idekonsole.scripting.IteratorSequence
import com.intellij.idekonsole.scripting.evaluate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usages.Usage
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageViewPresentation
import com.intellij.usages.UsageViewSettings

/**
 * @author simon
 */

class KUsagesPresentation {
    var usageViewSettings: UsageViewSettings = UsageViewSettings.getInstance();
    val myUsageViewSettings = UsageViewSettings();
    val project:Project
    constructor(project:Project) {
        this.project = project
    }

    fun <T: Usage> showUsages(usages: Sequence<T>, searchQuery: String, refactoring: Runnable? = null){
        myUsageViewSettings.loadState(usageViewSettings);
        val usagesViewManager = UsageViewManager.getInstance(project)

        val presentation = createPresentation(searchQuery, false)
        val usagesEvaluated = usages.evaluate(KSettings.TIME_LIMIT)
        val usagesView = usagesViewManager.showUsages(emptyArray(), usagesEvaluated.evaluated.toTypedArray<Usage>(), presentation)

        ProgressManager.getInstance().run(object: Task.Backgroundable(project, "Searching") {
            override fun run(indicator: ProgressIndicator) {
                var remainingUsages: IteratorSequence<T> = usagesEvaluated.remaining
                while (!indicator.isCanceled) {
                    ApplicationManager.getApplication().invokeAndWait({
                        ApplicationManager.getApplication().runReadAction {
                            val evaluated = remainingUsages.asSequence().evaluate(KSettings.TIME_LIMIT)
                            evaluated.evaluated.forEach {
                                usagesView.appendUsage(it)
                            }
                            remainingUsages = evaluated.remaining
                        }
                    }, ModalityState.NON_MODAL)
                    if (remainingUsages.isEmpty()) {
                        break;
                    }
                }
                ApplicationManager.getApplication().invokeLater{
                    ApplicationManager.getApplication().runReadAction {
                        if (remainingUsages.isEmpty() && refactoring != null) {
                            val canNotMakeString = RefactoringBundle.message("usageView.need.reRun")
                            //todo deal with checkonly status and write action
                            usagesView.addPerformOperationAction({
                                ApplicationManager.getApplication().runWriteAction(refactoring)
                            }, "", canNotMakeString, RefactoringBundle.message("usageView.doAction"), false)
                        }
                    }
                }
            }
        })
    }


    private fun createPresentation(searchQuery: String, isShowCancelButton : Boolean): UsageViewPresentation {
        val presentation = UsageViewPresentation()
        presentation.targetsNodeText = "Searched items"

        presentation.isShowCancelButton = isShowCancelButton

        //todo.....
        presentation.usagesString = searchQuery
        presentation.tabText = FindBundle.message("find.usages.ambiguous.title")

        return presentation
    }
}