package web

import bot.SpotistreamBot
import bot.util.executeAsync
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import dataclass.ApiChannelData
import dataclass.ChannelData
import dataclass.CommandData
import dataclass.PointReward
import dataclass.TwitchReward
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import util.Cache
import util.Config
import web.auth.SpotifyAPI
import web.auth.TwitchAPI
import web.dataclass.TwitchSession
import web.dataclass.UserSession
import web.util.respondMessage
import web.util.respondResourceHtml
import web.util.validateTokenCookie
import web.util.validateTokenHeader
import web.util.validateTokenHeaderUser
import kotlin.time.Duration.Companion.days

fun launchServer() {
    embeddedServer(Netty, 5009) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                encodeDefaults = true
            })
        }

        install(CORS) {
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            anyHost()
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Accept)
        }

        install(Sessions) {
            cookie<UserSession>("auth") {
                cookie.httpOnly = false
                cookie.maxAge = 365.days
                cookie.domain = "dqmme.de"
            }

            cookie<TwitchSession>("twitch") {
                cookie.httpOnly = false
                cookie.maxAge = 7.days
            }
        }

        routing {
            get("/") {
                val channelInfo = call.validateTokenCookie() ?: return@get

                if (channelInfo.twitchChannelId == null || channelInfo.spotifyRefreshToken == null) {
                    call.respondRedirect("/setup")
                }

                call.respondResourceHtml("index.html")
            }

            get("/commands") {
                call.validateTokenCookie() ?: return@get
                call.respondResourceHtml("commands.html")
            }

            get("/rewards") {
                call.validateTokenCookie() ?: return@get
                call.respondResourceHtml("rewards.html")
            }

            get("/setup") {
                call.validateTokenCookie() ?: return@get
                call.respondResourceHtml("setup.html")
            }

            get("/stream/{user}") {
                call.respondResourceHtml("stream.html")
            }

            route("/auth") {
                get("/twitch") {
                    call.respondRedirect(
                        "https://id.twitch.tv/oauth2/authorize" +
                                "?response_type=code" +
                                "&client_id=${Config.getTwitchClientId()}" +
                                "&redirect_uri=${Config.getTwitchRedirectUri()}" +
                                "&scope="
                    )
                }

                get("/twitch-rewards") {
                    call.respondRedirect(
                        "https://id.twitch.tv/oauth2/authorize" +
                                "?response_type=code" +
                                "&client_id=${Config.getTwitchClientId()}" +
                                "&redirect_uri=${Config.getTwitchRedirectUri()}" +
                                "&scope=${
                                    listOf(
                                        "channel:read:redemptions",
                                        "channel:manage:redemptions"
                                    ).joinToString(" ")
                                }"
                    )
                }

                get("/spotify") {
                    call.respondRedirect(
                        "https://accounts.spotify.com/authorize" +
                                "?response_type=code" +
                                "&client_id=${Config.getSpotifyClientId()}" +
                                "&redirect_uri=${Config.getSpotifyRedirectUri()}" +
                                "&scope=${
                                    listOf(
                                        "user-read-currently-playing",
                                        "user-modify-playback-state",
                                        "user-read-email"
                                    ).joinToString(" ")
                                }"
                    )
                }

                route("/callback") {
                    get("/twitch") {
                        val userInfo = call.validateTokenCookie() ?: return@get
                        val code = call.parameters["code"]

                        if (code == null) {
                            call.respondRedirect("/auth/twitch")
                            return@get
                        }

                        val token = TwitchAPI.generateToken(code)

                        if (token == null) {
                            call.respondRedirect("/auth/twitch")
                            return@get
                        }

                        val user = TwitchAPI.getUser(token.accessToken)

                        if (user == null) {
                            call.respondRedirect("/auth/twitch")
                            return@get
                        }

                        call.sessions.set(TwitchSession(token.accessToken, token.refreshToken))

                        val channelData = Cache.getChannelDataByDiscordId(userInfo.discordUserId)

                        if (channelData == null) {
                            call.respondRedirect("/")
                            return@get
                        }

                        if ((token.scope ?: emptyList()).isNotEmpty()) {
                            Cache.setTwitchToken(user.id.toInt(), token.accessToken)
                            Cache.saveChannelData(
                                channelData.copy(
                                    twitchChannelId = user.id.toInt(),
                                    twitchRefreshToken = token.refreshToken
                                )
                            )

                            SpotistreamBot.twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
                                OAuth2Credential(
                                    "twitch",
                                    token.accessToken
                                ), user.id
                            )

                            call.respondRedirect("/rewards")
                        } else {
                            SpotistreamBot.twitchClient.chat.joinChannel(user.login)

                            Cache.saveChannelData(channelData.copy(twitchChannelId = user.id.toInt()))
                            call.respondRedirect("/setup")
                        }
                    }

                    get("/spotify") {
                        val userInfo = call.validateTokenCookie() ?: return@get
                        val code = call.parameters["code"]

                        if (code == null) {
                            call.respondRedirect("/auth/spotify")
                            return@get
                        }

                        val token = SpotifyAPI.generateToken(code)

                        if (token == null) {
                            call.respondRedirect("/auth/spotify")
                            return@get
                        }

                        val user = SpotifyAPI.getUser(token.accessToken)

                        if (user == null) {
                            call.respondRedirect("/auth/spotify")
                            return@get
                        }

                        val channelData = Cache.getChannelDataByDiscordId(userInfo.discordUserId)

                        if (channelData == null) {
                            call.respondRedirect("/")
                            return@get
                        }

                        if (channelData.twitchChannelId != null) {
                            Cache.setSpotifyToken(
                                userId = channelData.discordUserId,
                                channelId = channelData.twitchChannelId,
                                token = token.accessToken
                            )
                        } else {
                            Cache.setSpotifyToken(
                                userId = channelData.discordUserId,
                                token = token.accessToken
                            )
                        }

                        Cache.saveChannelData(channelData.copy(spotifyRefreshToken = token.refreshToken))
                        call.respondRedirect("/setup")
                    }
                }
            }

            route("/api") {
                get("/channel") {
                    val channelData = call.validateTokenHeader() ?: return@get
                    var apiChannelData = ApiChannelData(
                        channelData.discordUserId,
                        channelData.twitchChannelId,
                        null,
                        channelData.spotifyRefreshToken != null,
                        channelData.twitchRefreshToken != null,
                        null,
                        channelData.commandData,
                        channelData.pointRewards
                    )

                    if (channelData.twitchChannelId != null) {
                        val twitchChannel = TwitchAPI.getUser(channelData.twitchChannelId)

                        apiChannelData = apiChannelData.copy(twitchChannelName = twitchChannel?.displayName)
                    }

                    val spotifyToken = Cache.getSpotifyToken(channelData.discordUserId)
                    if (spotifyToken != null) {
                        val spotifyUser = SpotifyAPI.getUser(spotifyToken)

                        apiChannelData = apiChannelData.copy(spotifyEmail = spotifyUser?.email)
                    }

                    call.respond(apiChannelData)
                }

                get("/command/{id}") {
                    val channelData = call.validateTokenHeader() ?: return@get
                    val commandId = call.parameters["id"]

                    if (commandId == null) {
                        call.respondMessage("No command id given", HttpStatusCode.BadRequest)
                        return@get
                    }

                    val commandData = channelData.getCommandData(commandId)

                    if (commandData == null) {
                        call.respondMessage("Command not found", HttpStatusCode.NotFound)
                        return@get
                    }

                    call.respond(commandData)
                }

                post("/command") {
                    val channelData = call.validateTokenHeader() ?: return@post
                    val commandData = call.receiveNullable<CommandData>()

                    if (commandData == null) {
                        call.respondMessage("This is not a valid command data", HttpStatusCode.BadRequest)
                        return@post
                    }

                    var commandList = channelData.commandData.toMutableList()

                    commandList.removeIf { it.commandId == commandData.commandId }
                    commandList.add(commandData)

                    commandList = commandList.sortedBy { it.commandId }.toMutableList()

                    Cache.saveChannelData(channelData.copy(commandData = commandList))
                    call.respondMessage("The command was successfully updated")
                }

                get("/reward/{id}") {
                    val channelData = call.validateTokenHeader() ?: return@get
                    val rewardId = call.parameters["id"]

                    if (rewardId == null) {
                        call.respondMessage("No reward id given", HttpStatusCode.BadRequest)
                        return@get
                    }

                    val pointReward = channelData.getPointReward(rewardId)

                    if (pointReward == null) {
                        call.respondMessage("No reward found", HttpStatusCode.NotFound)
                        return@get
                    }

                    call.respond(pointReward)
                }

                post("/reward") {
                    val channelData = call.validateTokenHeader() ?: return@post
                    val pointReward = call.receiveNullable<PointReward>()

                    if (pointReward == null) {
                        call.respondMessage("This is not a valid point reward", HttpStatusCode.BadRequest)
                        return@post
                    }

                    var rewardList = channelData.pointRewards.toMutableList()

                    if (rewardList.find { it.id != pointReward.id && it.twitchRewardId == pointReward.twitchRewardId && it.action == pointReward.action } != null) {
                        rewardList.removeIf { it.id == pointReward.id }
                    }

                    rewardList.removeIf { it.id == pointReward.id }
                    rewardList.add(pointReward)

                    rewardList = rewardList.sortedBy { it.id }.toMutableList()

                    Cache.saveChannelData(channelData.copy(pointRewards = rewardList))
                    call.respondMessage("The point reward was successfully updated")
                }

                delete("/reward/{id}") {
                    val channelData = call.validateTokenHeader() ?: return@delete
                    val rewardId = call.parameters["id"]

                    if (rewardId == null) {
                        call.respondMessage("No reward id given", HttpStatusCode.BadRequest)
                        return@delete
                    }

                    val newRewards = channelData.pointRewards.toMutableList()
                    newRewards.removeIf { it.id == rewardId }

                    Cache.saveChannelData(channelData.copy(pointRewards = newRewards))
                    call.respondMessage("You successfully deleted the reward")
                }

                get("/twitch-rewards") {
                    val channelData = call.validateTokenHeader() ?: return@get

                    if (channelData.twitchChannelId == null) {
                        call.respondMessage("You don't have twitch linked", HttpStatusCode.Unauthorized)
                        return@get
                    }

                    println(Cache.twitchTokens)

                    val twitchToken = Cache.getTwitchToken(channelData.twitchChannelId)

                    if (twitchToken == null) {
                        call.respondMessage("You don't have this feature authorized", HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val rewards = SpotistreamBot.twitchClient.helix.getCustomRewards(
                        twitchToken,
                        channelData.twitchChannelId.toString(),
                        null,
                        false
                    ).executeAsync().rewards

                    val rewardList = mutableListOf<TwitchReward>()

                    rewards.sortedBy { it.cost }.forEach {
                        rewardList.add(TwitchReward(it.id, it.title))
                    }

                    call.respond(rewardList)
                }

                @Serializable
                data class AddChannelRequest(
                    @SerialName("_id") val discordUserId: Long
                )

                post("/add-channel") {
                    val userData = call.validateTokenHeaderUser() ?: return@post

                    if (userData.discordUser.id != Config.getOwnerId()) {
                        call.respondMessage("No permission", HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val body = call.receive<AddChannelRequest>()

                    Cache.saveChannelData(ChannelData(body.discordUserId))
                    call.respondMessage("Successful")
                }
            }

            static("/static") {
                resources("")
            }
        }
    }.start(wait = true)
}