package com.intellij.ideconsole

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import java.util.*

//----------- find, refactor

fun usages(node: PsiElement): List<PsiReference> {
    return Collections.emptyList();
}

fun instances(cls: Class<PsiElement>): List<PsiElement> {
    return Collections.emptyList();
}

fun <T : PsiElement> List<T>.refactor(refactoring: (T) -> Unit) {
    //todo show in usages view before refactoring
   this.forEach { refactoring(it); }
}

//------------ project structure navigation

fun Project.modules(): List<Module> {
    return Collections.emptyList();
    //todo
}

fun Module.packages(): List<PsiPackage> {
    return Collections.emptyList();
    //todo
}

fun PsiPackage.roots(): List<PsiFile> {
    return Collections.emptyList();
    //todo
}

//------------ util

fun <T : PsiNamedElement> List<T>.withName(name: String): List<T> = this.filter { it.name == name };

fun <T : PsiNamedElement> List<T>.oneWithName(name: String): T? = this.withName(name).firstOrNull();

fun print(s: String) {
  //todo
}

fun internalMode() {
    //todo switch to internal mode
}
