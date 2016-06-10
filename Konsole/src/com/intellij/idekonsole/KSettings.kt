package com.intellij.idekonsole

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.awt.Color
import java.util.*

@State(name = "IDEKonsole", storages = arrayOf(Storage("/ide_konsole.xml")))
internal class KSettings : PersistentStateComponent<KSettings.Data> {
    companion object {
        val instance: KSettings
            get() = ServiceManager.getService(KSettings::class.java)

        val LIB_NAME = "Z_SECRET_LIB"
        val MODULE_NAME = "Z_SECRET_MODULE"

        val SRC_DIR = "com/intellij/idekonsole/runtime/";
        val PSI_STUBS = "PsiClassReferences.kt"
        val JAVA_TOKEN_STUBS = "JavaTokenReferences.kt"
        val CONSOLE_FILE_PATH = SRC_DIR + "Test.kt";

        val BACKGROUND_COLOR = Color.LIGHT_GRAY
    }

    var data = Data()

    override fun loadState(value: Data) {
        data = value
    }

    override fun getState(): Data = data

    fun getConsoleHistory() = ArrayList(data.CONSOLE_HISTORY)

    fun appendConsoleHistory(string: String) {
        data.CONSOLE_HISTORY.remove(string)

        data.CONSOLE_HISTORY.add(string)
        if (data.CONSOLE_HISTORY.size > 50) {
            val oldHistory = data.CONSOLE_HISTORY
            data.CONSOLE_HISTORY = ArrayList<String>()
            data.CONSOLE_HISTORY.addAll(oldHistory.subList(oldHistory.size - 50, oldHistory.size))
        }
    }

    fun removeConsoleHistory(content: String) {
        data.CONSOLE_HISTORY.remove(content)
    }

    fun clearConsoleHistory() {
        data.CONSOLE_HISTORY.clear()
    }

    class Data {
        var CONSOLE_HISTORY = ArrayList<String>()
    }
}