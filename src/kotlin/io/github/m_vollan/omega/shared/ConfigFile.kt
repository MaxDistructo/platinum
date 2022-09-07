package io.github.m_vollan.omega.shared

import org.json.*

object ConfigFile {
    var configFile: JSONObject? = null
    fun getToken() : String {
        if(configFile != null){
            return configFile!!.getString("token")
        }
        else
        {
            loadDefaultConfig()
            return getToken()
        }
    }
    fun loadConfigFrom(file: String){
        configFile = JSONObject(JSONTokener(Utils.readFile(Utils.getRunningDir() + file)))
    }
    fun loadDefaultConfig(){
        loadConfigFrom("/config.json")
    }
    fun get(token: String): String{
        if(configFile != null) {
            return configFile!!.getString(token)
        }
        else {
            loadDefaultConfig()
            return get(token)
        }
    }

    fun getServerConfig(serverId: Long): JSONObject{
        return getServerConfig(serverId.toString())
    }
    fun getServerConfig(serverId: String): JSONObject{
        if(configFile != null) {
            return configFile!!.getJSONObject(serverId)
        }
        else {
            loadDefaultConfig()
            return getServerConfig(serverId)
        }
    }

    fun serverGet(serverId: Long, token: String): String{
        return serverGet(serverId.toString(), token)
    }
    fun serverGet(serverId: String, token: String): String{
        if(configFile != null) {
            val config = getServerConfig(serverId)
            return config.getString(token)
        }
        else {
            loadDefaultConfig()
            return serverGet(serverId, token)
        }
    }
}