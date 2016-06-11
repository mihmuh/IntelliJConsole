package com.intellij.idekonsole.scripting

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.idekonsole.KDataHolder
import com.intellij.idekonsole.KEditor
import com.intellij.idekonsole.results.KHelpResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.ArrayUtil
import com.intellij.util.CommonProcessors

//----------- find, refactor

fun usages(node: PsiElement, scope: GlobalSearchScope? = KDataHolder.scope!!): List<PsiReference> {
    val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(node, false)!!
    val processor = CommonProcessors.CollectProcessor<UsageInfo>()
    val psiElements = ArrayUtil.mergeArrays(handler.primaryElements, handler.secondaryElements)
    val options = handler.getFindUsagesOptions(null)
    if (scope != null) options.searchScope = scope
    for (psiElement in psiElements) {
        handler.processElementUsages(psiElement, processor, options)
    }
    return processor.results.map { it.reference }.filterNotNull()
}

fun nodes(scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<PsiElement> =
        files(scope).flatMap { it.descendants() }

fun <T : PsiElement> instances(cls: PsiClassRef<T>, scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<T> =
        nodes(scope).withKind(cls)

fun Module.sourceRoots(): List<PsiDirectory> =
        ModuleRootManager.getInstance(this).sourceRoots
                .map { PsiManager.getInstance(project).findDirectory(it) }.filterNotNull()

fun files(scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<PsiFile> {
    return project.modules(scope).asSequence()
            .flatMap { it.sourceRoots().asSequence() }
            .flatMap { deepSearch(it, {i -> i.subdirectories.asSequence()}) }
            .flatMap { it.files.asSequence() }
            .filter { scope.contains(it.virtualFile) }
}

fun PsiElement.descendants(): Sequence<PsiElement> {
    return sequenceOf(this).plus(this.children.asSequence().flatMap { it.descendants() })
}

class Refactoring<T : PsiElement>(elements: Sequence<T>, refactoring: (T) -> Unit) {
    val elements: Sequence<T> = elements
    val refactoring: (T) -> Unit = refactoring
}

fun <T : PsiElement> List<T>.refactor(refactoring: (T) -> Unit) =
        asSequence().refactor(refactoring)

fun <T : PsiElement> Sequence<T>.refactor(refactoring: (T) -> Unit) =
        show(Refactoring(this, refactoring))

@JvmName("refactorReferences") fun List<PsiReference>.refactor(refactoring: (PsiElement) -> Unit) =
        asSequence().refactor(refactoring)

@JvmName("refactorReferences") fun Sequence<PsiReference>.refactor(refactoring: (PsiElement) -> Unit) =
        map { it.resolve() }.filterNotNull().asSequence().refactor(refactoring)

//------------ project structure navigation

//todo make for-internal-use
fun context(): PsiElement? {
    val editor = editor() ?: return null
    return editor.inputPsiFile
}

val project: Project
    get() = KDataHolder.project!!

var scope: GlobalSearchScope
    get() = KDataHolder.scope!!
    set(s: GlobalSearchScope) {
        KDataHolder.scope = s
    }

val globalScope: GlobalSearchScope
    get() = EverythingGlobalScope.allScope(project)

val projectScope: GlobalSearchScope
    get() = EverythingGlobalScope.projectScope(project)

private fun editor(): KEditor? = KDataHolder.editor

fun Project.modules(scope: GlobalSearchScope = KDataHolder.scope!!): List<Module> =
        ModuleManager.getInstance(this).modules.filter { scope.isSearchInModuleContent(it) }.toList()

val help = { KHelpResult("I'm the help of your dream") }

//------------ util

fun <T : PsiNamedElement> List<T>.withName(name: String): List<T> =
        filter { it.name == name }

fun <T : PsiNamedElement> List<T>.oneWithName(name: String): T? =
        withName(name).firstOrNull()

fun <T : PsiElement> List<PsiElement>.withKind(k: PsiClassRef<T>): List<T> =
        filterIsInstance(k.myRef)

fun <T : PsiElement> Sequence<PsiElement>.withKind(k: PsiClassRef<T>): Sequence<T> =
        filterIsInstance(k.myRef)

fun <T> T.hasValue(e: T): Boolean {
    return this == e
}