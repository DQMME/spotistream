package bot.reward

import bot.SpotistreamBot
import bot.util.await
import bot.util.executeAsync
import bot.util.onRewardRedeemed
import bot.util.queueSong
import dataclass.RewardAction
import se.michaelthelin.spotify.SpotifyApi
import util.Cache
import util.Config

object RewardManager {
    fun register() {
        SpotistreamBot.eventHandler.onRewardRedeemed { event ->
            val channelData =
                Cache.getChannelDataByChannelId(event.redemption.channelId.toInt()) ?: return@onRewardRedeemed
            val accessToken = Cache.getSpotifyToken(event.redemption.channelId.toInt()) ?: return@onRewardRedeemed
            val spotifyClient = SpotifyApi.Builder().setAccessToken(accessToken).build()
            val rewardId = event.redemption.reward.id
            val pointRewards = channelData.getPointRewards(rewardId)

            pointRewards.forEach {
                var response = it.response

                when (it.action) {
                    RewardAction.QUEUE_SONG -> {
                        response = try {
                            val args = event.redemption.userInput?.split(" ") ?: return@forEach

                            if (args.isEmpty()) return@forEach

                            queueSong(args[0], args.joinToString(" "), response, accessToken)
                        } catch (_: Exception) {
                            "Das hat nicht geklappt"
                        }
                    }

                    RewardAction.PAUSE_PLAYBACK -> {
                        response = try {
                            spotifyClient.pauseUsersPlayback().build().await()
                        } catch (_: Exception) {
                            "Das hat nicht geklappt"
                        }
                    }

                    RewardAction.RESUME_PLAYBACK -> {
                        response = try {
                            spotifyClient.startResumeUsersPlayback().build().await()
                        } catch (_: Exception) {
                            "Das hat nicht geklappt"
                        }
                    }

                    RewardAction.SKIP_SONG -> {
                        response = try {
                            spotifyClient.skipUsersPlaybackToNextTrack().build().await()
                        } catch (_: Exception) {
                            "Das hat nicht geklappt"
                        }
                    }

                    RewardAction.PREVIOUS_SONG -> {
                        response = try {
                            spotifyClient.skipUsersPlaybackToPreviousTrack().build().await()
                        } catch (_: Exception) {
                            "Das hat nicht geklappt"
                        }
                    }

                    RewardAction.SET_VOLUME -> {
                       try {
                            val args = event.redemption.userInput?.split(" ") ?: return@forEach

                            if (args.isEmpty()) return@forEach

                            val volume = args[0].replace("%", "").toIntOrNull()

                           response = if (volume == null || volume < 1 || volume > 100) {
                               "Verwendung: <1-100%>"
                           } else {
                               spotifyClient.setVolumeForUsersPlayback(volume).build().await()

                               response.replace("%VOLUME%", "$volume")
                           }
                        } catch (_: Exception) {
                            response = "Das hat nicht geklappt"
                        }
                    }
                }

                val channelName = SpotistreamBot.twitchClient.helix.getUsers(
                    Config.getTwitchAccessToken(),
                    listOf(event.redemption.channelId),
                    null
                ).executeAsync().users.firstOrNull()?.login

                if(channelName != null) {
                    SpotistreamBot.twitchClient.chat.sendMessage(channelName, response)
                }
            }
        }
    }
}