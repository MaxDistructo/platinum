package io.dedyn.engineermantra.omega

import ch.qos.logback.classic.Logger
import io.dedyn.engineermantra.omega.bot.BotMain
import io.dedyn.engineermantra.omega.shared.ConfigFileJson
import io.dedyn.engineermantra.omega.website.WebMain
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.Compression
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object Main {
    class WebsiteScope: CoroutineScope{
        override val coroutineContext: CoroutineContext
            get() = EmptyCoroutineContext
    }
    class BotScope: CoroutineScope{
        override val coroutineContext: CoroutineContext
            get() = EmptyCoroutineContext
    }
    @JvmStatic
    fun main(args: Array<String>)
    {
        BotMain.logger = LoggerFactory.getLogger("Omega") as Logger

        BotMain.logger.info("Setting up JDA")
        val builder: JDABuilder = JDABuilder.create(ConfigFileJson.getToken(), GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES)
            .setBulkDeleteSplittingEnabled(false)
            .setCompression(Compression.NONE)
            .setActivity(Activity.playing("Use /help to get commands"))

        BotMain.logger.info("JDA Init")
        BotMain.jda = builder.build()
        BotMain.jda.awaitReady()
        BotMain.logger.info("JDA Init Complete")

        val websiteScope = WebsiteScope()
        val botScope = BotScope()

        //Now that we have a long-running process, run the WebMain
        websiteScope.launch{
            BotMain.logger.info("Starting up website")
            WebMain.run();
        }
        //And follow with the bot main.
        botScope.launch {
            BotMain.run()
        }

    }
}