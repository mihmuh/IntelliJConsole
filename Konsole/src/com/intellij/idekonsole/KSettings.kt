package com.intellij.idekonsole

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "IDEKonsole", storages = arrayOf(Storage("/ide_konsole.xml")))
internal class KSettings : PersistentStateComponent<KSettings.Data> {
    companion object {
        val instance: KSettings
            get() = ServiceManager.getService(KSettings::class.java)

        val LIB_NAME = "KONSOLE_LIB"
        val MODULE_NAME = "KONSOLE_MODULE"

        val PLACEHOLDER = "{PLACEHOLDER}"

        // FIXME
        val INITIAL_CONTENT = "package konsole;\nfun foo() {}\n\nfun main() {\n{PLACEHOLDER}\n}\n"
    }

    var data = Data()

    override fun loadState(value: Data) {
        data = value
    }

    override fun getState(): Data = data

    class Data {
    }
}