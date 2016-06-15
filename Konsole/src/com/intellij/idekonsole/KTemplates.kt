package com.intellij.idekonsole

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.searches.ClassInheritorsSearch

object KTemplates {
    private val COMMAND = "{COMMAND}"
    private val MAGIC = "/*MAGIC*/"

    private val CONSOLE_CONTENT =
            """
            package com.intellij.idekonsole.runtime;

            import com.intellij.idekonsole.runtime.*
            import com.intellij.idekonsole.context.aliases.*
            import com.intellij.idekonsole.scripting.*
            import com.intellij.idekonsole.scripting.java.*
            import org.jetbrains.kotlin.j2k.*

            fun main_exec(){
            val function = {
                ${COMMAND}
            }
            show (function)
            }""".trimIndent()


    val consoleContent = CONSOLE_CONTENT.replace(COMMAND, MAGIC + "      " + MAGIC)
    fun getConsoleCaretOffset(text: String) = text.indexOf(MAGIC) + MAGIC.length;
    fun getConsoleFolding1End(text: String) = getConsoleCaretOffset(text)
    fun getConsoleFolding2Start(text: String) = text.lastIndexOf(MAGIC)

    fun getConsoleBlocks(text: String): List<Int> {
        return listOf(getConsoleFolding1End(text), getConsoleFolding2Start(text))
    }

    fun initHelperClasses(module: Module) {
        val psiStubsFile = KIdeaModuleBuilder.createKtClass(module, KSettings.SRC_DIR + KSettings.PSI_STUBS)
        val javaTokensFile = KIdeaModuleBuilder.createKtClass(module, KSettings.SRC_DIR + KSettings.JAVA_TOKEN_STUBS)
        module.project.save()
        DumbService.getInstance(module.project).runWhenSmart {
            FileUtil.writeToFile(VfsUtil.virtualToIoFile(psiStubsFile), generatePsiClassReferences(module).toString())
            FileUtil.writeToFile(VfsUtil.virtualToIoFile(javaTokensFile), generateJavaTokenReferences(module).toString())
        }
    }

    private fun generatePsiClassReferences(module: Module): CharSequence {
        val sb = StringBuilder()
        sb.append("package com.intellij.idekonsole.runtime;\n")
        sb.append("\n")
        sb.append("import com.intellij.idekonsole.scripting.PsiClassRef;\n")
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
        sb.append("import com.intellij.psi.JavaTokenType;\n")
        sb.append("\n")

        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        val element = JavaPsiFacade.getInstance(module.project).findClass("com.intellij.psi.JavaTokenType", scope)!!
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