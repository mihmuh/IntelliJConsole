package com.intellij.idekonsole.scripting

import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.ModulesScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import java.util.*

//todo fun classes
//todo fun method

fun cls(name: String): PsiClass? {
    //todo filter classes by name end, not by fq class name, e.g. findClass(b.c) can return "a.b.c"
    val p = project()
    if (p == null) return null;
    return JavaPsiFacade.getInstance(p).findClass(name, GlobalSearchScope.allScope(p));
}

fun methods(classAndMethod: String): List<PsiMethod> {
    //todo
    val className = classAndMethod.substringBeforeLast(".", "DefaultClass")
    val methodName = classAndMethod.substringAfterLast(".", "DefaultMethod")
    val m = cls(className)?.findMethodsByName(methodName, false);
    return if (m == null) Collections.emptyList() else m.toList();
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