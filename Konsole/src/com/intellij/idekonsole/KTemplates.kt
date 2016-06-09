package com.intellij.idekonsole

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.searches.ClassInheritorsSearch

object KTemplates {
    private val CARET = "{CARET}"
    private val BLOCK = "{BLOCK}"
    private val NOFOLD_START = "{FOLDING_START}"
    private val NOFOLD_END = "{FOLDING_END}"

    private val CONSOLE_CONTENT =
            """
            package com.intellij.idekonsole.runtime;

            import com.intellij.idekonsole.results.*
            import com.intellij.idekonsole.runtime.*
            import com.intellij.idekonsole.scripting.*

            fun main_exec(){
            show({
                ${BLOCK}
                ${NOFOLD_START}${CARET}${NOFOLD_END}
                ${BLOCK}
            })
            }

            """.trimIndent()


    fun getConsoleContent() = CONSOLE_CONTENT.replace(BLOCK, "").replace(CARET, "").replace(NOFOLD_START, "").replace(NOFOLD_END, "")
    fun getConsoleCaretOffset() = CONSOLE_CONTENT.replace(BLOCK, "").replace(NOFOLD_START, "").replace(NOFOLD_END, "").indexOf(CARET)
    fun getConsoleFoldingStart() = CONSOLE_CONTENT.replace(BLOCK, "").replace(CARET, "").replace(NOFOLD_END, "").indexOf(NOFOLD_START)
    fun getConsoleFoldingEnd() = CONSOLE_CONTENT.replace(BLOCK, "").replace(CARET, "").replace(NOFOLD_START, "").indexOf(NOFOLD_END)
    fun getConsoleBlocks():List<Int> {
        val onlyBlock = CONSOLE_CONTENT.replace(CARET, "").replace(NOFOLD_END, "").replace(NOFOLD_START, "")
        val b1 = onlyBlock.indexOf(BLOCK)
        val b2 = onlyBlock.replaceFirst(BLOCK, "").indexOf(BLOCK)
        return listOf(b1,b2)
    }

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

    private fun generateJavaTokenReferences(module: Module): CharSequence {
        val sb = StringBuilder()
        sb.append("package com.intellij.idekonsole.runtime;\n")
        sb.append("\n")
        sb.append("import com.intellij.psi.JavaTokenType\n")
        sb.append("\n")

        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        val element = JavaPsiFacade.getInstance(module.project).findClass("import com.intellij.psi.JavaTokenType", scope)!!
        val fields = element.fields

        for (field in fields) {
            createJavaTokenRef(sb, field.name!!)
        }

        return sb
    }

    private fun createPsiClassRef(sb: StringBuilder, qualifiedName: String) {
        if (qualifiedName.endsWith("Impl")) return
        if (!qualifiedName.startsWith("com.intellij.psi.")) return

        val className = qualifiedName.removePrefix("com.intellij.psi.")
        if (className.contains(".")) return

        sb.append("val $className = PsiClassRef($qualifiedName::class.java)\n")
    }

    private fun createJavaTokenRef(sb: StringBuilder, name: String) {
        sb.append("val $name = JavaTokenType.$name\n")
    }
}