package io.dedyn.engineermantra.omega.shared

import java.sql.Timestamp

object DatabaseObject {
    data class Strike(val id: Long, val userId: Long, val serverId: Long, var type: String, var reason: String?,
                      var points: Int?, val moderatorId: Long, val date: Timestamp)
    {
        fun display(): String{
            return "Strike ID: ${id}\n" +
                    "Strike Type: ${type}\n" +
                    "Reason: ${reason}\n" +
                    "Points: ${points ?: 0}\n" +
                    "Issued By: <@${moderatorId}>\n" +
                    "Issued Time: <t:${(date.time/1000)}:F>"
        }
    }
    data class BoosterPerks(val id: Int, val userId: Long, val serverId: Long, val roleId: Long)

    data class Server(val serverId: Long, val auditChannel: Long)

    //We are using json here so we can have a list of key-value pairs without having to add another column for each
    //entry.
    data class Usernames(val userId: Long, val json: String)

    data class RoleBannedUser(val banId: Int, val userId: Long, val roleId: Long)
}