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

        val TIME_LIMIT = 100L

        val MAX_USAGES = 1000
    }

    var data = Data()

    override fun loadState(value: Data) {
        data = value
    }

    override fun getState(): Data = data

    fun getConsoleHistory() = ConsoleHistory(data.CONSOLE_HISTORY)

    class ConsoleHistory(list: MutableList<String>) : MutableList<String> by list {
        val myList = list
        fun smartAppend(string: String) {
            remove(string)
            add(string)
            while (size > 50) {
                myList.removeAt(0)
            }
        }
    }
    class Data {
        var CONSOLE_HISTORY = ArrayList<String>()
    }
}