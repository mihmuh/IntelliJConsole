package com.intellij.idekonsole.results

import com.intellij.find.FindManager
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.find.impl.FindManagerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usages.*
import java.util.*

/**
 * @author simon
 */

//todo set cool searched items
class UsagesPresentation {
    var usageViewSettings: UsageViewSettings = UsageViewSettings.getInstance();
    val myUsageViewSettings = UsageViewSettings();
    val project:Project
    constructor(project:Project) {
        this.project = project
    }

    fun showUsages(elements: List<PsiElement>){
        myUsageViewSettings.loadState(usageViewSettings);
        val usagesViewManager = UsageViewManager.getInstance(project)

        val usages = LinkedList<Usage>()
        elements.forEach { usages.add(UsageInfo2UsageAdapter(UsageInfo(it))) }
        val presentation = createPresentation()
        usagesViewManager.showUsages(emptyArray(), Array(usages.size,{ i -> usages[i] } ), presentation)
    }

    private fun createPresentation(): UsageViewPresentation {
        val presentation = UsageViewPresentation()
        presentation.targetsNodeText = "Searched items"

        presentation.isShowCancelButton = false

        //todo.....
        presentation.usagesString = RefactoringBundle.message("usageView.usagesText")
        presentation.tabText = RefactoringBundle.message("usageView.tabText")

        return presentation
    }
}