package io.github.m_vollan.omega.bot

import io.github.m_vollan.omega.shared.ConfigFile
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.templates.TemplateChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import java.awt.Color

class SlashCommandListenerAdapter: ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val time = System.currentTimeMillis()
        when(event.name){
            "ping"-> event.reply("Pong!").setEphemeral(true).flatMap{
                event.hook.editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)} // then edit original
                .queue()
            "echo" -> event.reply(event.getOption("message")!!.asString).queue()
            "give_role" -> giveRole(event)
            "give_all_role" -> giveRoleBulk(event)
            //"give_all_role_restricted" -> giveRoleBulk(event)
            //"test_give_all_role" -> giveRoleBulk(event, true)
            //"test_give_all_role_restricted" -> giveRoleBulk(event,true)
            //"suggest" -> addSuggestion(event)
            "set" -> setConfigValue(event)
            "lockdown" -> lockdownChannel(event)
            "unlock" -> unlockChannel(event)
            "lockdown_server" -> lockdownServer(event)
            "unlock_server" -> unlockServer(event)
            "color" -> setColor(event)
            else -> println("Command not found")
        }
    }
    fun giveRole(event: SlashCommandInteractionEvent)
    {
        event.deferReply().queue()
        val role = event.getOption("role")!!.asRole
        val user = event.getOption("user")!!.asUser
        val guild = event.guild
        guild!!.addRoleToMember(user, role).complete()
        event.hook.sendMessage("Done").queue()
    }
    class RoleFindThread(val guild: Guild, val roleList: List<Role>, val restrictionList: MutableList<Member>): Thread()
    {
        //This is NOT memory efficient but easier to list for the finish of this thread being once we add all
        //the members to the shared list
        override fun run() {
            val restrictions: MutableList<Member> = mutableListOf()
            for(role2 in roleList) {
                BotMain.logger.debug("Finding members with role: ${role2.name}")
                guild.findMembers { m -> m.roles.contains(role2) }.onSuccess{l -> restrictions.addAll(l)}.get()
            }
            restrictionList.addAll(restrictions)
        }
    }

    class AddRoleThread(val event: SlashCommandInteractionEvent, val guild: Guild, val role: Role, val restrictionList: MutableList<Member>, val testMode: Boolean): Thread()
    {
        override fun run()
        {
            while(restrictionList.size <= 0){
                BotMain.logger.debug("Sleeping 100ms, waiting for completion of finding members")
                sleep(100)
            }
            var numAdditions = 0
            //For each member, if they are not in the restricted list, add the role
            BotMain.logger.debug("Looking for members without the roles. ${restrictionList.size} members have the role.")
            for(member in guild.members)
            {
                //BotMain.logger.debug("Checking if ${member.effectiveName} needs ${role.name}")
                if(!restrictionList.contains(member))
                {
                    if(!testMode) {

                        guild.addRoleToMember(member, role).queue()
                    }
                    numAdditions++
                }
            }
            if(testMode){
                event.hook.sendMessage("${role.name} would be rolled out to $numAdditions members").queue()
            }
            else {
                event.hook.sendMessage("${role.name} is being rolled out to $numAdditions members").queue()
            }
        }
    }

    fun giveRoleBulk(event: SlashCommandInteractionEvent)
    {
        event.deferReply().queue()
        val role = event.getOption("role")!!.asRole
        val guild = event.guild!!
        val otherRoles: MutableList<Role> = mutableListOf()
        val testMode = event.getOption("testMode")?.asBoolean ?: false
        //Try to get each role one by one. This shouldn't fail until we get all the restrictions in the list
        try{
            otherRoles.add(event.getOption("role2")!!.asRole)
            otherRoles.add(event.getOption("role3")!!.asRole)
            otherRoles.add(event.getOption("role4")!!.asRole)
            otherRoles.add(event.getOption("role5")!!.asRole)
        }
        catch(_: Exception){}

        //Also add members of the new role to the restricted list so that we don't double give out
        otherRoles.add(role)

        val membersOfRestricted: MutableList<Member> = mutableListOf()

        //Start finding all the members that do NOT need the role or have been restricted from getting it
        RoleFindThread(guild, otherRoles.toList(), membersOfRestricted).start()
        //Start another thread to add the roles to all the members
        AddRoleThread(event, guild, role, membersOfRestricted, testMode).start()
    }

    //This is not needed anymore with the addition of Fourm channels.
    fun addSuggestion(event: SlashCommandInteractionEvent){
        val config = ConfigFile.serverGet(event.guild!!.idLong, "suggestion") ?: "0"
        if(config == "0")
        {
            event.reply("Please set a suggestion channel before running the suggestion command.").setEphemeral(true).queue()
            return
        }
        val suggestionChannel: TextChannel? = event.guild!!.getTextChannelById(config)

        //Send an embed to the channel and add 2 reactions to the message after it has been sent
        suggestionChannel!!.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle("Suggestion: ")
                .setDescription(event.getOption("suggestion")!!.asString)
                //.addField(MessageEmbed.Field("", event.getOption("suggestion")!!.asString,true))
                .setAuthor(event.member!!.effectiveName + "#" + event.member!!.user.discriminator, event.member!!.effectiveAvatarUrl, event.member!!.effectiveAvatarUrl)
                .setColor(event.member!!.color)
                //.setFooter(event.guild!!.name, event.guild!!.iconUrl)
                .build()
        ).queue { m ->
            m.addReaction(Emoji.fromUnicode("U+2B06")).queue()
            m.addReaction(Emoji.fromUnicode("U+2B07")).queue()
        }
        event.reply("Suggestion Sent").setEphemeral(true).queue()
    }

    fun setConfigValue(event: SlashCommandInteractionEvent){
        when(event.subcommandName){
            "suggestion_channel" -> ConfigFile.serverSet(event.guild!!.id, "suggestion", event.getOption("channel")!!.asChannel.id)
        }
        event.reply("Done!").setEphemeral(true).queue()
    }

    fun lockdownChannel(event: SlashCommandInteractionEvent){
        val channel = event.guildChannel
        //channel.permissionContainer.memberPermissionOverrides
        for(permission in channel.permissionContainer.permissionOverrides){
          if(permission.allowed.contains(Permission.MESSAGE_SEND)) {
              val allowed = permission.allowed
              val denied = permission.denied
              allowed.remove(Permission.MESSAGE_SEND)
              channel.permissionContainer.upsertPermissionOverride(permission.permissionHolder!!).setAllowed(allowed).setDenied(denied)
          }
        }
        channel.permissionContainer.upsertPermissionOverride(event.guild!!.publicRole).deny(Permission.MESSAGE_SEND).queue()
        event.reply("Done! :lock:")
    }

    fun unlockChannel(event: SlashCommandInteractionEvent) {
        val channel = event.guildChannel
        for(permission in channel.permissionContainer.permissionOverrides){
            permission.delete().queue()
        }
        channel.asStandardGuildChannel().manager.sync()
        event.reply("Done! :unlock:")
    }

    fun lockdownServer(event: SlashCommandInteractionEvent){
        //O(N^2).... this will be slow af but does a load of work
        event.deferReply().queue()
        for(channel in event.guild!!.channels){
            for(permission in channel.permissionContainer.permissionOverrides)
            {
                //Handle the 1 case we DON'T want to override
                if(permission.isRoleOverride && permission.role != event.getOption("role") && permission.allowed.contains(Permission.MESSAGE_SEND))
                {
                    val allowed = permission.allowed
                    val denied = permission.denied
                    allowed.remove(Permission.MESSAGE_SEND)
                    //permission.delete().complete()
                    channel.permissionContainer.upsertPermissionOverride(permission.permissionHolder!!).setAllowed(allowed).setDenied(denied).queue()
                }
                else
                {
                    val allowed = permission.allowed
                    val denied = permission.denied
                    allowed.remove(Permission.MESSAGE_SEND)
                    //permission.delete().complete()
                    channel.permissionContainer.upsertPermissionOverride(permission.permissionHolder!!).setAllowed(allowed).setDenied(denied).queue()
                }
            }
        }
        event.reply("Done! :lock:").queue()
    }

    fun unlockServer(event: SlashCommandInteractionEvent){

        for(channel in event.guild!!.channels){
            (channel as StandardGuildChannel).manager.sync().queue()
        }
        event.reply("Done! :unlock:").queue()
    }

    fun setColor(event: SlashCommandInteractionEvent){
        val user: User = event.getOption("user", event.user, OptionMapping::getAsUser)
        val color = event.getOption("color")!!.asString
        val roles = event.guild!!.getRolesByName(user.name, false)
        val role: Role


        if(roles.isEmpty()){

            role = event.guild!!.createRole()
                .setName(user.name)
                .setColor(Color.decode(color))
                .complete()
            event.guild!!.addRoleToMember(user, role)
        }
        else{
            role = roles[0]
            role.manager.setColor(Color.decode(color)).complete()
        }
        event.reply("Done!").complete()
    }
}

