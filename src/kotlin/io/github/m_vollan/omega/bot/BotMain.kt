package io.github.m_vollan.omega.bot

import ch.qos.logback.classic.Logger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationMap
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.CommandEditAction

object BotMain {
    val bot_name = "Omega"
    lateinit var jda: JDA;
    lateinit var logger: Logger
    fun run()
    {
        logger.info("Adding slash commands")
        jda.addEventListener(SlashCommandListenerAdapter())
        jda.upsertCommand("ping", "Ping the bot").queue()
        jda.upsertCommand(Commands.slash("echo", "Repeats what you said")
            .addOption(OptionType.STRING, "message", "the message to say", true, false)
        ).complete()
        jda.upsertCommand(Commands.slash("give_role", "Gives a role to a user")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
            .addOption(OptionType.ROLE,"role","The role to give", true, false)
            .addOption(OptionType.USER, "user", "The user to give a role to", true, false)
        ).complete()
        jda.upsertCommand(Commands.slash("give_all_role", "Give all members a role")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
            .addOption(OptionType.ROLE, "role", "The role to give", true, false)
        ).complete()
        jda.upsertCommand(Commands.slash("give_all_role_restricted", "Give all members a role if they don't have another role")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
            .addOption(OptionType.ROLE, "role", "The role to give out", true, false)
            .addOption(OptionType.ROLE, "role2", "1st restriction", true, false)
            .addOption(OptionType.ROLE, "role3", "2nd restriction", false, false)
            .addOption(OptionType.ROLE, "role4", "3rd restriction", false, false)
            .addOption(OptionType.ROLE, "role5", "4th restriction", false, false)
        ).complete()
        jda.upsertCommand(Commands.slash("test_give_all_role", "Give all members a role")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
            .addOption(OptionType.ROLE, "role", "The role to give", true, false)
        ).complete()
        jda.upsertCommand(Commands.slash("test_give_all_role_restricted", "Give all members a role if they don't have another role")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
            .addOption(OptionType.ROLE, "role", "The role to give out", true, false)
            .addOption(OptionType.ROLE, "role2", "1st restriction", true, false)
            .addOption(OptionType.ROLE, "role3", "2nd restriction", false, false)
            .addOption(OptionType.ROLE, "role4", "3rd restriction", false, false)
            .addOption(OptionType.ROLE, "role5", "4th restriction", false, false)
        ).complete()
        jda.upsertCommand(Commands.slash("suggest", "Suggest a new thing for the server to add")
            .setGuildOnly(true)
            .addOption(OptionType.STRING, "suggestion", "The suggestion",true, false)
        ).complete()
        logger.info("Finished adding slash commands")
    }

}