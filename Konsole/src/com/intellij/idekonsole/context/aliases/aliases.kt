package com.intellij.idekonsole.context.aliases

import com.intellij.idekonsole.context.Context
import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.openapi.project.Project
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope

@Suppress("UNUSED")
val context: Context
    get() = Context.instance()
@Suppress("UNUSED")
val project: Project
    get() = Context.instance().project
@Suppress("UNUSED")
val output: ConsoleOutput
    get() = Context.instance().output
@Suppress("UNUSED")
var scope: GlobalSearchScope
    get() = Context.instance().scope
    set(value) {
        Context.instance().scope = value
    }

@Suppress("UNUSED")
fun globalScope(): GlobalSearchScope = EverythingGlobalScope.allScope(project)
@Suppress("UNUSED")
fun projectScope(): GlobalSearchScope = EverythingGlobalScope.projectScope(project)

