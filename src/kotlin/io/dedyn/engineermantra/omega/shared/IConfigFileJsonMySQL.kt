package io.dedyn.engineermantra.omega.shared

import org.json.JSONException
import org.json.JSONObject

interface IConfigFileJsonMySQL{
    var loadedFile: JSONObject
    fun load(): Boolean

    fun save()

    fun get(token: String): String?{
        return try {
            loadedFile.getString(token)} catch(e: JSONException){null}
    }

    fun set(token: String, value: String){
        loadedFile.remove(token)
        loadedFile.put(token, value)
    }

    fun delete(id: Long)

}