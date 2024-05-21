package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.shared.ConfigMySQL
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.mariuszgromada.math.mxparser.Expression

class CountingListenerAdapter: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent){
        if(!event.isFromGuild || event.isWebhookMessage || event.member!!.user.isBot){
            return
        }
        val countingInfo = ConfigMySQL.getCountingInfo(event.guild.idLong) ?: return
        if(countingInfo.channelId != event.channel.idLong){
            return
        }
        var digit = 0L;
        //Attempt to figure out what the user has said.
        if(event.message.contentRaw.all{char -> char.isDigit()}){
            digit = event.message.contentRaw.toLong()
        }
        else{
            val expression = Expression(event.message.contentRaw)
            val result = expression.calculate()
            if (!result.isNaN()){
                digit = result.toLong()
            }
        }
        //Ignore non-counts or 0s. 0 alone is never a valid number so we ignore it
        if(digit == 0L) return
        //Ignore double counts
        if(digit == countingInfo.currentCount) return
        if ((countingInfo.mostRecent == event.message.author.idLong && digit == (countingInfo.currentCount + 1)) || digit != (countingInfo.currentCount + 1)){
            //BAD. NO 2x in a row from same person or count + 1
            event.message.addReaction(Emoji.fromUnicode("U+0058")).queue()
            countingInfo.mostRecent = 0
            countingInfo.currentCount = 0
            ConfigMySQL.setCountingInfo(countingInfo)
            return
        }
        val digitEntered = (event.message.contentRaw).toLong()
        if (digitEntered == (countingInfo.currentCount + 1)) {
            if (digitEntered > countingInfo.topCount) {
                event.message.addReaction(Emoji.fromUnicode("U+2611")).queue()
                countingInfo.mostRecent = event.author.idLong
                countingInfo.currentCount++
                countingInfo.topCount = countingInfo.currentCount

            } else {
                event.message.addReaction(Emoji.fromUnicode("U+2705")).queue()
                countingInfo.mostRecent = event.author.idLong
                countingInfo.currentCount++
            }
        }
        ConfigMySQL.setCountingInfo(countingInfo)
    }

}