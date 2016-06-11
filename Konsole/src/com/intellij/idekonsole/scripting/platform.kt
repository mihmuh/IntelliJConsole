package com.intellij.idekonsole.scripting

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.idekonsole.KDataHolder
import com.intellij.idekonsole.KEditor
import com.intellij.idekonsole.results.KHelpResult
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
import com.intellij.idekonsole.results.KUsagesResult
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
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes

//----------- find, refactor

fun usages(node: PsiElement, scope: GlobalSearchScope? = KDataHolder.scope!!): List<PsiReference> {
    val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(node, false);
    val processor = CommonProcessors.CollectProcessor<UsageInfo>()
    val psiElements = ArrayUtil.mergeArrays(handler!!.primaryElements, handler.secondaryElements)
    val options = handler.getFindUsagesOptions(null)
    if (scope != null) options.searchScope = scope
    for (psiElement in psiElements) {
        handler.processElementUsages(psiElement, processor, options)
    }
    return processor.getResults().map { it.reference }.filterNotNull();
}

fun <T : PsiElement> instances(cls: PsiClassRef<T>, scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<T> {
    return nodes(scope).withKind(cls)
}

fun Module.sourceRoots(): List<PsiDirectory> {
    val sourceRoots = ModuleRootManager.getInstance(this).getSourceRoots(JavaModuleSourceRootTypes.SOURCES)
    return sourceRoots.map { PsiManager.getInstance(project).findDirectory(it) }.filterNotNull()
}

fun <T> deepSearch(seed: T, f: (T) -> Sequence<T>): Sequence<T> {
    return sequenceOf(seed) + f(seed).flatMap { deepSearch(it, f) }
}

fun <T> wideSearch(seed: Sequence<T>, f: (T) -> Sequence<T>): Sequence<T> {
    return seed + wideSearch(seed.flatMap(f), f)
}

fun nodes(scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<PsiElement> {
    return files(scope).flatMap { it.descendants() }
}

fun files(scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<PsiFile> {
    return project.modules().asSequence()
            .flatMap { it.sourceRoots().asSequence() }
            .flatMap { deepSearch(it, {i -> i.subdirectories.asSequence()}) }
            .flatMap { it.files.asSequence() }
}

fun PsiElement.descendants(): Sequence<PsiElement> {
    return sequenceOf(this).plus(this.children.asSequence().flatMap { it.descendants() })
}

class Refactoring<T : PsiElement>(elements: List<T>, refactoring: (T) -> Unit) {
    val elements: List<T> = elements
    val refactoring: (T) -> Unit = refactoring
}

fun <T : PsiElement> List<T>.refactor(refactoring: (T) -> Unit) {
    show(Refactoring(this, refactoring))
}

fun <T : PsiElement> Sequence<T>.refactor(refactoring: (T) -> Unit) = toList().refactor(refactoring)

//------------ project structure navigation

//todo make for-internal-use
fun context(): PsiElement? {
    val editor = editor() ?: return null
    return editor.inputPsiFile;
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

fun Project.modules(): List<Module> {
    return ModuleManager.getInstance(this).modules.filterNotNull();
}

val help = { KHelpResult("I'm the help of your dream"); }

//------------ util

fun <T : PsiNamedElement> List<T>.withName(name: String): List<T> = this.filter { it.name == name };

fun <T : PsiNamedElement> List<T>.oneWithName(name: String): T? = this.withName(name).firstOrNull();

fun <T : PsiElement> List<PsiElement>.withKind(k: PsiClassRef<T>): List<T> {
    return this.filterIsInstance(k.myRef);
}

fun <T : PsiElement> Sequence<PsiElement>.withKind(k: PsiClassRef<T>): Sequence<T> {
    return this.filter { k.myRef.isAssignableFrom(it.javaClass) }.map { it as T };
}

fun show(r: KResult) {
    editor()?.addResult(r)
}

fun show(s: String) {
    show(KStdoutResult(s))
}

fun show(vararg e: PsiElement) {
    show(e.toList())
}

private val EMPTY_SEQ = "Empty sequence"

fun <T : PsiElement> show(refactoring: Refactoring<T>) {
    if (refactoring.elements.isNotEmpty()) {
        val result = KUsagesResult(refactoring.elements.asSequence(), "", editor(), refactoring.refactoring)
        result.openUsagesView()
        return show(result)
    }
    show(EMPTY_SEQ)
}

fun show(e: Sequence<Any?>) {
    if (e.first() is PsiElement) {
        return show(KUsagesResult(e.filterIsInstance<PsiElement>(), "", editor()))
    }
    if (e.first() is PsiReference) {
        return show(KUsagesResult(e.map { (it as PsiReference).element!! }, "", editor()))
    }
    if (e.first() is KResult) {
        return e.forEach { show(it as KResult) }
    }
}

fun show(e: List<Any?>) {
    if (e.isNotEmpty()) {
        show(e.asSequence())
    } else {
        return show(EMPTY_SEQ)
    }
    show(e.toString())
}

fun show(f: () -> Any?) {
    show(f())
}

fun show(o: Any?) {
    if (o == null) {
        show("null")
    } else if (o is Unit) {
        //do nothing
    } else if (o is (() -> Any?)) {
        show(o)
    } else if (o is KResult) {
        show(o)
    } else if (o is String) {
        show(o)
    } else if (o is List<*>) {
        show(o)
    } else if (o is PsiElement) {
        show(o)
    } else if (o is Sequence<*>) {
        show(o as Sequence<Any?>)
    } else {
        show(o.toString())
    }
}

fun show(u: PsiReference) {
    show(u.element!!)
}

fun <T> T.hasValue(e: T): Boolean {
    return this == e
}