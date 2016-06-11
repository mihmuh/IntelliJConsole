package com.intellij.idekonsole.scripting

import com.intellij.idekonsole.KDataHolder
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

fun cls(name: String): PsiClass? = classes(name).firstOrNull()

fun methods(classAndMethod: String): Sequence<PsiMethod> {
    val i = classAndMethod.lastIndexOf(".")
    if (i == -1) throw IllegalArgumentException("Could not parse method name: `$classAndMethod`. Expected format: `Class.method`")
    val className = classAndMethod.substring(0, i)
    val methodName = classAndMethod.substring(i + 1)
    return classes(className).flatMap { it.findMethodsByName(methodName, false).toList().asSequence() }
}

fun meth(classAndMethod: String): PsiMethod? = methods(classAndMethod).firstOrNull()

fun Project.topPackages(): List<PsiPackage> {
    return this.modules().flatMap { it.topPackages() }.distinct()
}

fun Project.packages(s: GlobalSearchScope = scope): List<PsiPackage> {
    return topPackages().flatMap { it.allSubpackages(s).toList() }
}

fun Module.topPackages(): List<PsiPackage> {
    return ModuleRootManager.getInstance(this).getSourceRoots(JavaModuleSourceRootTypes.SOURCES)
            .map { PsiManager.getInstance(project).findDirectory(it) }.filterNotNull()
            .map { JavaDirectoryService.getInstance().getPackage(it) }.filterNotNull()
}

fun Module.packages(): List<PsiPackage> {
    return topPackages().flatMap { it.allSubpackages(ModulesScope(Collections.singleton(this), this.project)).toList() }
}

private fun PsiPackage.allSubpackages(s: GlobalSearchScope): Sequence<PsiPackage> {
    return deepSearch(this, {p -> p.getSubPackages(s).asSequence()})
}

private val parserFacade = JavaPsiFacade.getInstance(project).parserFacade

fun String.asClass(): PsiClass = parserFacade.createClassFromText(this, null).assertValid();

fun String.asStatement(): PsiStatement = parserFacade.createStatementFromText(this, null).assertValid();

fun String.asTypeElement(context: PsiElement?): PsiTypeElement = parserFacade.createTypeElementFromText(this, context).assertValid();

fun String.asType(context: PsiElement?): PsiType = asTypeElement(context).type;

fun PsiElement.brokenReferences(): Sequence<String> {
    val myBroken = this.references.asSequence().filter { it.resolve() == null }.map { it.canonicalText }
    return myBroken.plus(children.asSequence().flatMap { it.brokenReferences() })
}

class ParsePsiException(message: String) : RuntimeException(message)

fun <T : PsiElement> T.assertValid(): T {
    val brokenReferences = this.brokenReferences().toList()
    if (brokenReferences.isNotEmpty()) {
        val first = brokenReferences.first()
        throw ParsePsiException("Could not resolve reference `$first`")
    }
    return this
}

fun String.asType(): PsiType = asType(null);

fun String.asExpression(): PsiExpression = asExpression(null);

fun String.asExpression(context: PsiElement?): PsiExpression = parserFacade.createExpressionFromText(this, context);

fun PsiClass.inheritors(): List<PsiClass> {
    return ClassInheritorsSearch.search(this).toList();
}

fun PsiExpression.hasType(type: String): Boolean {
    try {
        val thisType = this.type
        if (thisType != null) {
            return type.asType(this).isAssignableFrom(thisType)
        }
        return false;
    } catch(e: ParsePsiException) {
        return false
    }
}

fun PsiExpression.replaceWithExpression(newNode: String) {
    this.replace(newNode.asExpression(this))
}

/*
* .asPsi(experimantal)
* */