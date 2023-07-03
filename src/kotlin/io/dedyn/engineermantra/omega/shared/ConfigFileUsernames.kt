package io.dedyn.engineermantra.omega.shared

import org.json.JSONObject
import org.json.JSONTokener

class ConfigFileUsernames(val loadedUID: Long) : IConfigFileJsonMySQL{
    override var loadedFile: JSONObject = JSONObject()
    override fun load(): Boolean {
        val json_str: String? = ConfigMySQL.getUsernames(loadedUID)?.json ?: return false
        loadedFile = JSONObject(JSONTokener(json_str))
        return true
    }

    override fun save(){
        ConfigMySQL.storeUsernames(loadedUID, loadedFile.toString())
    }

    override fun delete(id: Long){
        ConfigMySQL.deleteUsernames(id)
    }

}