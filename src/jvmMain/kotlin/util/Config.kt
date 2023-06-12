package util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.readText

object Config {
    @Serializable
    private data class ConfigData(
        @SerialName("owner_id") val ownerId: Long,
        @SerialName("mongo_connection") val mongoConnection: String,
        val twitch: ConfigTwitch,
        val spotify: ConfigSpotify
    )

    @Serializable
    private data class ConfigTwitch(
        @SerialName("client_id") val clientId: String,
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("access_token") val accessToken: String,
        @SerialName("redirect_uri") val redirectUri: String
    )

    @Serializable
    private data class ConfigSpotify(
        @SerialName("client_id") val clientId: String,
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("redirect_uri") val redirectUri: String
    )

    private lateinit var configData: ConfigData

    operator fun invoke() {
        configData = Json.decodeFromString(Path("config.json").readText())
    }

    fun getOwnerId() = configData.ownerId

    fun getMongoConnection() = configData.mongoConnection

    fun getTwitchClientId() = configData.twitch.clientId

    fun getTwitchClientSecret() = configData.twitch.clientSecret

    fun getTwitchAccessToken() = configData.twitch.accessToken

    fun getTwitchRedirectUri() = configData.twitch.redirectUri

    fun getSpotifyClientId() = configData.spotify.clientId

    fun getSpotifyClientSecret() = configData.spotify.clientSecret

    fun getSpotifyRedirectUri() = configData.spotify.redirectUri
}