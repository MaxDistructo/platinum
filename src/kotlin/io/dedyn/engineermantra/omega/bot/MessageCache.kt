package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.Main
import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import java.time.Clock
import kotlin.collections.HashMap

typealias Snowflake = Long


class MessageCache{
    class CachedMessage(message: Message){
        val messageID: Snowflake
        var content: String
        var lastEdited: Long
        var isPinned: Boolean = false
        var author: Snowflake

        init{
            messageID  = message.idLong
            content = message.contentRaw
            lastEdited = Clock.systemUTC().instant().toEpochMilli()
            isPinned = message.isPinned
            author = message.author.idLong
        }
    }

    private val backend: HashMap<Snowflake, CachedMessage> = hashMapOf()
    private var limit = 250000
    private val scope = Main.BotScope()
    private val ioScope = Main.BotScope()
    private var isLocked: Boolean = false
    init {
        scope.launch{
            //If the size is too big, lets start deleting old cached messages
            if(backend.size > limit)
            {
                pruneCache()
            }
        }
    }

    fun add(message: Message)
    {
        ioScope.launch{
            if(isLocked)
            {
                Thread.sleep(1)
            }
            else{
                backend[message.idLong] = CachedMessage(message)
            }
        }
    }
    fun remove(messageID: Long)
    {
        ioScope.launch{
            if(isLocked)
            {
                Thread.sleep(1)
            }
            else {
                backend.remove(messageID)
            }
        }
    }
    fun remove(message: Message)
    {
        ioScope.launch{
            if(isLocked)
            {
                Thread.sleep(1)
            }
            else {
                backend.remove(message.idLong)
            }
        }
    }
    fun get(messageId: Long): CachedMessage?
    {
         if(isLocked){
             return null
         }
         else
         {
             val potential = backend[messageId] ?: return null
             return potential
         }
    }
    fun get(message: Message): CachedMessage?
    {
        if(isLocked){
            return null
        }
        else
        {
            val potential = backend[message.idLong] ?: return null
            return potential
        }
    }
    fun setCacheLimit(size: Int){
        limit = size
    }
    fun destroy() {
        scope.cancel()
    }

    public fun pruneCache()
    {
        isLocked = true
        val deleteQueue = mutableListOf<Long>()
        for(message in backend)
        {
            //If the current time is the time of last edit +30 days and is not pinned, remove it from the cache.
            if(Clock.systemUTC().instant().toEpochMilli() > (message.value.lastEdited + 2592000000) && !message.value.isPinned){
                deleteQueue.add(message.key)
            }
            //Assuming that JDA's user cache is within policy, if we cannot find the user we assume they have been deleted
            //or are otherwise out of scope spec so per retention policy we remove any message attributed to them.
            if(BotMain.jda.getUserById(message.value.author) == null)
            {
                deleteQueue.add(message.key)
            }
        }
        for(element in deleteQueue)
        {
            backend.remove(element)
        }
        isLocked = false
    }
}

