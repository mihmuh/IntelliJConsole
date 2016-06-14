package com.intellij.idekonsole.context

import com.intellij.idekonsole.scripting.ConsoleOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

// FIXME: we can load separate instances of this class in different MegaLoader's -
// and initialize its fields via reflection before executing the payload
// but for now - just share same instances, expecting that no one will launch multiple commands simultaneously

class Context(val project: Project, var scope: GlobalSearchScope = GlobalSearchScope.projectScope(project), val output: ConsoleOutput) {

    companion object {
        private var instance: Context? = null
        fun instance(): Context {
            if (instance == null) {
                throw UnsupportedOperationException("No console context available. It may be caused by using invokeLater() in console")
            }
            return instance!!
        }
        fun wrapCallback(f: () -> Unit): () -> Unit {
            val context = instance()
            return {
                context.execute(f)
            }
        }
    }

    fun execute(f: () -> Unit) {
        ApplicationManager.getApplication().assertReadAccessAllowed()
        instance = this
        try {
            f.invoke()
        } finally {
            instance = null
        }
    }

}

fun runReadAndWait(context: Context, f: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait({
        ApplicationManager.getApplication().runReadAction {
            context.execute(f)
        }
    }, ModalityState.NON_MODAL)
}

fun runReadLater(context: Context, f: () -> Unit) {
    ApplicationManager.getApplication().invokeLater {
        ApplicationManager.getApplication().runReadAction {
            context.execute(f)
        }
    }
}

fun runRead(context: Context, f: () -> Unit) {
    ApplicationManager.getApplication().runReadAction {
        context.execute(f)
    }
}

fun runWriteLater(context: Context, f: () -> Unit) {
    ApplicationManager.getApplication().invokeLater {
        ApplicationManager.getApplication().runReadAction {
            context.execute(f)
        }
    }
}