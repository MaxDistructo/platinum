package io.github.m_vollan.omega

import ch.qos.logback.classic.Logger
import io.github.m_vollan.omega.bot.BotMain
import io.github.m_vollan.omega.bot.SlashCommandListenerAdapter
import io.github.m_vollan.omega.website.WebMain
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.Compression
import org.slf4j.LoggerFactory
import kotlin.text.*

object Main {
    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun main(args: Array<String>)
    {
        BotMain.logger = LoggerFactory.getLogger("Omega") as Logger

        //token, intents
        BotMain.logger.info("Setting up JDA")
        val builder: JDABuilder = JDABuilder.create("", GatewayIntent.GUILD_MEMBERS);
        builder.setBulkDeleteSplittingEnabled(false)
        builder.setCompression(Compression.NONE)
        builder.setActivity(Activity.playing("Starting up...."))

        BotMain.logger.info("JDA Init")
        BotMain.jda = builder.build()
        BotMain.jda.awaitReady()
        BotMain.logger.info("JDA Init Complete")

        BotMain.logger.info("Adding slash commands")
        BotMain.jda.addEventListener(SlashCommandListenerAdapter())
        BotMain.jda.upsertCommand("ping", "Ping the bot").queue()

    }
}