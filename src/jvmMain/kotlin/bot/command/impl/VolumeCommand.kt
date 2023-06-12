package bot.command.impl

import bot.command.AbstractCommand
import bot.util.await
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import dataclass.CommandData
import se.michaelthelin.spotify.SpotifyApi
import util.Cache
import bot.util.reply

object VolumeCommand : AbstractCommand() {
    override val id = "volume"

    override suspend fun execute(event: ChannelMessageEvent, data: CommandData, args: List<String>) {
        val accessToken = Cache.getSpotifyToken(event.channel.id.toInt()) ?: return
        val spotifyClient = SpotifyApi.Builder().setAccessToken(accessToken).build()

        try {
            val volume = args.getOrNull(0)?.replace("%", "")?.toIntOrNull()

            if (volume == null || volume < 1 || volume > 100) {
                event.reply("Verwendung: !volume <1-100>%")
                return
            }

            spotifyClient.setVolumeForUsersPlayback(volume).build().await()

            event.reply(data.response.replace("%VOLUME%", "$volume%"))
        } catch (_: Exception) {
            event.reply("Das hat nicht geklappt.")
        }
    }
}