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
import java.util.stream.Stream

fun classes(name: String, scope: GlobalSearchScope = KDataHolder.scope!!): List<PsiClass> {
    val sn = PsiNameHelper.getShortClassName(name)
    val candidates = PsiShortNamesCache.getInstance(project).getClassesByName(sn, scope)
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
    return this.modules().map { it.topPackage() }.filterNotNull().distinct()
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

private val parserFacade = JavaPsiFacade.getInstance(project).parserFacade

fun String.asClass(): PsiClass = parserFacade.createClassFromText(this, null).assertValid();

fun String.asStatement(): PsiStatement = parserFacade.createStatementFromText(this, null).assertValid();

fun String.asTypeElement(context : PsiElement?): PsiTypeElement = parserFacade.createTypeElementFromText(this, context).assertValid();

fun String.asType(context : PsiElement?): PsiType = asTypeElement(context).type;

fun PsiElement.brokenReferences() : Stream<String> {
    val myBroken = this.references.asList().stream().filter { it.resolve() == null }.map { it.canonicalText }
    return Stream.concat(myBroken, children.asList().stream().flatMap { it.brokenReferences() })
}

class ParsePsiException(message: String) : RuntimeException(message)

fun <T : PsiElement> T.assertValid() : T {
    val brokenReferences = this.brokenReferences().toList()
    if (brokenReferences.isNotEmpty()) {
        val first = brokenReferences.first()
        throw ParsePsiException("Could not resolve reference `$first`")
    }
    return this
}

fun String.asType(): PsiType = asType(null);

fun String.asExpression(): PsiExpression = asExpression(null);

fun String.asExpression(context : PsiElement?): PsiExpression = parserFacade.createExpressionFromText(this, context);

fun PsiClass.inheritors(): List<PsiClass>{
    return ClassInheritorsSearch.search(this).toList();
}

fun PsiExpression.hasType(type : String) : Boolean {
    try {
        val thisType = this.type
        if (thisType != null) {
            return type.asType(this).isAssignableFrom(thisType)
        }
        return false;
    } catch(e:ParsePsiException) {
        return false
    }
}

fun PsiExpression.replaceWithExpression(newNode: String) {
    this.replace(newNode.asExpression(this))
}

/*
* .asPsi(experimantal)
* */