package com.intellij.idekonsole

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.net.URL

/**
 * @author simon
 */
object KIdeaModuleBuilder {
    fun createFile(project: Project, language: Language): VirtualFile {
        val module = createModule(project)
        configureModule(project, module)


        val dir = VfsUtil.virtualToIoFile(getSourceDir(module))
        val file = File(dir, "Test.kt")

        if (!file.exists()) {
            file.createNewFile()
        }

        return VfsUtil.findFileByIoFile(file, true)!!
    }

    private fun getSourceDir(module: Module): VirtualFile {
        val srcPath = VfsUtil.getParentDir(module.moduleFilePath) + "/src"
        return VfsUtil.createDirectories(srcPath)
    }

    fun createModule(project: Project): Module {
        val moduleManager = ModuleManager.getInstance(project)
        val module = moduleManager.findModuleByName(KSettings.MODULE_NAME)
        if (module != null) return module

        return ApplicationManager.getApplication().runWriteAction(Computable {
            val moduleName = KSettings.MODULE_NAME.toLowerCase()
            val modulePath = project.baseDir.path + '/' + moduleName + '/'
            val path = modulePath + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION
            val newModule = moduleManager.newModule(path, ModuleTypeId.JAVA_MODULE)

            return@Computable newModule
        })
    }

    fun configureModule(project: Project, module: Module) {
        ApplicationManager.getApplication().runWriteAction {
            val rootModel = ModuleRootManager.getInstance(module).modifiableModel

            val library = createIdeLibrary(project)

            if (rootModel.findLibraryOrderEntry(library) == null) rootModel.addLibraryEntry(library)

            val sdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(JavaSdk.getInstance())
            if (sdk != null) rootModel.sdk = sdk

            val moduleDir = "file://" + VfsUtil.getParentDir(module.moduleFilePath)!!
            val sourceDir = getSourceDir(module)

            var contentEntry = rootModel.contentEntries.find { it.url == moduleDir }
            if (contentEntry == null) contentEntry = rootModel.addContentEntry(moduleDir)
            contentEntry.addSourceFolder(sourceDir, false)

            rootModel.commit()
        }
    }

    private fun createIdeLibrary(project: Project): Library {
        val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val library = libraryTable.getLibraryByName(KSettings.LIB_NAME)
        if (library != null) return library

        return ApplicationManager.getApplication().runWriteAction (Computable {
            val newLibrary = libraryTable.createLibrary(KSettings.LIB_NAME)
            val libraryModel = newLibrary.modifiableModel

            val ideaLibraries = VfsUtil.getUrlForLibraryRoot(File(PathManager.getHomePath()+"/lib"))
            libraryModel.addJarDirectory(ideaLibraries, true)

            for (p in PluginManager.getPlugins()) {
                val clsUrl = "file://${p.path}/classes"
                val vf = VfsUtil.findFileByURL(URL(clsUrl))
                if (vf != null) {
                    libraryModel.addRoot(clsUrl, OrderRootType.CLASSES)
                }

                val libUrl = "file://${p.path}/lib"
                val vf2 = VfsUtil.findFileByURL(URL(libUrl))
                if (vf2 != null) {
                    libraryModel.addJarDirectory(vf2, true)
                }
            }

            libraryModel.commit()

            return@Computable newLibrary
        })
    }
}
