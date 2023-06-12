package bot.command.impl

import bot.command.AbstractCommand
import bot.util.await
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import dataclass.CommandData
import se.michaelthelin.spotify.SpotifyApi
import util.Cache
import bot.util.reply

object SkipCommand : AbstractCommand() {
    override val id = "skip"

    override suspend fun execute(event: ChannelMessageEvent, data: CommandData, args: List<String>) {
        val accessToken = Cache.getSpotifyToken(event.channel.id.toInt()) ?: return
        val spotifyClient = SpotifyApi.Builder().setAccessToken(accessToken).build()

        try {
            spotifyClient.skipUsersPlaybackToNextTrack().build().await()

            event.reply(data.response)
        } catch (_: Exception) {
            event.reply("Das hat nicht geklappt.")
        }
    }
}