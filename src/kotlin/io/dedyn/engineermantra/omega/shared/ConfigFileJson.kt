package io.dedyn.engineermantra.omega.shared

import org.json.*

object ConfigFileJson {
    private var configFile: JSONObject? = null
    private var defaultConfig: String = "/config/config.json"
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

    fun setUser(userId: String, key: String, value: String){
        lateinit var userConfig: JSONObject
        if(configFile != null) {
            try {
                userConfig = configFile!!.getJSONObject(userId)
            }
            catch(e: JSONException)
            {
                userConfig = JSONObject()
            }
        }
        else{
            loadDefaultConfig()
            return setUser(userId, key, value)
        }
        //By the time we get to here, userConfig MUST be a value
        userConfig.put(key, value)

    }

    fun getUser(userId: String, key: String): String{
        lateinit var userConfig: JSONObject
        if(configFile != null) {
            try {
                userConfig = configFile!!.getJSONObject(userId)
            }
            catch(e: JSONException)
            {
                return ""
            }
        }
        else{
            loadDefaultConfig()
            return getUser(userId, key)
        }
        //By the time we get to here, userConfig MUST be a value
        return try {
            userConfig.getString(key)
        } catch(e: JSONException) {
            ""
        }
    }

    fun deleteUser(userId: String){
        lateinit var userConfig: JSONObject
        if(configFile != null) {
            try {
                configFile!!.remove(userId.toString())
            }
            catch(e: JSONException)
            {
                //User doesn't exist in our config.
                //Just return
                return;
            }
        }
        else{
            loadDefaultConfig()
            return deleteUser(userId)
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