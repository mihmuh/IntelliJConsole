package com.intellij.idekonsole

import com.intellij.idekonsole.results.KResult
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.AsyncResult
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ExceptionUtil
import java.net.URL

object KCommandHandler {
    private val LOG = Logger.getInstance(KResult::class.java)

    fun compile(module: Module, file: VirtualFile): AsyncResult<Computable<KResult>> {
        val future = AsyncResult<Computable<KResult>>()

        CompilerManager.getInstance(module.project).compile(module, { aborted, errors, warnings, compileContext ->
            val outputPath = CompilerPaths.getModuleOutputPath(module, false)
            val url = URL("file://$outputPath/")
            val classloader = MegaLoader(url)
            val clazz = classloader.loadClass("konsole.runtime.TestKt");

            try {
                val m = clazz.getMethod("main_exec")

                future.setDone(Computable {
                    val res = m.invoke(null)
                    return@Computable res as KResult
                })
            } catch(e: Exception) {
                future.setDone(Computable<KResult> {
                    ExceptionUtil.rethrowAllAsUnchecked(e)
                    return@Computable null
                })
            }
        })

        return future
    }
}
