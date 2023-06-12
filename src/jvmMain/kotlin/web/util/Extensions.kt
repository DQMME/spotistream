package web.util

import dataclass.ChannelData
import dataclass.HttpMessage
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import util.Cache
import web.auth.AccountAPI
import web.dataclass.UserInfo
import web.dataclass.UserSession

suspend fun ApplicationCall.respondResourceHtml(name: String) = respondText(
    this::class.java.classLoader.getResource(name)!!.readText(),
    ContentType.Text.Html
)

suspend fun ApplicationCall.respondMessage(message: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(status, HttpMessage(status.value, message))

suspend fun ApplicationCall.validateTokenCookie(): ChannelData? {
    val token = sessions.get<UserSession>()

    if (token == null) {
        respondRedirect("https://www.dqmme.de")
        return null
    }

    val userInfo = AccountAPI.getUser(token.token)

    if(userInfo == null) {
        respondRedirect("https://www.dqmme.de")
        return null
    }

    val channelData = Cache.getChannelDataByDiscordId(userInfo.discordUser.id)

    if(channelData == null) {
        respondRedirect("https://www.dqmme.de")
        return null
    }

    return channelData
}

suspend fun ApplicationCall.validateTokenHeader(): ChannelData? {
    val token = request.header(HttpHeaders.Authorization)

    if (token == null) {
        respondMessage("No token provided", HttpStatusCode.Unauthorized)
        return null
    }

    val userInfo = AccountAPI.getUser(token)

    if (userInfo == null) {
        respondMessage("Couldn't validate your token", HttpStatusCode.Unauthorized)
        return null
    }

    val channelData = Cache.getChannelDataByDiscordId(userInfo.discordUser.id)

    if (channelData == null) {
        respondMessage("Couldn't retrieve channel data", HttpStatusCode.Unauthorized)
        return null
    }

    return channelData
}

suspend fun ApplicationCall.validateTokenHeaderUser(): UserInfo? {
    val token = request.header(HttpHeaders.Authorization)

    if (token == null) {
        respondMessage("No token provided", HttpStatusCode.Unauthorized)
        return null
    }

    val userInfo = AccountAPI.getUser(token)

    if (userInfo == null) {
        respondMessage("Couldn't validate your token", HttpStatusCode.Unauthorized)
        return null
    }

    return userInfo
}

suspend inline fun <reified T> HttpResponse.bodyOrNull() = try {
    body<T>()
} catch (_: Exception) {
    null
}