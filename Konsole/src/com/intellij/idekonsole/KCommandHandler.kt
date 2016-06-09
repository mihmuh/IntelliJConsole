package com.intellij.idekonsole

import com.intellij.idekonsole.results.KResult
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.AsyncResult
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.util.ExceptionUtil
import java.net.URL
import java.net.URLClassLoader

object KCommandHandler {
    private val LOG = Logger.getInstance(KResult::class.java)

    fun compile(module: Module, editor: KEditor): AsyncResult<Computable<Any?>> {
        val future = AsyncResult<Computable<Any?>>()

        CompilerManager.getInstance(module.project).compile(module, { aborted, errors, warnings, compileContext ->
            val outputPath = CompilerPaths.getModuleOutputPath(module, false)
            val url = URL("file://$outputPath/")
            val classloader = URLClassLoader(arrayOf(url), AllClassesClassLoader(VirtualFileSystem::class.java.classLoader))
            val clazz = classloader.loadClass("com.intellij.idekonsole.runtime.TestKt");

            KDataHolder.project = module.project
            KDataHolder.editor = editor
            try {
                val m = clazz.getMethod("main_exec")

                future.setDone(Computable {
                    val res = m.invoke(null)
                    return@Computable res
                })
            } catch(e: Exception) {
                future.setDone(Computable<Any?> {
                    ExceptionUtil.rethrowAllAsUnchecked(e)
                    return@Computable null
                })
            }
        })

        return future
    }
}
