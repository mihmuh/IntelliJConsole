package com.intellij.idekonsole.scripting.java

import com.intellij.idekonsole.KDataHolder
import com.intellij.idekonsole.scripting.deepSearch
import com.intellij.idekonsole.scripting.modules
import com.intellij.idekonsole.scripting.project
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.ModulesScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import java.util.*

fun classes(name: String, scope: GlobalSearchScope = KDataHolder.scope!!): Sequence<PsiClass> {
    val sn = PsiNameHelper.getShortClassName(name)
    val candidates = PsiShortNamesCache.getInstance(project).getClassesByName(sn, scope)
    return candidates.asSequence().filter {
        it.qualifiedName?.endsWith(name) ?: false
    }
}

fun cls(name: String): PsiClass? =
        classes(name).firstOrNull()

fun methods(classAndMethod: String): Sequence<PsiMethod> {
    val i = classAndMethod.lastIndexOf(".")
    if (i == -1) throw IllegalArgumentException("Could not parse method name: `$classAndMethod`. Expected format: `Class.method`")
    val className = classAndMethod.substring(0, i)
    val methodName = classAndMethod.substring(i + 1)
    return classes(className).flatMap { it.findMethodsByName(methodName, false).toList().asSequence() }
}

fun meth(classAndMethod: String): PsiMethod? =
        methods(classAndMethod).firstOrNull()

private fun PsiPackage.allSubpackages(s: GlobalSearchScope): Sequence<PsiPackage> {
    return deepSearch(this, { p -> p.getSubPackages(s).asSequence() })
}

fun Project.topPackages(): List<PsiPackage> =
        modules().flatMap { it.topPackages() }.distinct()

fun Project.packages(s: GlobalSearchScope = KDataHolder.scope!!): List<PsiPackage> =
        topPackages().flatMap { it.allSubpackages(s).toList() }

fun Module.topPackages(): List<PsiPackage> =
        ModuleRootManager.getInstance(this).getSourceRoots(JavaModuleSourceRootTypes.SOURCES)
                .map { PsiManager.getInstance(project).findDirectory(it) }.filterNotNull()
                .map { JavaDirectoryService.getInstance().getPackage(it) }.filterNotNull()

fun Module.packages(): List<PsiPackage> =
        topPackages().flatMap { it.allSubpackages(ModulesScope(Collections.singleton(this), this.project)).toList() }

fun PsiClass.inheritors(): List<PsiClass> = ClassInheritorsSearch.search(this).toList()
