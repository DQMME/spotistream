package bot.command.impl

import bot.command.AbstractCommand
import bot.util.await
import bot.util.reply
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import dataclass.CommandData
import se.michaelthelin.spotify.SpotifyApi
import util.Cache

object PauseCommand : AbstractCommand() {
    override val id = "pause"

    override suspend fun execute(event: ChannelMessageEvent, data: CommandData, args: List<String>) {
        val accessToken = Cache.getSpotifyToken(event.channel.id.toInt()) ?: return
        val spotifyClient = SpotifyApi.Builder().setAccessToken(accessToken).build()

        try {
            spotifyClient.pauseUsersPlayback().build().await()

            event.reply(data.response)
        } catch (_: Exception) {
            event.reply("Das hat nicht geklappt.")
        }
    }
}