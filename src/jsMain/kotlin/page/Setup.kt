package page

import csstype.ClassName
import dataclass.ApiChannelData
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.strong
import react.useEffectOnce
import react.useState
import util.getChannelData

private val scope = MainScope()

val setup = FC<Props> {
    var channelData by useState<ApiChannelData?>(null)

    useEffectOnce {
        scope.launch {
            channelData = getChannelData()
        }
    }

    h1 {
        +"Accounts verbinden"
    }
    h2 {
      +"Hier siehst du, welche Accounts du schon verbunden hast und welche du noch verbinden musst, um "
      strong {
          className = ClassName("primary")

          +"SpotiStream"
      }

      +" nutzen zu können."
    }

    var reLinkTwitch = false

    if(channelData != null && channelData?.twitchChannelId == null) {
        button {
            className = ClassName("fail-button")

            +"Twitch"

            onClick = {
                window.location.href = "/auth/twitch"
            }
        }
    } else if(channelData != null && channelData?.twitchChannelId != null) {
        if(channelData?.twitchChannelName != null) {
            h3 {
                +"Aktueller Twitch Account: "
                strong {
                    className = ClassName("primary")

                    +"${channelData?.twitchChannelName}"
                }
            }
        }

        button {
            id = "link-twitch-button"
            className = ClassName("success-button")

            +"Twitch"

            onClick = onClick@ {
                if(reLinkTwitch) {
                    window.location.href = "/auth/twitch"
                    return@onClick
                }

                reLinkTwitch = true
                document.getElementById("link-twitch-button")?.innerHTML = " Wirklich neu verbinden?"
            }
        }
    }

    br {}
    br {}

    var reLinkSpotify = false

    if(channelData != null && channelData?.hasLinkedSpotify == false) {
        button {
            className = ClassName("fail-button")

            +"Spotify"

            onClick = {
                window.location.href = "/auth/spotify"
            }
        }
    } else if(channelData != null && channelData?.hasLinkedSpotify == true) {
        if(channelData?.spotifyEmail != null) {
            h3 {
                +"Aktueller Spotify Account: "
                strong {
                    className = ClassName("primary")

                    +"${channelData?.spotifyEmail}"
                }
            }
        }

        button {
            id = "link-spotify-button"
            className = ClassName("success-button")

            +"Spotify"

            onClick = onClick@ {
                if(reLinkSpotify) {
                    window.location.href = "/auth/spotify"
                    return@onClick
                }

                reLinkSpotify = true
                document.getElementById("link-spotify-button")?.innerHTML = " Wirklich neu verbinden?"
            }
        }
    }

    if(channelData != null && channelData?.hasLinkedSpotify == true && channelData?.twitchChannelId != null) {
        br {}
        br {}
        br {}

        a {
            href = "/"

            button {
                className = ClassName("menu-button home-icon")

                +"Zur Startseite"
            }
        }
    }
}