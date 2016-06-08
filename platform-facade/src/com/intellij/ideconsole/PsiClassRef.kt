package com.intellij.ideconsole

import com.intellij.psi.PsiElement

class PsiClassRef <T : PsiElement>(ref: Class<T>) {
    val myRef: Class<T>;

    init {
        myRef = ref;
    }
}