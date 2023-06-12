package dataclass

import bot.util.defaultCommands
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelData(
    @SerialName("_id") val discordUserId: Long,
    @SerialName("twitch_channel_id") val twitchChannelId: Int? = null,
    @SerialName("spotify_refresh_token") val spotifyRefreshToken: String? = null,
    @SerialName("twitch_refresh_token") val twitchRefreshToken: String? = null,
    @SerialName("command_data") val commandData: MutableList<CommandData> = defaultCommands,
    @SerialName("point_rewards") val pointRewards: MutableList<PointReward> = mutableListOf()
) {
    fun getCommandData(commandId: String) = commandData.find { it.commandId == commandId }

    fun getPointReward(id: String) = pointRewards.find { it.id == id }

    fun getPointRewards(twitchRewardId: String): MutableList<PointReward> {
        return pointRewards.stream()
            .filter { it.twitchRewardId == twitchRewardId }
            .toList()
    }
}