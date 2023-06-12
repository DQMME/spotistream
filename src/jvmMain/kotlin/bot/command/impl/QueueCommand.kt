package bot.command.impl

import bot.command.AbstractCommand
import bot.util.queueSong
import bot.util.reply
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import dataclass.CommandData
import util.Cache


object QueueCommand : AbstractCommand() {
    override val id = "queue"

    override suspend fun execute(event: ChannelMessageEvent, data: CommandData, args: List<String>) {
        val accessToken = Cache.getSpotifyToken(event.channel.id.toInt()) ?: return

        event.reply(queueSong(args[0], args.joinToString(" "), data.response, accessToken))
    }
}