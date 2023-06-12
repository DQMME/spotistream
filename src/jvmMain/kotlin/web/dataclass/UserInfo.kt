package web.dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    @SerialName("discord_user") val discordUser: DiscordUser,
    @SerialName("authorized_features") val authorizedFeatures: MutableList<Feature> = mutableListOf()
)
