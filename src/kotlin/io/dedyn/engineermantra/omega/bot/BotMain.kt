package io.dedyn.engineermantra.omega.bot

import ch.qos.logback.classic.Logger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

object BotMain {
    val bot_name = "Omega"
    lateinit var jda: JDA;
    lateinit var logger: Logger
    val messageCache = MessageCache()
    val voiceCache = mutableMapOf<Long, Long>()
    val auditThread = AuditThread()

    fun run()
    {
        logger.info("Adding slash commands")
        jda.addEventListener(SlashCommandListenerAdapter())
        jda.addEventListener(LoggerListenerAdapter())
        jda.addEventListener(SCListenerAdapter())
        jda.addEventListener(LevelingListenerAdapter())
        //Parse the known commands that we have and register any new ones but not do the old
        val commandNames = mutableListOf<String>()
        for(command in jda.retrieveCommands().complete())
        {
            commandNames.add(command.name)
        }

        if(!commandNames.contains("ping")) {
            jda.upsertCommand("ping", "Ping the bot").complete()
        }
        if(!commandNames.contains("echo")) {
            jda.upsertCommand(
                Commands.slash("echo", "Repeats what you said")
                    .addOption(OptionType.STRING, "message", "the message to say", true, false)
            ).complete()
        }
        if(!commandNames.contains("give_role")) {
            jda.upsertCommand(
                Commands.slash("give_role", "Gives a role to a user")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
                    .addOption(OptionType.ROLE, "role", "The role to give", true, false)
                    .addOption(OptionType.USER, "user", "The user to give a role to", true, false)
            ).complete()
        }
        if(!commandNames.contains("give_all_role")) {
            jda.upsertCommand(
                Commands.slash("give_all_role", "Give all members a role")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
                    .addOption(OptionType.ROLE, "role", "The role to give", true, false)
                    .addOption(OptionType.ROLE, "role2", "1st restriction", false, false)
                    .addOption(OptionType.ROLE, "role3", "2nd restriction", false, false)
                    .addOption(OptionType.ROLE, "role4", "3rd restriction", false, false)
                    .addOption(OptionType.ROLE, "role5", "4th restriction", false, false)
                    .addOption(
                        OptionType.BOOLEAN,
                        "test",
                        "Enable test mode to calculate without actually doing the role add",
                        false,
                        false
                    )
            ).complete()
        }
        if(!commandNames.contains("set")) {
            jda.upsertCommand(
                Commands.slash("set", "Set a config value")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .addSubcommands(
                        SubcommandData("suggestion_channel", "Set the suggestion channel")
                            .addOption(
                                OptionType.CHANNEL,
                                "channel",
                                "The channel to set as the suggestion channel",
                                true,
                                false
                            )
                    )
            ).complete()
        }
        if(!commandNames.contains("role")) {
            jda.upsertCommand(
                Commands.slash("role", "Set or create your booster role. If you're not a booster, this will not work.")
                    .setGuildOnly(true)
                    //If you can manage roles, you can just create this manually anyways.
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
                    .addOption(
                        OptionType.STRING,
                        "color",
                        "The color to set. This should be in hex format.",
                        true,
                        false
                    )
                    .addOption(
                        OptionType.STRING,
                        "icon_url",
                        "The URL to the Icon you want the bot to set",
                        false,
                        false
                    )
            ).complete()
        }
        if(!commandNames.contains("strike")) {
            jda.upsertCommand(
                Commands.slash("strike", "Issue a strike for a user")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_EVENTS))
                    .addOption(OptionType.STRING, "strike_type", "The type of strike to issue", true, true)
                    .addOption(OptionType.USER, "user", "The user to issue a strike against", true, false)
                    .addOption(OptionType.STRING, "reason", "A reason for the strike being issued", false, false)
                    .addOption(
                        OptionType.INTEGER,
                        "points",
                        "The amount of points to assign this strike. 1-4",
                        false,
                        false
                    )
                    .addOption(
                        OptionType.BOOLEAN,
                        "send_dm",
                        "Send a DM for this strike? (Default: true)",
                        false,
                        false
                    )
            ).complete()
        }
        if(!commandNames.contains("strikes")) {
            jda.upsertCommand(
                Commands.slash("strikes", "Check the strikes against yourself or someone else")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .addOption(OptionType.USER, "user", "The person to check strikes for", false, false)
                    .addOption(
                        OptionType.BOOLEAN,
                        "public",
                        "Show this in the current channel or in DMs?",
                        false,
                        false
                    )
            ).complete()
        }
        if(!commandNames.contains("editstrike")) {
            jda.upsertCommand(
                Commands.slash("editstrike", "Update a previously issued strike")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
                    .addOption(OptionType.INTEGER, "strikeid", "The ID number of the strike to modify", true, false)
                    .addOption(OptionType.STRING, "reason", "The reason for this strike", false, false)
                    .addOption(OptionType.INTEGER, "points", "Edit the number of points this strike counts for.")
            ).complete()
        }
        if(!commandNames.contains("audit")) {
            jda.upsertCommand(
                Commands.slash("audit", "Audit everything that the bot manages. (Booster perks, etc)")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            ).complete()
        }
        if(!commandNames.contains("username")) {
            jda.upsertCommand(
                Commands.slash(
                    "username",
                    "Show the known usernames for a member. If a game is not provided, this will show all known usernames"
                )
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .addOption(OptionType.USER, "user", "What user would you like to see usernames for", true, false)
                    .addOption(
                        OptionType.STRING,
                        "game",
                        "What game do you want to see the username from?",
                        false,
                        true
                    )
            ).complete()
        }
        if(!commandNames.contains("setusername")) {
            jda.upsertCommand(
                Commands.slash("setusername", "Set your username for a game")
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .addOption(OptionType.STRING, "username", "The username to set", true, false)
                    .addOption(OptionType.STRING, "game", "The game to set your username as", true, true)
            ).complete()
        }
        if(!commandNames.contains("poll")) {
            jda.upsertCommand(
                Commands.slash("poll", "Create a poll")
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .setGuildOnly(true)
                    .addOption(OptionType.STRING, "prompt", "What do you want to ask about?", true, false)
                    .addOption(OptionType.STRING, "option1", "Option 1", true, false)
                    .addOption(OptionType.STRING, "option2", "Option 2", true, false)
                    .addOption(OptionType.STRING, "option3", "Option 3", false, false)
                    .addOption(OptionType.STRING, "option4", "Option 4", false, false)
                    .addOption(OptionType.STRING, "option5", "Option 5", false, false)
                    .addOption(OptionType.STRING, "option6", "Option 6", false, false)
                    .addOption(OptionType.STRING, "option7", "Option 7", false, false)
                    .addOption(OptionType.STRING, "option8", "Option 8", false, false)
                    .addOption(OptionType.STRING, "option9", "Option 9", false, false)
                    .addOption(OptionType.STRING, "option10", "Option 10", false, false)
            ).complete()
        }
        if(!commandNames.contains("vote")) {
            jda.upsertCommand(
                Commands.slash("vote", "A poll with a yes/no option only")
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .setGuildOnly(true)
                    .addOption(OptionType.STRING, "prompt", "What do you want to ask about?", true, false)
            ).complete()
        }
        jda.getGuildById(967140876298092634)!!.upsertCommand(
            Commands.slash("agree", "Agree to the terms and conditions in this channel")
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                .setGuildOnly(true)
        ).complete()
        /*
        if(!commandNames.contains("md"))
        {
            jda.upsertCommand(
                Commands.slash("md", "Private Command")
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                    .setGuildOnly(false)
                    .addOption(OptionType.STRING, "function", "The function to run", true, false)
                    .addOption(OptionType.STRING, "args", "The arguments to pass to the function", true, false)
            ).complete()
        }
        */

        logger.info("Finished adding slash commands ")
        auditThread.start()
    }

    class AuditThread: Thread(){
        override fun run(){
            while(true){
                Auditing.runAuditing(jda)
                sleep(86400000L)
            }
        }
    }
}