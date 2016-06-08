package com.intellij.ideconsole

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
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
    val className = classAndMethod.substringBeforeLast(".", "DefaultClass")
    val methodName = classAndMethod.substringAfterLast(".", "DefaultMethod")
    val m = cls(className)?.findMethodsByName(methodName, false);
    return if (m == null) Collections.emptyList() else m.toList();
}

private fun parserFacade() = JavaPsiFacade.getInstance(project()).parserFacade

fun String.asClass(): PsiClass = parserFacade().createClassFromText(this, context());

fun String.asStatement(): PsiStatement = parserFacade().createStatementFromText(this, context());

fun String.asExpression(): PsiExpression = parserFacade().createExpressionFromText(this, context());

/*
* .asPsi(experimantal)
* .isSubtype, .type, type//
* */