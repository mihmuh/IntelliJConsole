package com.intellij.idekonsole.scripting

import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.results.KResult
import com.intellij.idekonsole.results.KStdoutResult
import com.intellij.idekonsole.results.usagesResult
import com.intellij.idekonsole.scripting.collections.SequenceLike
import com.intellij.idekonsole.scripting.collections.filterNotNull
import com.intellij.idekonsole.scripting.collections.isNotEmpty
import com.intellij.idekonsole.scripting.collections.map
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

private fun output(): ConsoleOutput = Context.instance().output
private fun project(): Project = Context.instance().project

interface ConsoleOutput {
    fun addResult(result: KResult)
    fun addResultAfter(result: KResult, anchor: KResult)
}

fun show(r: KResult) {
    output().addResult(r)
}

fun show(s: String) {
    show(KStdoutResult(s))
}

fun <T : PsiElement> show(refactoring: Refactoring<T>) {
    if (refactoring.elements.isNotEmpty()) {
        val result = usagesResult(refactoring, "", project(), output())
        result.openUsagesView()
        return show(result)
    }
    show(EMPTY_SEQ)
}

@JvmName("showPsiElement") fun show(vararg e: PsiElement) {
    show(e.toList())
}

@JvmName("showSeqPsiElement") fun show(s: Sequence<PsiElement>) {
    show(usagesResult(s, "", project(), output()))
}

@JvmName("showCocoSeqPsiElement") fun show(s: SequenceLike<PsiElement>) {
    show(usagesResult(s, "", project(), output()))
}

@JvmName("showListPsiElement") fun show(l: List<PsiElement>) {
    show(l.asSequence())
}

@JvmName("showSeqPsiElement") inline fun show(f: () -> Sequence<PsiElement>) {
    show(f.invoke())
}

@JvmName("showCocoSeqPsiElement") fun show(f: () -> SequenceLike<PsiElement>) {
    show(f.invoke())
}

@JvmName("showListPsiElement") inline fun show(f: () -> List<PsiElement>) {
    show(f.invoke())
}

@JvmName("showPsiReference") fun show(vararg e: PsiReference) {
    show(e.toList())
}

@JvmName("showSeqPsiReference") fun show(s: Sequence<PsiReference>) {
    return show(usagesResult(s.map { it.element }.filterNotNull(), "", project(), output()))
}

@JvmName("showCocoSeqPsiReference") fun show(s: SequenceLike<PsiReference>) {
    return show(usagesResult(s.map { it.element }.filterNotNull(), "", project(), output()))
}

@JvmName("showListPsiReference") fun show(l: List<PsiReference>) {
    show(l.asSequence())
}

@JvmName("showSeqPsiReference") inline fun show(f: () -> Sequence<PsiReference>) {
    show(f.invoke())
}

@JvmName("showCocoSeqPsiReference") inline fun show(f: () -> SequenceLike<PsiReference>) {
    show(f.invoke())
}

@JvmName("showListPsiReference") inline fun show(f: () -> List<PsiReference>) {
    show(f.invoke())
}

val EMPTY_SEQ = "Empty sequence"

fun show(f: () -> Any?) {
    show(f.invoke())
}

fun show(o: Any?) {
    if (o == null) {
        show("null")
    } else if (o is Unit) {
        //do nothing
    } else {
        show(o.toString())
    }
}