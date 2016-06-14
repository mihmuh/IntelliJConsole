package com.intellij.idekonsole.scripting

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.results.KHelpResult
import com.intellij.idekonsole.scripting.collections.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.ArrayUtil

//----------- find, refactor

private fun defaultScope(): GlobalSearchScope = Context.instance().scope
private fun defaultProject(): Project = Context.instance().project

fun usages(node: PsiElement, scope: GlobalSearchScope = defaultScope(), project: Project = defaultProject()): SequenceLike<PsiReference> {
    val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(node, false)!!
    val psiElements = ArrayUtil.mergeArrays(handler.primaryElements, handler.secondaryElements)
    val options = handler.getFindUsagesOptions(null)
    options.searchScope = scope
    return psiElements.asSequence().asSequenceLike()
            .flatMap { psiElement -> sequenceLikeProcessor<UsageInfo?> { processor -> handler.processElementUsages(psiElement, processor, options) }}
            .map { it?.reference }
            .filterNotNull().wrapWithRead()
}

fun nodes(scope: GlobalSearchScope = defaultScope()): Sequence<PsiElement> =
        files(scope).flatMap { it.descendants() }

fun <T : PsiElement> instances(cls: PsiClassRef<T>, scope: GlobalSearchScope = defaultScope()): Sequence<T> =
        nodes(scope).withKind(cls)

fun Module.sourceRoots(project: Project = defaultProject()): List<PsiDirectory> =
        ModuleRootManager.getInstance(this).sourceRoots
                .map { PsiManager.getInstance(project).findDirectory(it) }.filterNotNull()

fun files(scope: GlobalSearchScope = defaultScope(), project: Project = defaultProject()): Sequence<PsiFile> {
    return project.modules(scope).asSequence()
            .flatMap { it.sourceRoots().asSequence() }
            .flatMap { deepSearch(it, { i -> i.subdirectories.asSequence() }) }
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

fun Project.modules(scope: GlobalSearchScope = defaultScope()): List<Module> =
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