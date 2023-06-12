package web.dataclass

data class UserSession(val token: String)

data class TwitchSession(
    val accessToken: String,
    val refreshToken: String
)