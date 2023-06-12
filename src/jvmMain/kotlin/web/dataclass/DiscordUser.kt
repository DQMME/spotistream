package web.dataclass

import kotlinx.serialization.Serializable

@Serializable
data class DiscordUser(
    val id: Long,
    val username: String,
    val discriminator: String,
    val avatar: String?,
    val email: String?
) {
    fun getAvatarUrl(size: Int = 512): String = "https://cdn.discordapp.com/avatars/$id/$avatar.png?size=$size"
}

@Serializable
data class DiscordUserString(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?
) {
    fun getAvatarUrl(size: Int = 512): String = "https://cdn.discordapp.com/avatars/$id/$avatar.png?size=$size"
}