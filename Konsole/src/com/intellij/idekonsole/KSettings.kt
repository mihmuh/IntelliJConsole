package com.intellij.idekonsole

import com.intellij.openapi.components.*

@State(name = "IDEKonsole", storages = arrayOf(Storage(StoragePathMacros.APP_CONFIG + "/ide_konsole.xml")))
internal class KSettings : PersistentStateComponent<KSettings.Data> {
    var data = Data()
    val instance: KSettings
        get() = ServiceManager.getService(KSettings::class.java)

    override fun loadState(value: Data) {
        data = value
    }

    override fun getState(): Data = data

    class Data {
    }
}