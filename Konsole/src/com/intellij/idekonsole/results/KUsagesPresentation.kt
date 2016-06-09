package com.intellij.idekonsole.results

import com.intellij.find.FindBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usages.*
import com.intellij.usages.impl.UsageAdapter
import java.util.*

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

    fun <T:PsiElement>showUsages(elements: List<T>, searchQuery:String, refactoring: ((T) -> Unit)? = null, callback:Runnable? = null){
        myUsageViewSettings.loadState(usageViewSettings);
        val usagesViewManager = UsageViewManager.getInstance(project)

        val usages = LinkedList<Usage>()
        elements.forEach {
            if (it.isValid) {
                usages.add(UsageInfo2UsageAdapter(UsageInfo(it)))
            } else {
                usages.add(UsageAdapter())
            }
        }

        val presentation = createPresentation(searchQuery, refactoring != null)
        val usagesView = usagesViewManager.showUsages(emptyArray(), Array(usages.size, { i -> usages[i] }), presentation)
        val canNotMakeString = RefactoringBundle.message("usageView.need.reRun")

        //todo deal with checkonly status and write action
        if (refactoring != null) {
            usagesView.addPerformOperationAction({
                ApplicationManager.getApplication().runWriteAction {
                    elements.forEach { refactoring.invoke(it) }
                    callback?.run()
                }
            }, "", canNotMakeString, RefactoringBundle.message("usageView.doAction"), false)
        }
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