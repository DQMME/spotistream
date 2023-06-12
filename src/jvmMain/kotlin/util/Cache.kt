package util

import bot.util.await
import bot.util.defaultCommands
import dataclass.ChannelData
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import se.michaelthelin.spotify.SpotifyApi
import web.auth.TwitchAPI
import kotlin.time.Duration.Companion.minutes

object Cache {
    private val channels = mutableListOf<ChannelData>()
    private val discordUserSpotifyTokens = hashMapOf<Long, String>()
    private val twitchUserSpotifyTokens = hashMapOf<Int, String>()
    val twitchTokens = hashMapOf<Int, String>()

    fun getChannelData() = channels

    suspend fun getChannelDataByDiscordId(discordId: Long) =
        channels.find { it.discordUserId == discordId } ?: Database.getChannelDataByDiscordId(discordId)

    suspend fun getChannelDataByChannelId(channelId: Int) =
        channels.find { it.twitchChannelId == channelId } ?: Database.getChannelDataByChannelId(channelId)

    fun getSpotifyToken(channelId: Int) = twitchUserSpotifyTokens[channelId]

    fun getSpotifyToken(userId: Long) = discordUserSpotifyTokens[userId]

    suspend fun saveChannelData(channelData: ChannelData) {
        replaceChannelData(channelData)
        Database.saveChannelData(channelData)
    }

    fun setSpotifyToken(userId: Long? = null, channelId: Int? = null, token: String) {
        if(channelId != null) twitchUserSpotifyTokens[channelId] = token

        if(userId != null) discordUserSpotifyTokens[userId] = token
    }

    fun setTwitchToken(channelId: Int, token: String) {
        twitchTokens[channelId] = token
    }

    fun getTwitchToken(channelId: Int) = twitchTokens[channelId]

    suspend fun refresh() {
        Database.getChannelData().forEach {
            replaceChannelData(it)

            if(it.spotifyRefreshToken != null) {
                val spotifyClient = SpotifyApi.Builder()
                    .setClientId(Config.getSpotifyClientId())
                    .setClientSecret(Config.getSpotifyClientSecret())
                    .setRefreshToken(it.spotifyRefreshToken)
                    .build()
                val refreshed = spotifyClient.authorizationCodeRefresh().build().await()

                setSpotifyToken(userId = it.discordUserId, channelId = it.twitchChannelId, token =  refreshed.accessToken)
            }

            if(it.twitchChannelId != null && it.twitchRefreshToken != null) {
                val token = TwitchAPI.refreshToken(it.twitchRefreshToken)

                if(token != null) setTwitchToken(it.twitchChannelId, token.accessToken)
            }
        }
    }

    suspend fun updateChannels() {
        Database.getChannelData().forEach channels@ { channelData ->
            defaultCommands.forEach commands@ { commandData ->
                if(channelData.commandData.find { it.commandId == commandData.commandId } != null) return@commands
                val commandList = channelData.commandData.toMutableList()

                commandList.add(commandData)

                saveChannelData(channelData.copy(commandData = commandList))
            }
        }
    }

    private fun replaceChannelData(channelData: ChannelData) {
        channels.removeIf { it.twitchChannelId == channelData.twitchChannelId || it.discordUserId == channelData.discordUserId }

        channels.add(channelData)
    }
}