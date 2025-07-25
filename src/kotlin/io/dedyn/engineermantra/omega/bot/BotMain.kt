package io.dedyn.engineermantra.omega.bot

import ch.qos.logback.classic.Logger
import io.dedyn.engineermantra.omega.shared.TimerThread
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.mariuszgromada.math.mxparser.License

object BotMain {
    val imageProcessing: Boolean = false
    val bot_name = "Omega"
    lateinit var jda: JDA
    lateinit var logger: Logger
    val messageCache = MessageCache()
    val voiceCache = mutableMapOf<Long, Long>()
    val auditThread = AuditThread()
    val timerThread = TimerThread()
    var managerStoryteller: Long = 0L

    fun run()
    {
        License.iConfirmNonCommercialUse("acbdef")
        logger.info("Adding slash commands")
        jda.addEventListener(SlashCommandListenerAdapter())
        jda.addEventListener(LoggerListenerAdapter())
        jda.addEventListener(SCListenerAdapter())
        jda.addEventListener(LevelingListenerAdapter())
        jda.addEventListener(CountingListenerAdapter())
        //Parse the known commands that we have and register any new ones but not do the old
        val commandNames = mutableListOf<String>()
        val commands = jda.retrieveCommands().complete()
        //Get rid of old commands from the Bot
        for(command in commands)
        {
            when (command.name) {
                "record" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                "poll" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                "vote" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                "ping" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                "echo" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                "give_role" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                "editstrike" -> {
                    jda.deleteCommandById(command.id).queue();
                }
                else -> {
                    commandNames.add(command.name)
                }
            }
        }
        if(!commandNames.contains("give_all_role")) {
            jda.upsertCommand(
                Commands.slash("give_all_role", "Give all members a role")
                    .setContexts(InteractionContextType.GUILD)
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
        if(!commandNames.contains("role")) {
            jda.upsertCommand(
                Commands.slash("role", "Set or create your booster role. If you're not a booster, this will not work.")
                    .setContexts(InteractionContextType.GUILD)
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
                    .addSubcommands(
                        SubcommandData("issue", "Issue a strike")
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
                            ),
                        SubcommandData("edit", "Edit a previously issued strike")
                            .addOption(OptionType.INTEGER, "strikeid", "The ID number of the strike to modify", true, false)
                            .addOption(OptionType.STRING, "reason", "The reason for this strike", false, false)
                            .addOption(OptionType.INTEGER, "points", "Edit the number of points this strike counts for.")
                    )
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_EVENTS))
                    .setContexts(InteractionContextType.GUILD)

            ).complete()
        }
        if(!commandNames.contains("strikes")) {
            jda.upsertCommand(
                Commands.slash("strikes", "Check the strikes against yourself or someone else")
                    .setContexts(InteractionContextType.GUILD)
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
        if(!commandNames.contains("audit")) {
            jda.upsertCommand(
                Commands.slash("audit", "Audit everything that the bot manages. (Booster perks, etc)")
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            ).complete()
        }
        if(!commandNames.contains("report")){
            jda.upsertCommand(
                Commands.slash("report", "Report an issue with the application")
                    .addSubcommands(
                        SubcommandData("add", "Report a new issue")
                            .addOption(
                                OptionType.STRING,
                                "message",
                                "Please describe your issue with the application or the TOS violation you believe has occurred.",
                                true,
                                false
                            )
                            .addOption(
                                OptionType.STRING,
                                "action",
                                "What action would you like to have taken?",
                                true,
                                false
                            )
                            .addOption(
                                OptionType.BOOLEAN,
                                "reply",
                                "Can ${jda.retrieveApplicationInfo().complete().owner.name} contact you in relation to this issue?",
                                true,
                                false
                            )
                    )

            ).complete()
        }
        /**
        if(!commandNames.contains("poll")) {
            jda.upsertCommand(
                Commands.slash("poll", "Create a poll")
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .setContexts(InteractionContextType.GUILD)
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
                    .setContexts(InteractionContextType.GUILD)
                    .addOption(OptionType.STRING, "prompt", "What do you want to ask about?", true, false)
            ).complete()
        }
        */
        if(!commandNames.contains("level")) {
            jda.upsertCommand(
                Commands.slash("level", "View your level")
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .setContexts(InteractionContextType.GUILD)
                    .addOption(OptionType.USER, "user", "The user to get the level for. Defaults to yourself.", false, false)
            ).complete()
        }
        //REMEMBER!!!! COMMAND NAMES MUST BE LOWER CASE!
        if(!commandNames.contains("migrate_user")) {
            jda.upsertCommand(
                Commands.slash("migrate_user", "Migrate user")
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                    .setContexts(InteractionContextType.GUILD)
                    .addOption(OptionType.USER, "src_user", "The user to get the roles and stats from", true, false)
                    .addOption(OptionType.USER, "dest_user", "The user to give the roles and stats to", true, false)
            ).complete()
        }
        if(!commandNames.contains("top"))
        {
            jda.upsertCommand(
                Commands.slash("top", "Display the top 10 people by level in the server")
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .addOption(OptionType.INTEGER, "page", "Page number to display?")
            ).complete()
        }

        jda.getGuildById(1165357291629989979)!!.upsertCommand(
            Commands.slash("sync", "Sync roles from Salem Central")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .setContexts(InteractionContextType.GUILD)
        ).complete()
        jda.getGuildById(1165357291629989979)!!.upsertCommand(
            Commands.slash("goodnight", "Sends members to SLEEP")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .setContexts(InteractionContextType.GUILD)
        ).complete()
        jda.getGuildById(1165357291629989979)!!.upsertCommand(
            Commands.slash("goodmorning", "Wakes everyone up")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .setContexts(InteractionContextType.GUILD)
        ).complete()
        jda.getGuildById(1165357291629989979)!!.upsertCommand(
            Commands.slash("summon", "Summon all VC members")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .setContexts(InteractionContextType.GUILD)
        ).complete()
        jda.getGuildById(1165357291629989979)!!.upsertCommand(
            Commands.slash("goto", "Goto Person in VC")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .setContexts(InteractionContextType.GUILD)
                .addOption(OptionType.USER, "user", "The user to go to")
        ).complete()
        jda.getGuildById(1165357291629989979)!!.upsertCommand(
            Commands.slash("promote", "Promote a user to Storyteller")
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .setContexts(InteractionContextType.GUILD)
                .addOption(OptionType.USER, "user", "The user to go to", false)
                .addOption(OptionType.BOOLEAN, "force", "Force yourself to become primary", false)
        ).complete()
        logger.info("Finished adding slash commands ")
        auditThread.start()
        timerThread.start()
    }

    class AuditThread: Thread(){
        override fun run(){
            while(true){
                Auditing.runAuditing(jda)
                //We need to run this daily so that we meet Discord's 30-day retention policy
                messageCache.pruneCache()
                sleep(86400000L)
            }
        }
    }

}