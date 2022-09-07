package io.github.m_vollan.omega.bot

import io.github.m_vollan.omega.shared.ConfigFile
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

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
            "suggest" -> addSuggestion(event)
            "set" -> setConfigValue(event)
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

    fun addSuggestion(event: SlashCommandInteractionEvent){
        event.deferReply().queue()
        val suggestionChannel: TextChannel? = event.guild!!.getTextChannelById(ConfigFile.serverGet(event.guild!!.idLong, "suggestion"))
        suggestionChannel!!.sendMessageEmbeds(
            EmbedBuilder()
                .addField(MessageEmbed.Field("New Suggestion:", event.getOption("suggestion")!!.asString,true))
                .setAuthor(event.member!!.effectiveName + "#" + event.member!!.user.discriminator, event.member!!.avatarUrl, event.member!!.avatarUrl)
                .setColor(event.member!!.color)
                .setFooter(event.guild!!.name, event.guild!!.iconUrl)
                .build()
        )
        event.reply("Suggestion Sent").setEphemeral(true).queue()
    }

    fun setConfigValue(event: SlashCommandInteractionEvent){
        event.deferReply().queue()
        when(event.subcommandName){
            "suggestion_channel" -> ConfigFile.serverSet(event.guild!!.id, "suggestion", event.getOption("channel")!!.asChannel.id)
        }
        event.reply("Done!").setEphemeral(true).queue()
    }
}
