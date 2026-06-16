package io.dedyn.engineermantra.omega.bot

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.Date
import java.util.concurrent.TimeUnit

class AntiBotListenerAdapter : ListenerAdapter() {
    var historialMentions: MutableMap<Long, Pair<Date, Int>> = mutableMapOf()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || event.author.isSystem) return
        if (event.member?.hasPermission(Permission.MESSAGE_MANAGE) == true) return
        if (event.message.mentions.roles.isEmpty()) return

        for (mention in event.message.mentions.roles) {
            val currentTime = Date().time
            val entry = historialMentions[mention.idLong]

            // Check if role has been mentioned recently and count it
            if (entry != null && currentTime - entry.first.time < TimeUnit.MINUTES.toMillis(10)) {
                // If the same role is mentioned more than 3 times in less than 10 minutes, ban the user
                if (entry.second >= 3) {
                    event.member?.ban(1, TimeUnit.DAYS)
                        ?.reason("AntiBot: Too many role mentions in a short period of time")
                        ?.queue()
                    continue // Skip further processing for this role mention
                }
                // Increment count since the role is mentioned again within the time frame
                historialMentions[mention.idLong] = Pair(entry.first, entry.second + 1)
            } else {
                // If more than 10 minutes have passed or it's a new role, reset/initialize counter
                historialMentions[mention.idLong] = Pair(Date(), 1)
            }
        }
    }
}
