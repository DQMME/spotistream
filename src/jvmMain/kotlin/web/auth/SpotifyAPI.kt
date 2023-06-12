package web.auth

import bot.util.await
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.michaelthelin.spotify.SpotifyApi
import util.Config
import web.dataclass.SpotifyUser
import web.util.bodyOrNull
import java.net.URI

object SpotifyAPI {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    @Serializable
    private data class SpotifyTokenRequest(
        @SerialName("client_id") val clientId: String,
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("code") val code: String,
        @SerialName("grant_type") val grantType: String,
        @SerialName("redirect_uri") val redirectUri: String,
    )

    @Serializable
    data class SpotifyTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String
    )

    suspend fun generateToken(code: String): SpotifyTokenResponse? {
        val spotifyClient = SpotifyApi.Builder()
            .setClientId(Config.getSpotifyClientId())
            .setClientSecret(Config.getSpotifyClientSecret())
            .setRedirectUri(URI(Config.getSpotifyRedirectUri()))
            .build()

        val response = spotifyClient.authorizationCode(code).build().await() ?: return null

        return SpotifyTokenResponse(response.accessToken, response.refreshToken)
    }

    suspend fun getUser(token: String) = client.get("https://api.spotify.com/v1/me") {
        header(HttpHeaders.Authorization, "Bearer $token")
    }.bodyOrNull<SpotifyUser>()
}