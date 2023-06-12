package bot.command

import bot.SpotistreamBot
import bot.util.onMessage
import bot.util.userLevel
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import dataclass.CommandData
import util.Cache

abstract class AbstractCommand {
    abstract val id: String

    abstract suspend fun execute(event: ChannelMessageEvent, data: CommandData, args: List<String>)

    fun register() {
        SpotistreamBot.eventHandler.onMessage {
            val channelData = Cache.getChannelDataByChannelId(it.channel.id.toInt()) ?: return@onMessage
            val commandData = channelData.getCommandData(id) ?: return@onMessage
            val message = it.message
            if (!message.lowercase().startsWith(commandData.usage.lowercase())) return@onMessage
            if(it.userLevel().level < commandData.requiredLevel.level) return@onMessage

            val args = message.split(" ").subList(1, message.split(" ").size)

            execute(it, commandData, args)
        }
    }
}