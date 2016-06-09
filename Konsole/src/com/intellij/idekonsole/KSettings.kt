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

        val FILE_NAME =
                "konsole/runtime/Test.kt";

        // FIXME
        val INITIAL_CONTENT =
                """
                package konsole.runtime;

                import com.intellij.idekonsole.results.*
                import com.intellij.idekonsole.scripting.*

                fun main_exec(): KResult {
                    // code here
                    return KStdoutResult("Hello World")
                }
                """.trimIndent()
    }

    var data = Data()

    override fun loadState(value: Data) {
        data = value
    }

    override fun getState(): Data = data

    class Data {
    }
}