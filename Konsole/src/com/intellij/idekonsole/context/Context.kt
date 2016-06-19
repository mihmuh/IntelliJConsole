package com.intellij.idekonsole.context

import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.*

// FIXME: we can load separate instances of this class in different MegaLoader's -
// and initialize its fields via reflection before executing the payload
// but for now - assuming that everywhere context is required it was set explicitly

class Context(val project: Project, var scope: GlobalSearchScope = GlobalSearchScope.projectScope(project), val output: ConsoleOutput) {

    companion object {
        private var instance: ThreadLocal<LinkedList<Context?>> = ThreadLocal.withInitial { LinkedList<Context?>() }
        fun instance(): Context {
            if (instance.get().isEmpty()) {
                throw UnsupportedOperationException("No console context available. It may be caused by using invokeLater() in console")
            }
            return instance.get().first!!
        }
        fun wrapCallback(f: () -> Unit): () -> Unit {
            val context = instance()
            return {
                context.execute(f)
            }
        }
    }

    fun execute(f: () -> Unit) {
        instance.get().addFirst(this)
        try {
            f.invoke()
        } finally {
            instance.get().removeFirst()
        }
    }

}

fun runRead(context: Context, f: () -> Unit) {
    ApplicationManager.getApplication().runReadAction {
        context.execute(f)
    }
}