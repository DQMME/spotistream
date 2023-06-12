package bot.util

import com.github.philippheuer.events4j.api.domain.IDisposable
import com.github.philippheuer.events4j.api.service.IEventHandler
import com.github.philippheuer.events4j.core.EventManager
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.twitch4j.chat.ITwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import com.netflix.hystrix.HystrixCommand
import dataclass.CommandData
import dataclass.UserLevel
import hu.akarnokd.rxjava.interop.RxJavaInterop
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitFirst
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.requests.AbstractRequest
import util.ioScope

suspend fun <R> HystrixCommand<R>.executeAsync(): R {
    val v1Observable = observe()
    val v2Observable = RxJavaInterop.toV2Observable(v1Observable)

    return v2Observable.awaitFirst()
}

inline fun <reified EH : IEventHandler> EventManager.getEventHandler(): EH = getEventHandler(EH::class.java)

inline fun <reified E> IEventHandler.onEvent(noinline handler: (E) -> Unit): IDisposable =
    onEvent(E::class.java, handler)

suspend fun <T> AbstractRequest<T>.await(): T = executeAsync().await()

fun ChannelMessageEvent.reply(message: String) = reply(twitchChat, message)

fun SimpleEventHandler.onMessage(event: suspend (ChannelMessageEvent) -> Unit) {
    onEvent<ChannelMessageEvent> {
        ioScope.launch {
            event.invoke(it)
        }
    }
}

fun SimpleEventHandler.onRewardRedeemed(event: suspend (RewardRedeemedEvent) -> Unit) {
    onEvent<RewardRedeemedEvent> {
        ioScope.launch {
            event.invoke(it)
        }
    }
}

fun String.toSpotifyId(): String? {
    val spotifyUrlRegex =
        Regex("https?://(www\\.)?open.spotify{1,256}\\.com/track{1,6}\\b([-a-zA-Z\\d()@:%_+.~#?&/=]*)")

    val matchResult = spotifyUrlRegex.find(this) ?: return null

    val spotifyUrlId = matchResult.value
        .split("?")[0]
        .replace("https", "")
        .replace("http", "")
        .replace("://", "")
        .replace("open.spotify.com", "")
        .replace("open.spotify.com", "")
        .replace("/", "")
        .replace("track", "")

    return "spotify:track:$spotifyUrlId"
}

suspend fun queueSong(id: String, query: String, response: String, accessToken: String): String {
    val spotifyClient = SpotifyApi.Builder().setAccessToken(accessToken).build()

    try {
        val songId = id.toSpotifyId()

        if (songId != null) {
            spotifyClient.addItemToUsersPlaybackQueue(songId).build().await()

            val song = spotifyClient.getTrack(songId.replace("spotify:track:", "")).build().await()

            return response
                .replace("%NAME%", song.name)
                .replace("%ARTIST%", song.artists[0].name)
                .replace("%LINK%", song.externalUrls["spotify"])
        }

        val song = spotifyClient.searchTracks(query).build().await().items.firstOrNull()
            ?: return "Kein passender Song gefunden."

        spotifyClient.addItemToUsersPlaybackQueue(song.uri).build().await()

        return response
            .replace("%NAME%", song.name)
            .replace("%ARTIST%", song.artists[0].name)
            .replace("%LINK%", song.externalUrls["spotify"])

    } catch (_: Exception) {
        return "Das hat nicht geklappt."
    }
}

fun ChannelMessageEvent.userLevel(): UserLevel {
    var userLevel = UserLevel.USER
    val badges = messageEvent.badges

    if (badges.containsKey("vip")) userLevel = UserLevel.VIP
    if (badges.containsKey("moderator")) userLevel = UserLevel.MODERATOR
    if (badges.containsKey("broadcaster")) userLevel = UserLevel.BROADCASTER

    return userLevel
}

val defaultCommands = mutableListOf(
    CommandData(
        "pause",
        "!pause",
        "Die Wiedergabe wurde pausiert.",
        listOf(),
        UserLevel.MODERATOR
    ),
    CommandData(
        "playlist",
        "!playlist",
        "Gerade läuft die Playlist \"%NAME%\" von \"%OWNER%\"! (\"%LINK%\")",
        listOf("%NAME%", "%OWNER%", "%LINK%"),
        UserLevel.USER
    ),
    CommandData(
        "previous",
        "!previous",
        "Der vorherige Song wurde gestartet.",
        listOf(),
        UserLevel.MODERATOR
    ),
    CommandData(
        "queue",
        "!queue",
        "Der Song \"%NAME%\" von \"%ARTIST%\" (\"%LINK%\") wurde in die Warteschlange hinzugefügt.",
        listOf("%NAME%", "%ARTIST%", "%LINK%"),
        UserLevel.MODERATOR
    ),
    CommandData(
        "resume",
        "!resume",
        "Die Wiedergabe wurde gestartet.",
        listOf(),
        UserLevel.MODERATOR
    ),
    CommandData(
        "skip",
        "!skip",
        "Der nächste Song wurde gestartet.",
        listOf(),
        UserLevel.MODERATOR
    ),
    CommandData(
        "song",
        "!song",
        "Gerade läuft \"%NAME%\" von \"%ARTIST%\"! (\"%LINK%\")",
        listOf("%NAME%", "%ARTIST%", "%LINK%"),
        UserLevel.USER
    ),
    CommandData(
        "volume",
        "!volume",
        "Du hast die Lautstärke auf %VOLUME% gesetzt.",
        listOf("%VOLUME%"),
        UserLevel.MODERATOR
    )
)