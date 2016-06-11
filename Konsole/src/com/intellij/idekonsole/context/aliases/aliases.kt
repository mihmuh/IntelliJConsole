package com.intellij.idekonsole.context.aliases

import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.openapi.project.Project
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope

val context: Context
    get() = Context.instance()
val project: Project
    get() = Context.instance().project
val output: ConsoleOutput
    get() = Context.instance().output
var scope: GlobalSearchScope
    get() = Context.instance().scope
    set(value) {
        Context.instance().scope = value
    }

fun globalScope(): GlobalSearchScope = EverythingGlobalScope.allScope(project)

fun projectScope(): GlobalSearchScope = EverythingGlobalScope.projectScope(project)

