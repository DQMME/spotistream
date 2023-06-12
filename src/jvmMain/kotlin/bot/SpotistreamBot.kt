package bot

import bot.command.impl.PauseCommand
import bot.command.impl.PlaylistCommand
import bot.command.impl.PreviousCommand
import bot.command.impl.QueueCommand
import bot.command.impl.ResumeCommand
import bot.command.impl.SkipCommand
import bot.command.impl.SongCommand
import bot.command.impl.VolumeCommand
import bot.reward.RewardManager
import bot.util.executeAsync
import bot.util.getEventHandler
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import util.Cache
import util.Config
import web.auth.TwitchAPI

object SpotistreamBot {
    lateinit var twitchClient: TwitchClient
    lateinit var eventHandler: SimpleEventHandler

    suspend fun start() {
        twitchClient = TwitchClientBuilder.builder()
            .withClientId(Config.getTwitchClientId())
            .withClientSecret(Config.getTwitchClientSecret())
            .withChatAccount(OAuth2Credential("twitch", Config.getTwitchAccessToken()))
            .withEnableHelix(true)
            .withEnableChat(true)
            .withEnablePubSub(true)
            .build()

        joinChannels()

        eventHandler = twitchClient.eventManager.getEventHandler()

        registerCommands()

        RewardManager.register()
    }

    private suspend fun joinChannels() {
        Cache.getChannelData().forEach {
            if (it.twitchChannelId == null) return@forEach

            val user =
                twitchClient.helix.getUsers(
                    Config.getTwitchAccessToken(),
                    mutableListOf(it.twitchChannelId.toString()),
                    null
                )
                    .executeAsync()
                    .users
                    .firstOrNull() ?: return@forEach

            twitchClient.chat.joinChannel(user.login)

            if (it.twitchRefreshToken == null) return@forEach

            val refreshedToken = TwitchAPI.refreshToken(it.twitchRefreshToken) ?: return@forEach

            twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
                OAuth2Credential(
                    "twitch",
                    refreshedToken.accessToken
                ), it.twitchChannelId.toString()
            )
        }
    }

    private fun registerCommands() {
        PauseCommand.register()
        PlaylistCommand.register()
        PreviousCommand.register()
        QueueCommand.register()
        ResumeCommand.register()
        SkipCommand.register()
        SongCommand.register()
        VolumeCommand.register()
    }
}