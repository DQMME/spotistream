package bot.command.impl

import bot.command.AbstractCommand
import bot.util.await
import bot.util.reply
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import dataclass.CommandData
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.enums.ModelObjectType
import util.Cache

object PlaylistCommand : AbstractCommand() {
    override val id = "playlist"

    override suspend fun execute(event: ChannelMessageEvent, data: CommandData, args: List<String>) {
        val accessToken = Cache.getSpotifyToken(event.channel.id.toInt()) ?: return
        val spotifyClient = SpotifyApi.Builder().setAccessToken(accessToken).build()

        try {
            val currentPlayback = spotifyClient.informationAboutUsersCurrentPlayback.build().await()

            if (currentPlayback == null || currentPlayback.context.type != ModelObjectType.PLAYLIST) {
                event.reply("Gerade läuft keine Playlist!")
                return
            }

            val playlist = spotifyClient.getPlaylist(
                currentPlayback.context.href.replace("https://api.spotify.com/v1/playlists/", "")
            ).build().await()

            event.reply(
                data.response
                    .replace("%NAME%", playlist.name)
                    .replace("%OWNER%", playlist.owner.displayName)
                    .replace("%LINK%", playlist.externalUrls["spotify"])
            )
        } catch (_: Exception) {
            event.reply("Das hat nicht geklappt.")
        }
    }
}