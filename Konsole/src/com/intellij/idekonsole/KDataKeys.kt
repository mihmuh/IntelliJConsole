package com.intellij.idekonsole

import com.intellij.openapi.actionSystem.DataKey

object KDataKeys {
    val K_TOOL_WINDOW: DataKey<KToolWindow> = DataKey.create("K_TOOL_WINDOW")
    val K_EDITOR: DataKey<KEditor> = DataKey.create("K_EDITOR")
}