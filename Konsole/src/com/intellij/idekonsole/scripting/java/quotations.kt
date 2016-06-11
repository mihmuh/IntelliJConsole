package com.intellij.idekonsole.scripting.java

import com.intellij.idekonsole.scripting.project
import com.intellij.psi.*

private val parserFacade = JavaPsiFacade.getInstance(project).parserFacade

private fun PsiElement.brokenReferences(): Sequence<String> {
    val myBroken = this.references.asSequence().filter { it.resolve() == null }.map { it.canonicalText }
    return myBroken.plus(children.asSequence().flatMap { it.brokenReferences() })
}

class ParsePsiException(message: String) : RuntimeException(message)

private fun <T : PsiElement> T.assertValid(): T {
    val brokenReferences = this.brokenReferences().toList()
    if (brokenReferences.isNotEmpty()) {
        val first = brokenReferences.first()
        throw ParsePsiException("Could not resolve reference `$first`")
    }
    return this
}

fun String.asClass(context: PsiElement? = null): PsiClass = parserFacade.createClassFromText(this, context).assertValid();

fun String.asStatement(context: PsiElement? = null): PsiStatement = parserFacade.createStatementFromText(this, context).assertValid();

fun String.asTypeElement(context: PsiElement? = null): PsiTypeElement = parserFacade.createTypeElementFromText(this, context).assertValid();

fun String.asType(context: PsiElement? = null): PsiType = asTypeElement(context).type;

fun String.asExpression(context: PsiElement? = null): PsiExpression = parserFacade.createExpressionFromText(this, context).assertValid();

fun PsiExpression.hasType(type: String): Boolean {
    try {
        val thisType = this.type
        if (thisType != null) {
            return type.asType(this).isAssignableFrom(thisType)
        }
        return false;
    } catch (e: ParsePsiException) {
        return false
    }
}

fun PsiExpression.replaceWithExpression(newNode: String) {
    this.replace(newNode.asExpression(this))
}

//todo: asPsi

