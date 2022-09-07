package io.github.m_vollan.omega.shared

import org.json.*

object ConfigFile {
    private var configFile: JSONObject? = null
    private var defaultConfig: String = "/config.json"
    fun getToken() : String? {
        if(configFile != null){
            try {
                return configFile!!.getString("token")
            }
            catch(e: JSONException)
            {
                return null
            }
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
        loadConfigFrom(defaultConfig)
    }
    fun setDefaultConfig(path: String){
        defaultConfig = path
    }

    fun writeDefaultConfig(){
        Utils.writeFile(configFile.toString(), Utils.getRunningDir() + defaultConfig)
    }

    fun get(token: String): String?{
        if(configFile != null) {
            try{
                return configFile!!.getString(token)
            }
            catch(e: JSONException)
            {
                return null
            }
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
            try {
                return configFile!!.getJSONObject(serverId)
            }
            catch(e: JSONException)
            {
                return JSONObject()
            }
        }
        else {
            loadDefaultConfig()
            return getServerConfig(serverId)
        }
    }

    fun serverGet(serverId: Long, token: String): String?{
        return serverGet(serverId.toString(), token)
    }
    fun serverGet(serverId: String, token: String): String?{
        if(configFile != null) {
            val config = getServerConfig(serverId)
            try {
                return config.getString(token)
            }
            catch(e: JSONException)
            {
                return null
            }
        }
        else {
            loadDefaultConfig()
            return serverGet(serverId, token)
        }
    }

    fun serverSet(serverId: String, token: String, value: String){
        if(configFile != null) {
            val config = getServerConfig(serverId)
            //Override what we need to change
            config.put(token, value)
            configFile!!.put(serverId, config)
            //Write out the change
            writeDefaultConfig()
            //Reload the change from disk
            loadDefaultConfig()
        }
        else {
            loadDefaultConfig()
            return serverSet(serverId, token, value)
        }
    }
}