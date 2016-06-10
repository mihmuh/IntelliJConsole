package com.intellij.idekonsole.scripting

/**
 * Created by Mihail.Buryakov on 6/9/2016.
 */

/*

instances(PsiBinaryExpression).filter { !(it.lOperand.isNullLiteral() || it.rOperand!!.isNullLiteral())&&it.operationTokenType.hasValue(EQEQ) && it.rOperand!!.hasType("String") }
            .refactor { it.replace("${it.lOperand.text}.equals(${it.rOperand!!.text})".asExpression()) }

 */