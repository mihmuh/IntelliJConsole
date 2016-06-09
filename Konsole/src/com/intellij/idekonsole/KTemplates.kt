package com.intellij.idekonsole

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.searches.ClassInheritorsSearch

object KTemplates {
    private val CARET = "{CARET}"
    private val FOLDING_START = "{FOLDING_START}"
    private val FOLDING_END = "{FOLDING_END}"

    private val CONSOLE_CONTENT =
            """
            package com.intellij.idekonsole.runtime;

            import com.intellij.idekonsole.results.*
            import com.intellij.idekonsole.runtime.*

            ${FOLDING_START}fun main_exec(): KResult {
                ${CARET}return KStdoutResult("Hello World")
            }${FOLDING_END}

            """.trimIndent()


    fun getConsoleContent() = CONSOLE_CONTENT.replace(CARET, "").replace(FOLDING_START, "").replace(FOLDING_END, "")
    fun getConsoleCaretOffset() = CONSOLE_CONTENT.replace(FOLDING_START, "").replace(FOLDING_END, "").indexOf(CARET)
    fun getConsoleFoldingStart() = CONSOLE_CONTENT.replace(CARET, "").replace(FOLDING_END, "").indexOf(FOLDING_START)
    fun getConsoleFoldingEnd() = CONSOLE_CONTENT.replace(CARET, "").replace(FOLDING_START, "").indexOf(FOLDING_END)

    fun initHelperClasses(module: Module) {
        val referenceFile = KIdeaModuleBuilder.createKtClass(module, KSettings.SRC_DIR + "PsiClassReferences.kt")
        if (referenceFile.length == 0L) {
            DumbService.getInstance(module.project).runWhenSmart {
                val content = generatePsiClassReferences(module)
                val ioFile = VfsUtil.virtualToIoFile(referenceFile)
                FileUtil.writeToFile(ioFile, content.toString())
            }
        }
    }

    private fun generatePsiClassReferences(module: Module): CharSequence {
        val sb = StringBuilder()
        sb.append("package com.intellij.idekonsole.runtime;\n")
        sb.append("\n")
        sb.append("import com.intellij.idekonsole.scripting.PsiClassRef\n")
        sb.append("\n")

        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        val element = JavaPsiFacade.getInstance(module.project).findClass("com.intellij.psi.PsiElement", scope)!!
        val elements = ClassInheritorsSearch.search(element, scope, true)

        val set = elements.findAll().map { it.qualifiedName!! }.toSet()
        for (qualifiedName in set) {
            createPsiClassRef(sb, qualifiedName)
        }

        return sb
    }

    private fun createPsiClassRef(sb: StringBuilder, qualifiedName: String) {
        if (qualifiedName.endsWith("Impl")) return
        if (!qualifiedName.startsWith("com.intellij.psi.")) return

        val className = qualifiedName.removePrefix("com.intellij.psi.")
        if (className.contains(".")) return
        val varName = "`$$className`"

        sb.append("val $varName = PsiClassRef($qualifiedName::class.java)\n")
    }
}