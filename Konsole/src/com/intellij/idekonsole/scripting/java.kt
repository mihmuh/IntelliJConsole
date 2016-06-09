package com.intellij.idekonsole.scripting

import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.ModulesScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.SearchScope
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import java.util.*

fun classes(name: String): List<PsiClass?> {
    val p = project()
    val sn = PsiNameHelper.getShortClassName(name)
    val candidates = PsiShortNamesCache.getInstance(project()).getClassesByName(sn, EverythingGlobalScope(p))
    return candidates.filter {
        val qname = it.qualifiedName
        qname != null && qname.endsWith(name)
    }
}

fun cls(name: String): PsiClass? = classes(name).filterNotNull().firstOrNull()

fun methods(classAndMethod: String): List<PsiMethod> {
    val i = classAndMethod.lastIndexOf(".")
    assert(i != -1);
    val className = classAndMethod.substring(0, i)
    val methodName = classAndMethod.substring(i + 1)
    val m = classes(className).filterNotNull().flatMap { it.findMethodsByName(methodName, false).toList() };
    return m.toList();
}

fun meth(classAndMethod: String): PsiMethod? {
    return methods(classAndMethod).first()
}

fun Project.topPackages(): List<PsiPackage> {
    return this.modules().map { it.topPackage() }.filterNotNull()
}

fun Project.packages(): List<PsiPackage> {
    return this.topPackages().flatMap { it.allSubpackages(ModulesScope(this.modules().toSet(), this)) }
}

fun Module.topPackage(): PsiPackage? {
    val sourceRoots = ModuleRootManager.getInstance(this).getSourceRoots(JavaModuleSourceRootTypes.SOURCES)
    for (sourceRoot in sourceRoots) {
        val directory = PsiManager.getInstance(project).findDirectory(sourceRoot)
        if (directory != null) {
            return JavaDirectoryService.getInstance().getPackage(directory)
        }
    }
    return null;
}

fun Module.packages(): List<PsiPackage> {
    val tp = this.topPackage()
    if (tp == null) return emptyList();
    return tp.allSubpackages(ModulesScope(Collections.singleton(this), this.project));
}

private fun PsiPackage.allSubpackages(s: GlobalSearchScope): List<PsiPackage> {
    val result = ArrayList<PsiPackage>()
    addSubpackages(result, this, s);
    return result;
}

private fun addSubpackages(result: ArrayList<PsiPackage>, psiPackage: PsiPackage, s: GlobalSearchScope) {
    result.add(psiPackage);
    for (p in psiPackage.getSubPackages(s)) {
        addSubpackages(result, p, s)
    }
}

private fun parserFacade() = JavaPsiFacade.getInstance(project()).parserFacade

fun String.asClass(): PsiClass = parserFacade().createClassFromText(this, context());

fun String.asStatement(): PsiStatement = parserFacade().createStatementFromText(this, context());

fun String.asExpression(): PsiExpression = parserFacade().createExpressionFromText(this, context());

/*
* .asPsi(experimantal)
* .isSubtype, .type, type//
* */