package io.github.m_vollan.omega

import ch.qos.logback.classic.Logger
import io.github.m_vollan.omega.bot.BotMain
import io.github.m_vollan.omega.bot.SlashCommandListenerAdapter
import io.github.m_vollan.omega.shared.ConfigFile
import io.github.m_vollan.omega.website.WebMain
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.Compression
import org.slf4j.LoggerFactory

object Main {
    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun main(args: Array<String>)
    {
        BotMain.logger = LoggerFactory.getLogger("Omega") as Logger

        BotMain.logger.info("Setting up JDA")
        val builder: JDABuilder = JDABuilder.create(ConfigFile.getToken(), GatewayIntent.GUILD_MEMBERS)
            .setBulkDeleteSplittingEnabled(false)
            .setCompression(Compression.NONE)
            .setActivity(Activity.playing("Use /help to get commands"))

        BotMain.logger.info("JDA Init")
        BotMain.jda = builder.build()
        BotMain.jda.awaitReady()
        BotMain.logger.info("JDA Init Complete")

        //Now that we have a long-running process, run the WebMain
        GlobalScope.launch{
            BotMain.logger.info("Starting up website")
            WebMain.run();
        }

        //And follow with the bot main.
        GlobalScope.launch {
            BotMain.run()
        }

    }
}