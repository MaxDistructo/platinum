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

    data class RoleBannedUser(val banId: Int, val userId: Long, val roleId: Long)

    data class Leveling(val levelingId: Int, val userId: Long, val serverId: Long, var voicePoints: Int, var textPoints: Int){
        //This is done this way so that it's accurate based on the 2 values you can change.
        //Enforces only modifying the voice/text points, not the final value.
        val levelingPoints
                get() = voicePoints + textPoints
    }
    data class Counting(val countingId: Int, val serverId: Long, val channelId: Long, var topCount: Long, var currentCount: Long, var mostRecent: Long)
}