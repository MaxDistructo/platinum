package io.github.m_vollan.omega.bot

import ch.qos.logback.classic.Logger
import net.dv8tion.jda.api.JDA

object BotMain {
    val bot_name = "Omega"
    lateinit var jda: JDA;
    lateinit var logger: Logger
    fun run()
    {
        logger.info("Adding slash commands")
        jda.addEventListener(SlashCommandListenerAdapter())
        jda.upsertCommand("ping", "Ping the bot").queue()
    }

}