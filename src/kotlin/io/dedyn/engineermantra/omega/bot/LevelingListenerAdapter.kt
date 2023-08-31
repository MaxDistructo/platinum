package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.shared.ConfigMySQL
import io.dedyn.engineermantra.omega.shared.Utils.calculateLevel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.random.Random

class LevelingListenerAdapter: ListenerAdapter() {

    //Basic message received
    val textUserList = HashMap<Long, Long>()
    override fun onMessageReceived(event: MessageReceivedEvent) {
        //We only want to do this on guild messages, return early if it's not from a guild.
        if(!event.isFromGuild || event.isWebhookMessage)
        {
            return
        }
        //Check if the user has sent a message within the last 60 seconds. Prevent spamming for points.
        if(event.member == null)
	    {
		    println("Member is null. (THIS SHOULD NEVER HAPPEN)");
	    }
        //IGNORE BOTS
        if(event.member!!.user.isBot)
        {
            return
        }
	    if(textUserList.containsKey(event.member!!.idLong))
        {
            val currentTime = System.currentTimeMillis()
            val difference = currentTime - textUserList[event.member!!.idLong]!!
            if(difference < 30*100)
            {
                return
            }
        }
        val random = Random.Default
        BotMain.logger.info("Granting Text Points to " + event.author.effectiveName)
        val leveling = ConfigMySQL.getLevelingPointsOrDefault(event.author.idLong, event.guild.idLong)
        BotMain.logger.info("Current Points: " + leveling.textPoints)
        val currentLevel = calculateLevel(leveling.levelingPoints)
        leveling.textPoints += random.nextInt(1, 10)
        BotMain.logger.info("New Points: " + leveling.levelingPoints)
        ConfigMySQL.updateLevelingPoints(leveling)
        val newLevel = calculateLevel(leveling.levelingPoints)
        //We are going to use an exponential curve on leveling
        if(newLevel > currentLevel){
            if(DiscordUtils.checkLeveledRoles(event.member!!)) {
                event.message.reply("${event.member!!.asMention} has leveled up to $newLevel!").queue()
            }
        }
        textUserList[event.member!!.idLong] = System.currentTimeMillis()
    }
    val userList = HashMap<Long,Long>()
    private fun grantPoints(member: Member)
    {
        BotMain.logger.info("Granting Voice Points to " + member.effectiveName)
        val leaveTime = System.currentTimeMillis()
        val joinTime = userList[member.idLong] ?: return
        val timeInVC = leaveTime - joinTime
        println("Raw timeInVC: $timeInVC")
        val leveling = ConfigMySQL.getLevelingPointsOrDefault(member.idLong, member.guild.idLong)
        val currentLevel = calculateLevel(leveling.levelingPoints)
        BotMain.logger.info("Current Points: " + leveling.voicePoints)
        println("Points to add: ${(timeInVC / (60.0*60*10))}")
        //1 point per 10 minutes in VC.
        leveling.voicePoints += (timeInVC / (60*60*10)).toInt()
        BotMain.logger.info("New Points: " + leveling.levelingPoints)
        ConfigMySQL.updateLevelingPoints(leveling)
        val newLevel = calculateLevel(leveling.levelingPoints)
        if(newLevel > currentLevel){
            DiscordUtils.checkLeveledRoles(member)
            val generalChannels = member.guild.getTextChannelsByName("general", false)
            //SC Override since the general channel doesn't fit the normal case.
            if(member.guild.idLong == 967140876298092634L)
            {
                generalChannels.clear()
                generalChannels.add(member.guild.getTextChannelById(967145650158436372)!!)
            }
            generalChannels[0].sendMessage("${member.asMention} has leveled up to $newLevel!").queue()
        }
        //We don't trust Discord to not bug and double call this method so overwrite the time so it doesn't
        //affect us too badly.
        userList[member.idLong] = leaveTime
    }

    //Triggers when a user joins/leaves VC
    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        //Moved between VCs
        if(event.channelJoined != null && event.channelLeft != null)
        {
            //On joining the AFK channel from another channel, grant points earned and stop counting.
            if(event.channelJoined!!.asVoiceChannel().idLong == event.guild.afkChannel!!.idLong)
            {
                grantPoints(event.member)
            }
            //Ignore this case for now, we just need to catch it at the start to not mistake this as a DC.
            return
        }
        //User Disconnected
        else if(event.channelJoined == null && event.channelLeft != null)
        {
            grantPoints(event.member)
        }
        //User Joined
        else{
            //Don't give points for AFK Channel. Discord thankfully moves the users for us so we just have to not start
            //giving points when they join the AFK channel.
            if(event.channelJoined!!.asVoiceChannel().idLong != event.guild.afkChannel!!.idLong) {
                userList[event.member.idLong] = System.currentTimeMillis()
            }
        }
    }
}
