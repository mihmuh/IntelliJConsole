package com.intellij.idekonsole.scripting

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.ide.DataManager
import com.intellij.ide.util.PackageUtil
import com.intellij.idekonsole.KDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.ArrayUtil
import com.intellij.util.CommonProcessors
import org.junit.Assert
import java.util.*

//----------- find, refactor

fun usages(node: PsiElement, scope: SearchScope? = EverythingGlobalScope(project())): List<PsiReference> {
    val project = project();
    val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(node, false);
    val processor = CommonProcessors.CollectProcessor<UsageInfo>()
    val psiElements = ArrayUtil.mergeArrays(handler!!.primaryElements, handler.secondaryElements)
    val options = handler.getFindUsagesOptions(null)
    if (scope != null) options.searchScope = scope
    for (psiElement in psiElements) {
        handler.processElementUsages(psiElement, processor, options)
    }
    return processor.getResults().map { it.reference }.requireNoNulls();
}

fun <T : PsiElement> instances(cls: PsiClassRef<T>): List<T> {
    return emptyList()
}

fun <T : PsiElement> List<T>.refactor(refactoring: (T) -> Unit) {
    //todo show in usages view before refactoring
    this.forEach { refactoring(it); }
}

//------------ project structure navigation

//todo make for-internal-use
fun context(): PsiElement? {
    val dc: DataContext? = DataManager.getInstance().getDataContextFromFocus().getResultSync(100);
    if (dc == null) return null;
    return KDataKeys.CONTEXT_CLASS.getData(dc);
}

fun project(): Project? {
    val dc: DataContext? = DataManager.getInstance().getDataContextFromFocus().getResultSync(100);
    if (dc == null) return null;
    return PlatformDataKeys.PROJECT.getData(dc);
}

fun Project.modules(): List<Module> {
    return ModuleManager.getInstance(this).modules.filterNotNull().toList();
}

fun PsiPackage.roots(scope: GlobalSearchScope = EverythingGlobalScope(project())): List<PsiFile> {
    val files = this.getFiles(scope);
    if (files == null) return emptyList();
    return files.requireNoNulls().toList();
}

fun help(): String = "I'm the help of your dream"

//------------ util

fun <T : PsiNamedElement> List<T>.withName(name: String): List<T> = this.filter { it.name == name };

fun <T : PsiNamedElement> List<T>.oneWithName(name: String): T? = this.withName(name).firstOrNull();

fun print(s: String) {
    val dc: DataContext? = DataManager.getInstance().getDataContextFromFocus().getResultSync(100);
    if (dc == null) return;
    val editor = KDataKeys.K_EDITOR.getData(dc)
    editor?.handleCommand(s);
}

//todo there should be more "print" functions

//todo internal mode (impossible?)