package util

import dataclass.ApiChannelData
import dataclass.CommandData
import dataclass.PointReward
import dataclass.TwitchReward
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessage
import io.ktor.http.contentType
import io.ktor.http.decodeCookieValue
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.document
import kotlinx.serialization.json.Json

val client = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

suspend fun getTwitchRewards(): MutableList<TwitchReward> {
    val token = authTokenFromCookies() ?: return mutableListOf()
    val response = client.get("/api/twitch-rewards") {
        header(HttpHeaders.Authorization, token)
    }

    if(response.status.isSuccess()) {
        return response.body()
    }

    return mutableListOf(TwitchReward("error", "error"))
}

suspend fun getChannelData(): ApiChannelData? {
    val token = authTokenFromCookies() ?: return null
    return client.get("/api/channel") {
        header(HttpHeaders.Authorization, token)
    }.body<ApiChannelData>()
}

suspend fun saveCommand(commandData: CommandData) {
    val token = authTokenFromCookies() ?: return
    client.post("/api/command") {
        setBody(commandData)
        contentType(ContentType.Application.Json)

        header(HttpHeaders.Authorization, token)
    }.body<HttpMessage>()
}

suspend fun saveReward(pointReward: PointReward) {
    val token = authTokenFromCookies() ?: return
    client.post("/api/reward") {
        setBody(pointReward)
        contentType(ContentType.Application.Json)

        header(HttpHeaders.Authorization, token)
    }.body<HttpMessage>()
}

suspend fun deleteReward(rewardId: String) {
    val token = authTokenFromCookies() ?: return
    client.delete("/api/reward/$rewardId") {
        contentType(ContentType.Application.Json)

        header(HttpHeaders.Authorization, token)
    }.body<HttpMessage>()
}

private fun authTokenFromCookies(): String? {
    val name = "auth="
    val decodedCookie = decodeCookieValue(document.cookie, CookieEncoding.RAW)
    val cookies = decodedCookie.split(';')
    for (element in cookies) {
        var cookie = element
        while (cookie.first() == ' ') {
            cookie = cookie.substring(1)
        }
        if (cookie.indexOf(name) == 0) {
            return cookie.substring(name.length, cookie.length).replace("token=%2523s", "")
        }
    }
    return null
}