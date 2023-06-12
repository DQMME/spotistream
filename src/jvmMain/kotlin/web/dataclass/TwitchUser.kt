package web.dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitchUserResponse(
    val data: List<TwitchUser>
)

@Serializable
data class TwitchUser(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String
)
