package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.bot.DiscordUtils.simpleEmbed
import io.dedyn.engineermantra.omega.shared.ConfigFileJson
import io.dedyn.engineermantra.omega.shared.ConfigMySQL
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

object Auditing {
    fun auditEntry(serverId: Long, string: String)
    {
        val auditChannelId = ConfigFileJson.get("auditChannel")
        val channel = BotMain.jda.getGuildChannelById(auditChannelId ?: "") ?: return
        (channel as TextChannel).sendMessage(string).queue()
    }

    fun automodEntry(serverId: Long, string: String)
    {
        val auditChannelId = "1034146284312989707"
        val channel = BotMain.jda.getGuildChannelById(auditChannelId ?: "") ?: return
        (channel as TextChannel).sendMessageEmbeds(simpleEmbed(BotMain.jda.selfUser, string)).queue()
    }

    fun runAuditing(jda: JDA){
        //This is a safety measure so that we don't run auditing within the dev environment
        if(jda.selfUser.idLong != 1107721065947484302L) {
            for (guild in jda.guilds) {
                boosterAudit(jda.selfUser, guild)
                roleAudit(guild)
            }
        }
    }

    //This is called both directly when ran as a command and indirectly when ran by the auditing thread
    //DO NOT MAKE PRIVATE
    fun boosterAudit(botUser: User, guild: Guild){
        val boosters = guild.boosters
        for(boosterEntry in ConfigMySQL.getBoosters(guild.idLong))
        {
            //Force JDA to get us the most up-to-date information on if this member is in the server
            //Since this is an audit, we are fine with being a bit slower to properly verify accuracy
            val member = guild.retrieveMemberById(boosterEntry.userId).useCache(false).complete()
            if(member == null || !boosters.contains(member))
            {
                ConfigMySQL.removeBooster(boosterEntry)
                guild.getRoleById(boosterEntry.roleId)!!.delete().reason("Deleting Booster Role. Reason: Audit shows user no longer boosting.").queue()
            }
        }
        for(booster in boosters)
        {
            val boosterInfo = ConfigMySQL.getBoosterItem(booster.idLong, guild.idLong)
            //If it's null, we don't have a record of what role is theirs. Check for one with the
            //effective name and assign it to them. If there is not one, create it.
            if(boosterInfo == null) {
                // Try to find the existing booster role via the current technique.
                val roles = guild.getRolesByName(booster.user.name, false)
                var role: Role
                if (roles.isEmpty()) {
                    role = guild.createRole().setName(booster.effectiveName).complete()
                    guild.addRoleToMember(booster, role).queue()
                }
                else{
                    role = roles[0]
                }
                ConfigMySQL.addBoosterItem(booster.idLong, booster.guild.idLong, role.idLong)
                val dms = booster.user.openPrivateChannel().complete()
                dms.sendMessage("**Thank You for boosting Salem Central!**\n\n" +
                        "While you are boosting Salem Central, you will get the following perks. If there is another perk " +
                        "added, it will be announced.\n" +
                        "**Access to a Custom Role**\n" +
                        "   - You may create your own custom role using the ```/role``` command with" +
                        "${botUser.asMention}. This role may be updated as many times as you please with a custom color" +
                        "and icon.\n" +
                        "**Server Emoji/Soundboard**\n"+
                        "   - Upon request, we will add almost any emoji or soundboard sound you wish. This is subject to staff" +
                        "approval though as we cannot automate it."
                ).queue()
            }
        }
    }

    fun roleAudit(guild: Guild)
    {
        if(guild.idLong == 967140876298092634L) {
            for (member in guild.members) {
                if (!member.roles.contains(guild.getRoleById(1078829209616666705)) && !ConfigMySQL.checkHasRoleBan(member.idLong, 1078829209616666705))
                {
                    guild.addRoleToMember(member, guild.getRoleById(1078829209616666705)!!).queue()
                }
            }
        }
    }
}