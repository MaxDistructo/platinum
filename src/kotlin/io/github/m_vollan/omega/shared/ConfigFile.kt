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
}