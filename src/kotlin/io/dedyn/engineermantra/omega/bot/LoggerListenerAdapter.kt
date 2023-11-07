package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.Main
import io.dedyn.engineermantra.omega.bot.BotMain.messageCache
import io.dedyn.engineermantra.omega.bot.BotMain.voiceCache
import io.dedyn.engineermantra.omega.shared.ConfigFileJson
import io.dedyn.engineermantra.omega.shared.MessageLevel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.time.Instant
import java.util.*

class LoggerListenerAdapter : ListenerAdapter() {
    val ioScope = Main.BotScope()
    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (messageCache.get(event.message) != null && event.guild.idLong == 967140876298092634L) {
            val loggingChannelID: Long = 967156927731748914L
            val loggingChannel: MessageChannel = BotMain.jda.getGuildChannelById(loggingChannelID) as MessageChannel
            loggingChannel.sendMessageEmbeds(messageEditedEmbed(event)).queue()
        }
        messageCache.add(event.message)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        messageCache.add(event.message)
    }

    override fun onChannelCreate(event: ChannelCreateEvent)
    {
        if (event.isFromGuild && event.guild.idLong == 967140876298092634L) {
            ioScope.launch {
                var user: User? = null
                //Pull the user from the audit log. We'll have to wait for this as the rest of the logic
                //relies on this completing.
                event.guild.retrieveAuditLogs()
                    .type(ActionType.CHANNEL_CREATE)
                    .limit(1)
                    .queue { list -> user = list[0].user!! }
                while (user == null) {
                    Thread.sleep(1)
                }
                //Since we are going to prevent doing more work later, get the logging channel now.
                val loggingChannelID: Long = (ConfigFileJson.serverGet(event.guild.id, "logging_channel") ?: "967156927731748914").toLong()
                val loggingChannel: MessageChannel = BotMain.jda.getGuildChannelById(loggingChannelID) as MessageChannel
                //We check if the user who created the channel is TempVoice. If it isn't we wanna send the message
                //and early return.
                var member = event.guild.getMemberById(user!!.id)!!
                if (user!!.id == "762217899355013120") {
                    //Hackery time! Because of how TempVoice works, we want to detect who the owner of the VC is
                    //and attribute this to them instead of the bot.
                    println("This is a TempVoice channel")

                    //First, get all the perms on the newly created channel. We will be looking for one specific user perm.
                    //We are under the assumption this is a voice channel as TempVoice shouldn't be doing any other type
                    val channel = event.channel.asVoiceChannel()
                    //Another assumption based on how TempBot works. It sets the VC owner with a member perm.
                    val perms = channel.memberPermissionOverrides
                    for (perm in perms) {
                        if (perm.allowed.contains(Permission.VOICE_MOVE_OTHERS)) {
                            //Hello VC owner
                            member = perm.member!!
                            voiceCache[channel.idLong] = member.idLong
                            break
                        }
                    }
                    println("VC Owner: ${member.asMention}")
                    loggingChannel.sendMessageEmbeds(channelCreateTempVoiceEmbed(member, event.channel)).queue()
                }
                else {
                    loggingChannel.sendMessageEmbeds(channelCreateEmbed(member, event.channel)).queue()
                }
            }
        }
    }

    override fun onPermissionOverrideCreate(event: PermissionOverrideCreateEvent) {
        println("Permission Override")
        ioScope.launch {
            var moderator: Member? = null
            //Pull the user from the audit log. We'll have to wait for this as the rest of the logic
            //relies on this completing.
            println("Looking for who is responsible")
            event.guild.retrieveAuditLogs()
                .type(ActionType.CHANNEL_CREATE)
                .limit(1)
                .queue { list -> moderator = event.guild.getMember(list[0].user!!)}
            while (moderator == null) {
                Thread.sleep(1)
            }
            //We check if the user who created the channel is TempVoice. If it isn't we wanna send the message
            //and early return.
            if (moderator!!.id == "762217899355013120") {
                //Hackery time! Because of how TempVoice works, we want to detect who the owner of the VC is
                //and attribute this to them instead of the bot.

                //First, get all the perms on the newly created channel. We will be looking for one specific user perm.
                //We are under the assumption this is a voice channel as TempVoice shouldn't be doing any other type
                val channel = try{event.channel.asVoiceChannel()}catch(e: IllegalStateException){
                    return@launch
                }
                //Another assumption based on how TempBot works. It sets the VC owner with a member perm.
                //NOT ROLE!
                val perms = channel.memberPermissionOverrides
                println("Searching for VC Owner")
                for (perm in perms) {
                    if (perm.allowed.contains(Permission.VOICE_MOVE_OTHERS)) {
                        //Hello VC owner
                        voiceCache[channel.idLong] = perm.member!!.idLong
                        moderator = perm.member!!
                        break
                    }
                }
                println("VC Owner: ${moderator!!.asMention}")
            }
            if (event.guild.idLong == 967140876298092634L) {
                val loggingChannelID: Long = 967156927731748914L
                val loggingChannel: MessageChannel = BotMain.jda.getGuildChannelById(loggingChannelID) as MessageChannel
                if(event.isMemberOverride) {
                    loggingChannel.sendMessageEmbeds(
                        permissionOverrideCreateEmbed(
                            moderator!!,
                            event.member!!,
                            event.channel as ChannelUnion,
                            event.permissionOverride.allowed,
                            event.permissionOverride.denied,
                            event.permissionOverride.inherit
                        )
                    ).queue()
                }
                else{
                    loggingChannel.sendMessageEmbeds(
                        permissionOverrideCreateEmbed(
                            moderator!!,
                            event.role!!,
                            event.channel as ChannelUnion,
                            event.permissionOverride.allowed,
                            event.permissionOverride.denied
                        )
                    ).queue()
                }
            }
        }
    }

    override fun onPermissionOverrideDelete(event: PermissionOverrideDeleteEvent)
    {
        println("Permission Override")
        ioScope.launch {
            var moderator: Member? = null
            //Pull the user from the audit log. We'll have to wait for this as the rest of the logic
            //relies on this completing.
            println("Looking for who is responsible")
            event.guild.retrieveAuditLogs()
                .type(ActionType.CHANNEL_CREATE)
                .limit(1)
                .queue { list -> moderator = event.guild.getMember(list[0].user!!)}
            while (moderator == null) {
                Thread.sleep(1)
            }
            //We check if the user who created the channel is TempVoice. If it isn't we wanna send the message
            //and early return.
            if (moderator!!.id == "762217899355013120") {
                //Hackery time! Because of how TempVoice works, we want to detect who the owner of the VC is
                //and attribute this to them instead of the bot.

                //First, get all the perms on the newly created channel. We will be looking for one specific user perm.
                //We are under the assumption this is a voice channel as TempVoice shouldn't be doing any other type
                val channel = event.channel.asVoiceChannel()
                //Another assumption based on how TempBot works. It sets the VC owner with a member perm.
                //NOT ROLE!
                val perms = channel.memberPermissionOverrides
                //println("Searching for VC Owner")
                for (perm in perms) {
                    if (perm.allowed.contains(Permission.VOICE_MOVE_OTHERS)) {
                        //Hello VC owner
                        voiceCache[channel.idLong] = perm.member!!.idLong
                        moderator = perm.member!!
                        break
                    }
                }
                //println("VC Owner: ${moderator!!.asMention}")
            }
            if (event.guild.idLong == 967140876298092634L) {
                val loggingChannelID: Long = 967156927731748914L
                val loggingChannel: MessageChannel = BotMain.jda.getGuildChannelById(loggingChannelID) as MessageChannel
                if(event.isMemberOverride) {
                    loggingChannel.sendMessageEmbeds(
                        permissionOverrideCreateEmbed(
                            moderator!!,
                            event.member!!,
                            event.channel as ChannelUnion,
                            event.permissionOverride.allowed,
                            event.permissionOverride.denied,
                            event.permissionOverride.inherit
                        )
                    ).queue()
                }
                else{
                    loggingChannel.sendMessageEmbeds(
                        permissionOverrideCreateEmbed(
                            moderator!!,
                            event.role!!,
                            event.channel as ChannelUnion,
                            event.permissionOverride.allowed,
                            event.permissionOverride.denied
                        )
                    ).queue()
                }
            }
        }
    }

    fun messageEditedEmbed(event: MessageUpdateEvent): MessageEmbed {
        val builder = EmbedBuilder()

        //Pull everything we need out of the event instead of passing them all in individually
        val member = event.member!!
        val channel = event.channel
        val message = event.message
        val previous = messageCache.get(event.message)
        val authorAvatar = member.effectiveAvatarUrl

        builder.setTimestamp(Instant.now())
        //The URL here is translated by the Discord client into a #channel > Message link
        builder.setTitle("Message Edited: https://discord.com/channels/${member.guild.id}/${channel.id}/${message.id}")
        builder.setDescription("**Previous**: ${previous?.content ?: "Unavailable"}\n**Now:**: ${event.message.contentRaw}")
        builder.setColor(MessageLevel.Level.MODIFY.color)
        builder.setAuthor(member.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }

    fun channelCreateEmbed(member: Member, channel: ChannelUnion): MessageEmbed {
        val builder = EmbedBuilder()
        val authorAvatar = member.effectiveAvatarUrl

        builder.setTimestamp(Instant.now())
        //The URL here is translated by the Discord client into a #channel > Message link
        builder.setTitle("Channel Created: <#${channel.id}>")
        //builder.setDescription("via <@762217899355013120>")
        //builder.setDescription("**Previous**: ${previous?.content ?: "Unavailable"}\n**Now:**: ${event.message.contentRaw}")
        builder.setColor(MessageLevel.Level.MODIFY.color)
        builder.setAuthor(member.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }
    fun channelCreateTempVoiceEmbed(moderator: Member, channel: ChannelUnion): MessageEmbed {
        val builder = EmbedBuilder()
        val authorAvatar = moderator.effectiveAvatarUrl

        builder.setTimestamp(Instant.now())
        //The URL here is translated by the Discord client into a #channel > Message link
        builder.setTitle("Channel Created: <#${channel.id}>")
        builder.setDescription("via <@762217899355013120>")
        //builder.setDescription("**Previous**: ${previous?.content ?: "Unavailable"}\n**Now:**: ${event.message.contentRaw}")
        builder.setColor(MessageLevel.Level.MODIFY.color)
        builder.setAuthor(moderator.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }

    fun permissionOverrideCreateEmbed(member: Member, user: Member, channel: ChannelUnion, allowed: EnumSet<Permission>, denied: EnumSet<Permission>, inherit: EnumSet<Permission>): MessageEmbed{
        val builder = EmbedBuilder()
        val authorAvatar = member.effectiveAvatarUrl
        builder.setTimestamp(Instant.now())
        builder.setTitle("Permission Changed: @${user.effectiveName}:<#${channel.id}>")
        builder.setColor(MessageLevel.Level.MODIFY.color)
        val stringBuilder = StringBuilder()
        for(perm in allowed)
        {
            stringBuilder.append(":white_check_mark: ${perm.getName()}\n")
        }
        for(perm in denied)
        {
            stringBuilder.append(":no_entry: ${perm.getName()}\n")
        }
        builder.setDescription(stringBuilder.toString())
        builder.setAuthor(member.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }
    fun permissionOverrideCreateEmbed(member: Member, role: Role, channel: ChannelUnion, allowed: EnumSet<Permission>, denied: EnumSet<Permission>): MessageEmbed{
        val builder = EmbedBuilder()
        val authorAvatar = member.effectiveAvatarUrl
        builder.setTimestamp(Instant.now())
        builder.setTitle("Permission Changed: @${role.name}:<#${channel.id}>")
        builder.setColor(MessageLevel.Level.MODIFY.color)
        val stringBuilder = StringBuilder()
        for(perm in allowed)
        {
            stringBuilder.append(":white_check_mark: ${perm.getName()}\n")
        }
        for(perm in denied)
        {
            stringBuilder.append(":no_entry: ${perm.getName()}\n")
        }
        builder.setDescription(stringBuilder.toString())
        builder.setAuthor(member.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }
    //https://ptb.discord.com/channels/967140876298092634/1163106061507637278/1163127710437089291
    fun messageDeletedEmbed(member: Member, channel: MessageChannelUnion, old_message: String): MessageEmbed{
        val builder = EmbedBuilder()
        val authorAvatar = member.effectiveAvatarUrl
        builder.setTimestamp(Instant.now())
        builder.setTitle("Deleted: <#${channel.id}>")
        builder.setColor(MessageLevel.Level.DELETE.color)
        builder.setDescription(old_message)
        builder.setAuthor(member.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }
}