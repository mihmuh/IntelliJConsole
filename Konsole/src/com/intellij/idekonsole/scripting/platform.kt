package com.intellij.idekonsole.scripting

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.results.KHelpResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.ArrayUtil
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

//----------- find, refactor

private fun defaultScope(): GlobalSearchScope = Context.instance().scope
private fun defaultProject(): Project = Context.instance().project

private fun <T> concurrentPipe(project: Project, handler: (Processor<T>) -> Unit): Sequence<T> {
    val buffer = ContainerUtil.createConcurrentList<T>()
    val lock = Object()
    var finished: Boolean = false
    val waiting = AtomicInteger(0)

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Searching") {
        override fun run(indicator: ProgressIndicator) {
            handler.invoke(Processor<T> {
                buffer.add(it)
                if (waiting.get() > 0) {
                    synchronized(lock) {
                        if (waiting.get() > 0) {
                            lock.notifyAll()
                        }
                    }
                }
                true
            })
            finished = true
            if (waiting.get() > 0) {
                synchronized(lock) {
                    if (waiting.get() > 0) {
                        lock.notifyAll()
                    }
                }
            }
        }
    })
    return object : Sequence<T> {
        override fun iterator(): Iterator<T> {
            var nextIndex: Int = 0
            return object : Iterator<T> {
                override fun hasNext(): Boolean {
                    assert(nextIndex <= buffer.size)
                    if (nextIndex < buffer.size) {
                        return true
                    } else if (finished) {
                        return false
                    }
                    synchronized(lock) {
                        waiting.incrementAndGet()
                        while (nextIndex == buffer.size && !finished) {
                            lock.wait()
                        }
                        waiting.decrementAndGet()
                    }
                    return nextIndex < buffer.size
                }

                override fun next(): T {
                    if (!hasNext()) {
                        throw NoSuchElementException()
                    }
                    return buffer[nextIndex++]
                }
            }
        }
    }
}

//non concurrent version
private fun <T> pipeByList(handler: (Processor<T>) -> Unit): Sequence<T> {
    val buffer = ArrayList<T>()
    handler(Processor {
        buffer.add(it)
    })
    return buffer.asSequence()
}

fun usages(node: PsiElement, scope: GlobalSearchScope = defaultScope(), project: Project = defaultProject()): Sequence<PsiReference> {
    val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(node, false)!!
    val psiElements = ArrayUtil.mergeArrays(handler.primaryElements, handler.secondaryElements)
    val options = handler.getFindUsagesOptions(null)
    options.searchScope = scope
    return psiElements.asSequence()
            .flatMap { psiElement -> concurrentPipe<UsageInfo?>(project) { handler.processElementUsages(psiElement, it, options) } }
            .map { it?.reference }
            .filterNotNull()
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