package web.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.Config
import web.dataclass.TwitchUserResponse
import web.util.bodyOrNull

object TwitchAPI {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    @Serializable
    private data class TwitchTokenRequest(
        @SerialName("client_id") val clientId: String,
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("code") val code: String,
        @SerialName("grant_type") val grantType: String,
        @SerialName("redirect_uri") val redirectUri: String,
    )

    @Serializable
    private data class TwitchTokenRefreshRequest(
        @SerialName("client_id") val clientId: String,
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("grant_type") val grantType: String,
        @SerialName("redirect_uri") val redirectUri: String,
    )

    @Serializable
    data class TwitchTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        val scope: List<String>? = emptyList()
    )

    suspend fun generateToken(code: String) = client.post("https://id.twitch.tv/oauth2/token") {
        setBody(
            TwitchTokenRequest(
                Config.getTwitchClientId(),
                Config.getTwitchClientSecret(),
                code,
                "authorization_code",
                Config.getTwitchRedirectUri()
            )
        )

        contentType(ContentType.Application.Json)
    }.bodyOrNull<TwitchTokenResponse>()

    suspend fun refreshToken(refreshToken: String) = client.post("https://id.twitch.tv/oauth2/token") {
        setBody(
            TwitchTokenRefreshRequest(
                Config.getTwitchClientId(),
                Config.getTwitchClientSecret(),
                refreshToken,
                "refresh_token",
                Config.getTwitchRedirectUri()
            )
        )

        contentType(ContentType.Application.Json)
    }.bodyOrNull<TwitchTokenResponse>()

    suspend fun getUser(token: String) = client.get("https://api.twitch.tv/helix/users") {
        header(HttpHeaders.Authorization, "Bearer $token")
        header("Client-Id", Config.getTwitchClientId())
    }.bodyOrNull<TwitchUserResponse>()?.data?.firstOrNull()

    suspend fun getUser(id: Int) = client.get("https://api.twitch.tv/helix/users?id=$id") {
        header(HttpHeaders.Authorization, "Bearer ${Config.getTwitchAccessToken()}")
        header("Client-Id", "gp762nuuoqcoxypju8c569th9wz7q5")
    }.bodyOrNull<TwitchUserResponse>()?.data?.firstOrNull()

    @Serializable
    private data class GetModeratorsRequest(
        val operationName: String = "Mods",
        val variables: GetModeratorVariables,
        val extensions: GetModeratorExtensions = GetModeratorExtensions()
    )

    @Serializable
    private data class GetModeratorVariables(
        val login: String
    )

    @Serializable
    private data class GetModeratorExtensions(
        val persistedQuery: GetModeratorPersistedQuery = GetModeratorPersistedQuery()
    )

    @Serializable
    private data class GetModeratorPersistedQuery(
        val version: Int = 1,
        val sha256Hash: String = "cb912a7e0789e0f8a4c85c25041a08324475831024d03d624172b59498caf085"
    )

    @Serializable private data class GetModeratorsResponse(
        val data: GetModeratorsResponseData
    )

    @Serializable private data class GetModeratorsResponseData(
        val user: GetModeratorsResponseUser
    )

    @Serializable private data class GetModeratorsResponseUser(
        val mods : GetModeratorsResponseMods
    )

    @Serializable private data class GetModeratorsResponseMods(
        val edges : List<GetModeratorsResponseEdges>
    )

    @Serializable private data class GetModeratorsResponseEdges(
        val node : ModeratorUser
    )

    @Serializable
    data class ModeratorUser(
        val id: String,
        val login: String
    )

    suspend fun getModerators(token: String): MutableList<ModeratorUser> {
        val responseBody = client.post("https://gql.twitch.tv/gql") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("Client-Id", "kimne78kx3ncx6brgo4mv6wki5h1ko")

            setBody(listOf(GetModeratorsRequest(variables = GetModeratorVariables("dqmme"))))

            contentType(ContentType.Application.Json)
        }.bodyOrNull<List<GetModeratorsResponse>>()

        val moderators = mutableListOf<ModeratorUser>()

        responseBody?.getOrNull(0)?.data?.user?.mods?.edges?.forEach {
            moderators.add(it.node)
        }

        return moderators
    }
}