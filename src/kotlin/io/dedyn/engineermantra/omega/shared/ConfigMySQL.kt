package io.dedyn.engineermantra.omega.shared

import net.dv8tion.jda.api.entities.Message
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object ConfigMySQL: ConfigFileInterface {
    lateinit var connection: Connection
    val server_ip: String
    val server_port: String
    val db_name: String
    val username: String
    val password: String

    init{
        server_ip = ConfigFileJson.get("sql_server_ip") ?: "localhost"
        server_port = ConfigFileJson.get("sql_server_port") ?: "3306"
        db_name = ConfigFileJson.get("sql_database") ?: "omega"
        username = ConfigFileJson.get("sql_username") ?: "omega"
        password = ConfigFileJson.get("sql_password") ?: "REDACTED"
        load()
    }

    override fun load() {
        connection = DriverManager.getConnection("jdbc:mariadb://$server_ip:$server_port/$db_name?user=$username&password=$password")
    }

    override fun save() {
        //NOP, SQL saves in real time
        return
    }

    override fun get(token: String): String? {
        return ConfigFileJson.get(token)
    }

    override fun set(token: String, value: String) {
        //This is a NOP because we can't easily implement it
        //TODO: Come back to this, it's going to take a bunch of work to think this through
        return
    }
    private fun assertIsConnected()
    {
        if(connection.isClosed)
        {
            load()
        }
        else{
            connection.close()
            load()
        }
    }

    //This should ONLY be called once per SQL server and only by Main through the command line argument
    fun setupSQL(){
        assertIsConnected()
        connection.createStatement().execute("CREATE TABLE users( " +
                "userId BIGINT PRIMARY KEY NOT NULL UNIQUE);")
        connection.createStatement().execute("CREATE TABLE servers(" +
                "userId BIGINT PRIMARY KEY NOT NULL UNIQUE" +
                "serverId BIGINT PRIMARY KEY NOT NULL UNIQUE);")
        connection.createStatement().execute("CREATE TABLE message_cache(" +
                "messageId BIGINT PRIMARY KEY NOT NULL UNIQUE," +
                "content VARCHAR(1000)," +
                "lastEdited DATETIME," +
                "isPinned BOOLEAN);")
        connection.createStatement().execute("CREATE TABLE strikes(" +
                "strikeId BIGINT PRIMARY KEY NOT NULL UNIQUE AUTO_INCREMENT," +
                "serverId BIGINT," +
                "userId BIGINT," +
                "reason VARCHAR(200)," +
                "type VARCHAR(5)," +
                "moderatorId BIGINT," +
                "points INT," +
                "time DATETIME"+
                ");")
        connection.createStatement().execute("CREATE TABLE boosters(" +
                "boosterId INT PRIMARY KEY NOT NULL UNIQUE AUTO_INCREMENT," +
                "userId BIGINT," +
                "serverId BIGINT," +
                "roleId BIGINT" +
                ");")
        connection.createStatement().execute("CREATE TABLE usernames(" +
                "uid BIGINT PRIMARY KEY NOT NULL UNIQUE," +
                "json VARCHAR(2048)"+
                ");")
    }

    fun addStrike(strike: DatabaseObject.Strike)
    {
        assertIsConnected()
        connection.createStatement().executeUpdate("INSERT INTO strikes (userId, serverId, reason, type, points, moderatorId, time) VALUES (${strike.userId}, ${strike.serverId}, \"${strike.reason!!.replace("\"","\\\"").replace("\'","\\\'").substring(0, strike.reason!!.length.coerceAtMost(200))}\", \"${strike.type}\", ${strike.points}, ${strike.moderatorId}, NOW());")
    }

    fun updateStrike(strikeId: Long, reason: String?, points: Int?){
        assertIsConnected()
        if(reason != null) {
            connection.createStatement().executeUpdate("UPDATE strikes SET reason=\"${
                reason.replace("\"", "\\\"").replace("\'", "\\\'").substring(0, reason.length.coerceAtMost(200))
            }\" WHERE strikeId=$strikeId")
        }
        if(points != null) {
            connection.createStatement().executeUpdate("UPDATE strikes SET points=${points} WHERE strikeId=$strikeId")
        }
    }

    fun getStrikes(user: Long, server: Long): Array<DatabaseObject.Strike>
    {
        assertIsConnected()
        val query = connection.createStatement().executeQuery("SELECT strikeId, userId, serverId, reason, points, type, moderatorId, time FROM strikes WHERE userId=${user} AND serverId=$server;")
        val returnType = mutableListOf<DatabaseObject.Strike>()
        while(query.next()){
            returnType.add(
                DatabaseObject.Strike(
                    query.getLong("strikeId"),
                    query.getLong("userId"),
                    query.getLong("serverId"),
                    query.getString("type"),
                    query.getString("reason"),
                    query.getInt("points"),
                    query.getLong("moderatorId"),
                    query.getTimestamp("time")
                )
            )
        }
        return returnType.toTypedArray()
    }

    fun addBoosterItem(userId: Long, serverId: Long, roleId: Long)
    {
        assertIsConnected()
        connection.createStatement().executeUpdate("INSERT INTO boosters (userId, serverId, roleId) VALUES ($userId, $serverId, $roleId);")
    }
    fun updateBoosterItem()
    {
        //Not needed. Placeholder for future additions
    }
    fun getBoosterItem(userId: Long, serverId: Long): DatabaseObject.BoosterPerks?
    {
        assertIsConnected()
        val query = connection.createStatement().executeQuery("SELECT boosterId, userId, serverId, roleId FROM boosters WHERE userId=${userId} AND serverId=$serverId;")
        return if(!query.next()){
            null
        } else{
            DatabaseObject.BoosterPerks(query.getInt("boosterId"), query.getLong("userId"), query.getLong("serverId"), query.getLong("roleId"))
        }
    }

    fun getBoosters(serverId: Long): Array<DatabaseObject.BoosterPerks>
    {
        assertIsConnected()
        val query = connection.createStatement().executeQuery("SELECT boosterId, userId, serverId, roleId FROM boosters WHERE serverId=$serverId;")
        val returnType = mutableListOf<DatabaseObject.BoosterPerks>()
        while(query.next()){
            returnType.add(
                DatabaseObject.BoosterPerks(
                    query.getInt("boosterId"),
                    query.getLong("userId"),
                    query.getLong("serverId"),
                    query.getLong("roleId")
                )
            )
        }
        return returnType.toTypedArray()
    }

    fun removeBooster(obj: DatabaseObject.BoosterPerks)
    {
        assertIsConnected()
        connection.createStatement().executeUpdate("DELETE FROM boosters WHERE boosterId=${obj.id};")
    }

    //Implements new username claiming system
    fun getUsernames(id: Long): DatabaseObject.Usernames? {
        assertIsConnected()
        val query = connection.createStatement().executeQuery("SELECT * from usernames WHERE uid=$id;")
        if(!query.next()){
            return null
        }
        return DatabaseObject.Usernames(id, query.getString("json"))
    }

    fun storeUsernames(uid: Long, jsonStr: String) {
        assertIsConnected()
        //If they don't exist, getUsernames returns null. If they do exist, we just update the record already existing
        if(getUsernames(uid) == null) {
            connection.createStatement().executeUpdate("INSERT INTO usernames (uid, json) VALUES ($uid, \"${jsonStr.replace("\"", "\\\"").replace("\'", "\\\'")}\")")
        }
        else{
            connection.createStatement().executeUpdate("UPDATE usernames SET json=$jsonStr WHERE uid=$uid")
        }
    }

    fun deleteUsernames(uid: Long) {
        assertIsConnected()
        connection.createStatement().executeUpdate("DELETE FROM usernames WHERE uid=$uid")
    }

    fun roleBanUser(uid: Long, gid: Long, rid: Long)
    {
        assertIsConnected()
        connection.createStatement().executeUpdate("INSERT INTO rolebans (uid, gid, rid) VALUES ($uid, $gid, $rid);")
    }

    fun removeRoleBan(uid: Long, rid: Long)
    {
        assertIsConnected()
        connection.createStatement().executeUpdate("DELETE FROM rolebans WHERE uid=$uid AND rid=$rid;")
    }

    fun checkHasRoleBan(uid: Long, rid: Long): Boolean
    {
        assertIsConnected()
        val query = connection.createStatement().executeQuery("SELECT uid, rid FROM rolebans WHERE uid=$uid AND rid=$rid;")
        return query.next()
    }


}