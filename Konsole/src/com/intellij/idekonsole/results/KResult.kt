package com.intellij.idekonsole.results

import javax.swing.JComponent

interface KResult {
    fun getPresentation(): JComponent;
}
