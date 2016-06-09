package com.intellij.idekonsole.scripting

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.idekonsole.KDataHolder
import com.intellij.idekonsole.KEditor
import com.intellij.idekonsole.results.KPsiElementsResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
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
import java.util.stream.Stream

//----------- find, refactor

fun usages(node: PsiElement, scope: SearchScope? = EverythingGlobalScope(project())): List<UsageInfo> {
    val project = project();
    val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(node, false);
    val processor = CommonProcessors.CollectProcessor<UsageInfo>()
    val psiElements = ArrayUtil.mergeArrays(handler!!.primaryElements, handler.secondaryElements)
    val options = handler.getFindUsagesOptions(null)
    if (scope != null) options.searchScope = scope
    for (psiElement in psiElements) {
        handler.processElementUsages(psiElement, processor, options)
    }
    return processor.getResults().map { it };
}

fun <T : PsiElement> instances(cls: PsiClassRef<T>): Stream<T> {
    return nodes().filter { cls.myRef.isAssignableFrom(it.javaClass) }.map { it as T }
}

fun nodes() : Stream<PsiElement> {
    val project = project()
    return project!!.packages().stream()
            .flatMap { it.getFiles(GlobalSearchScope.projectScope(project)).map { it!! }.stream() }
            .flatMap { it.children.map { it!! }.stream() }.filter { it !is PsiWhiteSpace }
}

fun <T : PsiElement> List<T>.refactor(refactoring: (T) -> Unit) {
    //todo show in usages view before refactoring
    this.forEach { refactoring(it); }
}

//------------ project structure navigation

//todo make for-internal-use
fun context(): PsiElement? {
    val editor = editor() ?: return null
    return editor.inputPsiFile;
}

fun project(): Project? = KDataHolder.project

private fun editor(): KEditor? = KDataHolder.editor

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

fun show(r : KResult) {
    editor()?.show(r)
}

fun show(s : String) {
    show(KStdoutResult(s))
}

fun show(vararg e : PsiElement) {
    show(e.toList())
}

fun show(e : List<Any>) {
    if (e.isNotEmpty()) {
        if (e.all { it is PsiElement }) {
            return show(KPsiElementsResult(e.filterIsInstance<PsiElement>()))
        }
        if (e.all { it is UsageInfo }) {
            return show(KPsiElementsResult(e.filterIsInstance<UsageInfo>().map { it.element!! }))
        }
    }
    show(e.toString())
}

fun show(o : Any) {
    show(o.toString())
}

fun show(u : UsageInfo) {
    show(u.element!!)
}

//todo there should be more "print" functions

//todo internal mode (impossible?)