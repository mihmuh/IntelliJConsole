package com.intellij.idekonsole

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

@State(name = "IDEKonsole", storages = arrayOf(Storage("/ide_konsole.xml")))
internal class KSettings : PersistentStateComponent<KSettings.Data> {
    companion object {
        val instance: KSettings
            get() = ServiceManager.getService(KSettings::class.java)

        val PLACEHOLDER = "{PLACEHOLDER}"

        // FIXME
        val INITIAL_CONTENT = "package konsole;\nfun foo() {}\n\nfun main() {\n{PLACEHOLDER}\n}\n"
    }

    var data = Data()

    override fun loadState(value: Data) {
        data = value
    }

    override fun getState(): Data = data

    fun getModule(project: Project): Module? {
        // FIXME
        return ModuleManager.getInstance(project).sortedModules.firstOrNull()
    }

    class Data {
    }
}