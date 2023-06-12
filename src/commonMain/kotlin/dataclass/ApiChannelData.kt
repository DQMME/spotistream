package dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiChannelData(
    @SerialName("_id") val discordUserId: Long,
    @SerialName("twitch_channel_id") val twitchChannelId: Int? = null,
    @SerialName("twitch_channel_name") val twitchChannelName: String? = null,
    @SerialName("has_linked_spotify") val hasLinkedSpotify: Boolean? = false,
    @SerialName("has_linked_twitch_redemptions") val hasLinkedTwitchRedemptions: Boolean? = false,
    @SerialName("spotify_email") val spotifyEmail: String? = null,
    @SerialName("command_data") val commandData: MutableList<CommandData>,
    @SerialName("point_rewards") val pointRewards: MutableList<PointReward>
)
