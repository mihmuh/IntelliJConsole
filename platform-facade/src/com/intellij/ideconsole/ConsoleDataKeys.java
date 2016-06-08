package com.intellij.ideconsole;

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.psi.PsiClass;

public class ConsoleDataKeys {
    public static final DataKey<PsiClass> CONTEXT_CLASS = DataKey.create("console.contextClass");
}
