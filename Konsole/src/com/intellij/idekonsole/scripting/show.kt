package com.intellij.idekonsole.scripting

import com.intellij.idekonsole.KDataHolder
import com.intellij.idekonsole.KEditor
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
import com.intellij.idekonsole.results.KUsagesResult
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

private fun editor(): KEditor? = KDataHolder.editor

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
        show(o)
    } else {
        show(o.toString())
    }
}

fun show(u: PsiReference) {
    show(u.element!!)
}