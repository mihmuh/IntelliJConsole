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
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.ArrayUtil
import com.intellij.util.CommonProcessors
import java.util.stream.Collectors
import java.util.stream.Stream

//----------- find, refactor

fun usages(node: PsiElement, scope: GlobalSearchScope? = KDataHolder.scope!!): List<PsiReference> {
    val project = project();
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

fun <T : PsiElement> instances(cls: PsiClassRef<T>, scope: GlobalSearchScope = KDataHolder.scope!!): Stream<T> {
    return nodes(scope).withKind(cls)
}

fun <T> List<T>.stream() = J8Util.stream(this);
fun <T> Stream<T>.toList() : List<T> = collect(Collectors.toList<T>())

fun nodes(scope: GlobalSearchScope = KDataHolder.scope!!): Stream<PsiElement> {
    val project = project()
    return project!!.packages().stream()
            .flatMap { it.roots(scope).stream() }
            .flatMap { it.descendants() }.filter { it !is PsiWhiteSpace }
}

fun PsiElement.descendants(): Stream<PsiElement> {
    return Stream.concat(Stream.of(this), this.children.toList().stream()
            .flatMap { it.descendants() })
}

class Refactoring<T : PsiElement>(elements: List<T>, refactoring: (T) -> Unit) {
    val elements: List<T> = elements
    val refactoring: (T) -> Unit = refactoring
}
fun <T : PsiElement> List<T>.refactor(refactoring: (T) -> Unit) {
    show(Refactoring(this, refactoring))
}

fun <T : PsiElement> Stream<T>.refactor(refactoring: (T) -> Unit) = toList().refactor(refactoring)

//------------ project structure navigation

//todo make for-internal-use
fun context(): PsiElement? {
    val editor = editor() ?: return null
    return editor.inputPsiFile;
}

fun project(): Project = KDataHolder.project!!

private fun editor(): KEditor? = KDataHolder.editor

fun Project.modules(): List<Module> {
    return ModuleManager.getInstance(this).modules.filterNotNull().toList();
}

fun PsiPackage.roots(scope: GlobalSearchScope = KDataHolder.scope!!): List<PsiFile> {
    val files = this.getFiles(scope);
    if (files == null) return emptyList();
    return files.requireNoNulls().toList();
}

val help = {KHelpResult("I'm the help of your dream");}

//------------ util

fun <T : PsiNamedElement> List<T>.withName(name: String): List<T> = this.filter { it.name == name };

fun <T : PsiNamedElement> List<T>.oneWithName(name: String): T? = this.withName(name).firstOrNull();

fun <T : PsiElement> List<PsiElement>.withKind(k: PsiClassRef<T>): List<T> {
    return this.filterIsInstance(k.myRef);
}

fun <T : PsiElement> Stream<PsiElement>.withKind(k: PsiClassRef<T>): Stream<T> {
    return this.filter { k.myRef.isAssignableFrom(it.javaClass) }.map { it as T };
}

fun show(r: Stream<Any?>) {
    show(r.toArray().toList())
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

fun <T:PsiElement> show(refactoring: Refactoring<T>){
    if (refactoring.elements.isNotEmpty()) {
        val result = KUsagesResult(refactoring.elements, "", editor(), refactoring.refactoring)
        result.openUsagesView()
        return show(result)
    }
    show(EMPTY_SEQ)
}

fun show(e: List<Any?>) {
    if (e.isNotEmpty()) {
        if (e.all { it is PsiElement }) {
            return show(KUsagesResult(e.filterIsInstance<PsiElement>(), "", editor()))
        }
        if (e.all { it is PsiReference }) {
            return show(KUsagesResult(e.filterIsInstance<PsiReference>().map { it.element!! }, "", editor()))
        }
        if (e.all { it is KResult }) {
            return e.forEach { show(it as KResult) }
        }
    }
    show(EMPTY_SEQ)
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
    } else if (o is Stream<*>) {
        show(o as Stream<Any?>)
    } else {
        show(o.toString())
    }
}

fun show(u: PsiReference) {
    show(u.element!!)
}

fun <T> T.hasValue(e : T) : Boolean {
    return this == e
}