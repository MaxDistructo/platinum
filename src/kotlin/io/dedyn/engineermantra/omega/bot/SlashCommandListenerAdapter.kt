package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.bot.BotMain.logger
import io.dedyn.engineermantra.omega.bot.voice.ReceivingHandler
import io.dedyn.engineermantra.omega.shared.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URI
import java.sql.Timestamp
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.math.pow

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
            "set" -> setConfigValue(event)
            "role" -> setColor(event)
            "strike" -> giveStrike(event)
            "strikes" -> viewStrikes(event)
            "editstrike" -> editStrike(event)
            "audit" -> runAuditing(event)
            "poll" -> createPoll(event)
            "vote" -> createPoll(event)
            "agree" -> ruleAgreement(event)
            "level" -> checkLevel(event)
            "level2" -> checkLevel(event)
            "record" -> recordChannel(event)
            "sync" -> syncRoles(event)
            else -> println("Command not found")
        }
    }



    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        when(event.focusedOption.name)
        {
            "permission" -> event.replyChoices(choices(event.focusedOption.value, "permissions")).queue()
            "strike_type" -> event.replyChoices(choices(event.focusedOption.value, "strike_type")).queue()
            "game" -> event.replyChoices(choices(event.focusedOption.value, "game")).queue()
            else -> println("Autocomplete not found. Please check your command configuration. Missing: ${event.focusedOption.name}")
        }
    }


    //This is part of the AutoComplete code
    fun choices(partial_word: String, autocompleteName: String): List<Command.Choice>
    {
        var words: List<String>
        val type = listOf("Server", "Event", "Soundboard")
        val games = listOf("Town of Salem 2", "Town of Salem", "Minecraft", "Steam", "VR Chat", "Blizzard", "Epic")

        when(autocompleteName){
            "strike_type" -> words = type
            "game" -> words = games
            else -> words = listOf()
        }
        val outputList = mutableListOf<Command.Choice>()
        words.forEach{word -> if(word.startsWith(partial_word)) {outputList.add(Command.Choice(word, word))}}
        return outputList
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent)
    {
        if(event.roles.contains(event.guild.boostRole))
        {
            //New Booster! Send them the information about the booster perks
            val dms = event.member.user.openPrivateChannel().complete()
            dms.sendMessage("**Thank You for boosting Salem Central!**\n\n" +
                    "While you are boosting Salem Central, you will get the following perks. If there is another perk " +
                    "added, it will be announced.\n" +
                    "**Access to a Custom Role**\n" +
                    "   - You may create your own custom role using the ```/role``` command with" +
                    "${event.jda.selfUser.asMention}. This role may be updated as many times as you please with a custom color" +
                    "and icon.\n" +
                    "**Server Emoji/Soundboard**\n"+
                    "   - Upon request, we will add almost any emoji or soundboard sound you wish. This is subject to staff" +
                    "approval though as we cannot automate it."
            ).queue()
            val booster_info = ConfigMySQL.getBoosterItem(event.member.idLong, event.guild.idLong)
            if(booster_info == null)
            {
                val role = event.guild.createRole().setName(event.member.effectiveName).complete()
                event.guild.addRoleToMember(event.member, role).queue()
                ConfigMySQL.addBoosterItem(event.member.idLong, event.guild.idLong, role.idLong)
            }
        }
    }

    class RoleFindThread(val guild: Guild, val roleList: List<Role>, val restrictionList: MutableList<Member>): Thread()
    {
        //This is NOT memory efficient but easier to list for the finish of this thread being once we add all
        //the members to the shared list
        override fun run() {
            val restrictions: MutableList<Member> = mutableListOf()
            for(role2 in roleList) {
                logger.debug("Finding members with role: ${role2.name}")
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
                logger.debug("Sleeping 100ms, waiting for completion of finding members")
                sleep(100)
            }
            var numAdditions = 0
            //For each member, if they are not in the restricted list, add the role
            logger.debug("Looking for members without the roles. ${restrictionList.size} members have the role.")
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
    fun giveRole(event: SlashCommandInteractionEvent)
    {
        event.deferReply().queue()
        val role = event.getOption("role")!!.asRole
        val user = event.getOption("user")!!.asUser
        val guild = event.guild
        guild!!.addRoleToMember(user, role).complete()
        event.hook.sendMessage("Done").queue()
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

    fun setConfigValue(event: SlashCommandInteractionEvent)
    {
        when(event.subcommandName){
            "suggestion_channel" -> ConfigFileJson.serverSet(event.guild!!.id, "suggestion", event.getOption("channel")!!.asChannel.id)
        }
        event.reply("Done!").setEphemeral(true).queue()
    }

    fun setColor(event: SlashCommandInteractionEvent)
    {
        //This is locked to ONLY server boosters/staff with manage role permissions
        if(event.member!!.isBoosting || event.member!!.hasPermission(Permission.MANAGE_ROLES)) {
            val user: User = event.getOption("user", event.user, OptionMapping::getAsUser)
            var color = event.getOption("color")!!.asString
            //Fix a common user issue with them not putting the # at the start of the color code
            if(color[0] != '#')
            {
                color = "#$color"
            }
            val role: Role
            //Alternate path for boosters to more easily get the role.
            if(event.member!!.isBoosting)
            {
                val boosterInfo = ConfigMySQL.getBoosterItem(event.member!!.idLong, event.guild!!.idLong)!!
                role = event.guild!!.getRoleById(boosterInfo.roleId)!!
                //Booster perk, it sets the name of the role to the nickname (otherwise on the actual name)
                role.manager.setName(event.member!!.effectiveName).queue()
            }
            else {
                val roles = event.guild!!.getRolesByName(user.name, false)
                if (roles.isEmpty()) {
                    role = event.guild!!.createRole()
                        .setName(user.name)
                        .setColor(Color.decode(color))
                        .complete()
                    event.guild!!.addRoleToMember(user, role).queue()
                } else {
                    role = roles[0]
                }
            }
            role.manager.setColor(Color.decode(color)).queue()
            val icon = event.getOption("icon_url")
            //Take advantage of a library we already have as well as the built-in java stuff
            //to check if the URL is to an image.
            if(icon != null)
            {
                //We do not care if there's an error running this, we just ignore the param
                var potentialImage: BufferedImage?
                logger.info("Trying to get image: $icon")
                try {
                    //This is our most likely IOException though we could have more in the file write/delete code
                    potentialImage = ImageIO.read(URI.create(icon.asString).toURL())

                    //Now collect the image we got
                    logger.info("Writing image to file")
                    val tmpfile = File("tmp.png")
                    ImageIO.write(potentialImage, "png", tmpfile)
                    logger.info("uploading image to discord")
                    role.manager.setIcon(Icon.from(tmpfile)).complete()
                    tmpfile.delete()
                }
                catch(e: IOException) {
                    event.reply("Error setting role icon").queue()
                }
            }
            //This just quickly checks if the role is already above server booster, staff roles would be even higher
            //and we don't wanna move them.
            if(role.position < event.guild!!.boostRole!!.position) {
                event.guild!!.modifyRolePositions().selectPosition(role).moveAbove(event.guild!!.boostRole!!).queue()
                logger.info("Changing role position as it's below the booster role")
            }
            else {
                logger.info("Not changing role position as it's above the booster role")
            }
            event.reply("Done!").queue()
        }
        else{
            event.reply("You are not allowed to do this").queue()
        }
    }

    fun giveStrike(event: SlashCommandInteractionEvent)
    {
        assert(event.isFromGuild)
        event.reply("Issuing Strike").queue()
        val strike_type = event.getOption("strike_type")!!.asString.lowercase()
        val points = event.getOption("points")?.asInt ?: 0
        //We first check if they have event perms to give event strikes
        //ID does not exist at this point, we assign 0 to just create the object
        val strike = DatabaseObject.Strike(
            0,
            event.getOption("user")!!.asUser.idLong,
            event.guild!!.idLong,
            "event",
            event.getOption("reason")?.asString ?: "No Reason Provided",
            points,
            event.member!!.idLong,
            Timestamp.from(Instant.now())
        )
        if(event.member!!.hasPermission(Permission.MANAGE_EVENTS) && strike_type == "event")
        {
            //Perform the action
            ConfigMySQL.addStrike(strike)
            //Respond to user
            event.reply("Applied event strike to ${event.getOption("user")!!.asUser.asMention}").queue()
            //Optional action after
            if(event.getOption("send_dm")?.asBoolean ?: true)
            {
                val private_channel = event.getOption("user")!!.asUser.openPrivateChannel().complete()
                private_channel.sendMessage("You were issued an Event Strike by ${event.member!!.asMention} in ${event.guild!!.name} for ${event.getOption("reason")?.asString ?: "No Reason Provided"}").queue()
            }
            //Audit the event
            Auditing.auditEntry(
                event.guild!!.idLong,
                "${event.member!!.effectiveName} issued an Event Warning against ${event.getOption("user")!!.asUser.asMention} with reason: ${
                    event.getOption("reason")?.asString ?: "No Reason Provided"
                }"
            )
        }
        else if(event.member!!.hasPermission(Permission.MESSAGE_MANAGE) && strike_type == "server")
        {
            strike.type = "svr"
            ConfigMySQL.addStrike(strike)
            event.reply("Applied strike to ${event.getOption("user")!!.asUser.asMention}").queue()
            if(event.getOption("send_dm")?.asBoolean ?: true)
            {
                val private_channel = event.getOption("user")!!.asUser.openPrivateChannel().complete()
                private_channel.sendMessage("You were issued a Strike by ${event.member!!.asMention} in ${event.guild!!.name} for ${event.getOption("reason")?.asString ?: "No Reason Provided"}").queue()
            }
            Auditing.auditEntry(
                event.guild!!.idLong,
                "${event.member!!.effectiveName} issued a Warning against ${event.getOption("user")!!.asUser.asMention} with reason: ${
                    event.getOption("reason")?.asString ?: "No Reason Provided"
                }"
            )
        }
        else if(event.member!!.hasPermission(Permission.MANAGE_ROLES) && strike_type == "soundboard")
        {
            strike.type = "sbrd"
            ConfigMySQL.addStrike(strike)
            event.reply("Applied strike to ${event.getOption("user")!!.asUser.asMention}").queue()
            if(event.getOption("send_dm")?.asBoolean ?: true)
            {
                val private_channel = event.getOption("user")!!.asUser.openPrivateChannel().complete()
                private_channel.sendMessage("You issued a Soundboard Strike by ${event.member!!.asMention} in ${event.guild!!.name} for ${event.getOption("reason")?.asString ?: "No Reason Provided"}").queue()
            }
            Auditing.auditEntry(
                event.guild!!.idLong,
                "${event.member!!.effectiveName} issued a Soundboard Warning against ${event.getOption("user")!!.asUser.asMention} with reason: ${
                    event.getOption("reason")?.asString ?: "No Reason Provided"
                }"
            )
        }
    }

    fun viewStrikes(event: SlashCommandInteractionEvent)
    {
        assert(event.isFromGuild)
        event.reply("Sending strikes shortly").queue()
        //Get the strikes for the mentioned user or the author if a user was not provided
        val user = (event.getOption("user")?.asUser ?: event.user)
        val strikes = ConfigMySQL.getStrikes(user.idLong, event.guild!!.idLong)
        val privateChannel = event.user.openPrivateChannel().complete()
        if(event.getOption("public")?.asBoolean ?: false)
        {
            if(strikes.isEmpty())
            {
                if(event.channel.type == ChannelType.TEXT) {
                    event.channel.asTextChannel().sendMessageEmbeds(
                        DiscordUtils.simpleEmbed(
                            event.guild!!.getMember(user)!!,
                            "${user.asMention} has no strikes",
                            event.guild!!
                        )
                    ).queue()
                }
                else if(event.channel.type == ChannelType.VOICE)
                {
                    event.channel.asVoiceChannel().sendMessageEmbeds(
                        DiscordUtils.simpleEmbed(
                            event.guild!!.getMember(user)!!,
                            "${user.asMention} has no strikes",
                            event.guild!!
                        )
                    ).queue()
                }
            }
            for(strike in strikes)
            {
                if(event.channel.type == ChannelType.TEXT) {
                    event.channel.asTextChannel().sendMessageEmbeds(
                        DiscordUtils.simpleEmbed(
                            event.guild!!.getMember(user)!!,
                            strike.display(),
                            event.guild!!
                        )
                    ).queue()
                }
                else if(event.channel.type == ChannelType.VOICE)
                {
                    event.channel.asVoiceChannel().sendMessageEmbeds(
                        DiscordUtils.simpleEmbed(
                            event.guild!!.getMember(user)!!,
                            strike.display(),
                            event.guild!!
                        )
                    ).queue()
                }
            }
        }
        else{
            if(strikes.isEmpty())
            {
                privateChannel.sendMessageEmbeds(
                    DiscordUtils.simpleEmbed(
                        event.guild!!.getMember(user)!!,
                        "${user.asMention} has no strikes",
                        event.guild!!
                    )
                ).queue()
            }
            for(strike in strikes) {
                privateChannel.sendMessageEmbeds(
                    DiscordUtils.simpleEmbed(
                        event.guild!!.getMember(user)!!,
                        strike.display(),
                        event.guild!!
                    )
                ).queue()
            }
        }
    }

    fun editStrike(event: SlashCommandInteractionEvent)
    {
        val strikeId = event.getOption("strikeid")!!.asLong
        val reason = event.getOption("reason")?.asString
        val points = event.getOption("points")?.asInt
        ConfigMySQL.updateStrike(strikeId, reason, points)
        event.reply("Done!").setEphemeral(true).queue()
    }

    fun runAuditing(event: SlashCommandInteractionEvent)
    {
        assert(event.isFromGuild)
        event.reply("Audit in progress").queue()
        Auditing.runAuditing(event.jda)
    }

    fun createPoll(event: SlashCommandInteractionEvent)
    {
        val emoji = listOf(":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":zero:")
        val emoji_codepoint = listOf("U+0031 U+20E3", "U+0032 U+20E3", "U+0033 U+20E3", "U+0034 U+20E3","U+0035 U+20E3","U+0036 U+20E3","U+0037 U+20E3","U+0038 U+20E3","U+0039 U+20E3","U+0030 U+20E3")
        //We do null checks here based on the requirements setup in BotMain
        val prompt = event.getOption("prompt")!!.asString
        //val options = mutableListOf<String>(, event.getOption("option2")?.asString ?: "no")
        var i = 1
        val sb = StringBuilder()
        while(i <= 10)
        {
            if(i == 1)
            {
                sb.append("${emoji[i-1]} - ${event.getOption("option1")?.asString ?: "yes"}\n")
            }
            else if(i == 2)
            {
                sb.append("${emoji[i-1]} - ${event.getOption("option2")?.asString ?: "no"}\n")
            }
            else {
                sb.append("${emoji[i-1]} - ${event.getOption("option$i")?.asString ?: break}\n")
            }
            i++
        }
        event.reply("Done!").setEphemeral(true).queue()
        val message = event.channel.sendMessageEmbeds(DiscordUtils.simpleTitledEmbed(event.member!!, prompt, sb.toString(), event.guild!!)).complete()
        var ii = 1
        while(ii < i) {
            message.addReaction(Emoji.fromFormatted(emoji_codepoint[ii-1])).queue()
            ii++
        }
    }
    fun ruleAgreement(event: SlashCommandInteractionEvent)
    {
        //Command is dumb and lets the individual server listeners handle the button's action
        event.reply("Do you agree to the rules specified?")
            .addActionRow(Button.primary("agree", "Agree"), Button.danger("disagree", "Disagree"))
            .queue()
    }
    fun checkLevel(event: SlashCommandInteractionEvent)
    {
        val userOption = event.getOption("user")
        val user: Member? = if(userOption != null) {
            userOption.asMember
        } else {
            event.member
        }
        val leveling: DatabaseObject.Leveling = ConfigMySQL.getLevelingPointsOrDefault(user!!.idLong, event.guild!!.idLong)
        var level = Utils.calculateLevel(leveling.levelingPoints)
        val expCurrent = (6/5 * level.toDouble().pow(3) - 15 * level.toDouble().pow(2) + 100 * level.toDouble() - 140).toInt()
        level++
        val expNeeded = (6/5 * level.toDouble().pow(3) - 15 * level.toDouble().pow(2) + 100 * level.toDouble() - 140).toInt()
        event.replyEmbeds(DiscordUtils.simpleTitledEmbed(user, "Leveling Stats",
            "You are currently level ${Utils.calculateLevel(leveling.levelingPoints)}\nTotal Points: ${leveling.levelingPoints}\n"
                    +  "Points from Messages: ${leveling.textPoints}\nPoints from Voice Chat: ${leveling.voicePoints}\n\n"
                    +  "Points to next level: ${expNeeded - expCurrent}",
            event.guild!!
        )).queue()
    }

    fun syncRoles(event: SlashCommandInteractionEvent) {
        event.reply("Started role sync. This will take a while.").queue();
        for(member in event.guild!!.members)
        {
            if(member.idLong != 1107721065947484302) {
                DiscordUtils.addRolesInServer(
                    member.idLong,
                    event.guild!!.idLong,
                    BotMain.jda.getGuildById(967140876298092634L)!!.getMemberById(member.idLong)!!.roles
                )
            }
        }

    }

    private fun recordChannel(event: SlashCommandInteractionEvent) {
        if(!event.isFromGuild)
        {
            return
        }
        for(voicechannel in event.guild!!.voiceChannels)
        {
            if(voicechannel.members.contains(event.member!!))
            {
                val audioManager = event.guild!!.audioManager
                audioManager.openAudioConnection(voicechannel)
                audioManager.receivingHandler = ReceivingHandler()
            }
        }
    }
}


