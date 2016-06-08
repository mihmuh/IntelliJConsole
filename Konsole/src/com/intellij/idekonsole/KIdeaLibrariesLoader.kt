package com.intellij.idekonsole

import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.debugger.getClassName
import java.io.File

/**
 * @author simon
 */
class KIdeaLibrariesLoader :ProjectComponent {
    private val LIB_NAME = "IDEA_LIB"
    val myProject:Project

    constructor(p:Project) {
        myProject = p
    }

    override fun getComponentName(): String {
        return "KJavaLibrariesLoader"
    }

    override fun disposeComponent() {

    }

    override fun initComponent() {

    }

    override fun projectClosed() {

    }

    override fun projectOpened() {
        /*ApplicationManager.getApplication().runWriteAction {
            val moduleManager = ModuleManager.getInstance(myProject)
            val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject)
            if (libraryTable.getLibraryByName(LIB_NAME) == null) {
                val library = libraryTable.createLibrary(LIB_NAME)
                val libraryModel = library.modifiableModel

                val ideaLibraries = VfsUtil.getUrlForLibraryRoot(File(PathManager.getHomePath()))
                val pluginLibraries = VfsUtil.getUrlForLibraryRoot(File(PathManager.getPreInstalledPluginsPath()))

                libraryModel.addJarDirectory(ideaLibraries, true)
                libraryModel.addJarDirectory(pluginLibraries, true)
                libraryModel.commit()
                moduleManager.modules.forEach {
                    val model = ModuleRootManager.getInstance(it).modifiableModel
                    model.addLibraryEntry(library)
                    model.commit()
                }
            }
        }*/
    }
}