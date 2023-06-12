package page

import csstype.ClassName
import dataclass.ApiChannelData
import dataclass.CommandData
import dataclass.UserLevel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.useEffectOnce
import react.useState
import util.buttonElement
import util.divElement
import util.getChannelData
import util.inputValue
import util.saveCommand
import util.selectorElement

private var scope = MainScope()

val commands = FC<Props> {
    var channelData by useState<ApiChannelData?>(null)

    useEffectOnce {
        scope.launch {
            channelData = getChannelData()
        }
    }

    h1 {
        +"Commands verwalten"
    }

    channelData?.commandData?.sortedBy { it.commandId }?.forEach {
        div {
            className = ClassName("command")

            button {
                id = "${it.commandId}-button"
                className = ClassName("command-button")

                p {
                    +it.usage
                }

                i {
                    className = ClassName("fa fa-angle-down")
                }

                onClick = { event ->
                    val content = divElement("${it.commandId}-content")

                    if (content.style.display == "block") {
                        content.style.display = "none"
                        event.currentTarget.classList.remove("active")
                    } else {
                        content.style.display = "block"
                        event.currentTarget.classList.add("active")
                    }
                }
            }

            br {}

            div {
                id = "${it.commandId}-content"
                className = ClassName("command-content")

                p {
                    className = ClassName("title")

                    +"Name"
                }

                input {
                    id = "${it.commandId}-name"
                    type = InputType.text
                    defaultValue = it.usage
                }

                p {
                    className = ClassName("explanation")

                    +"Wie der Command ausgeführt wird"
                }

                p {
                    className = ClassName("title")

                    +"Antwort"
                }

                input {
                    id = "${it.commandId}-response"
                    className = ClassName("big-input")
                    type = InputType.text
                    defaultValue = it.response
                }

                p {
                    className = ClassName("explanation")
                    if (it.placeholders.isEmpty()) +"Keine Variablen"
                    else +"Verfügbare Variablen: ${it.placeholders.joinToString(", ")}"
                }

                p {
                    className = ClassName("title")

                    +"Benötigtes Level"
                }

                select {
                    id = "${it.commandId}-level"

                    option {
                        value = "0"
                        +"User"
                    }

                    option {
                        value = "1"
                        +"VIP"
                    }

                    option {
                        value = "2"
                        +"Moderator"
                    }

                    option {
                        value = "3"
                        +"Broadcaster"
                    }

                    defaultValue = it.requiredLevel.level.toString()
                }

                p {
                    className = ClassName("explanation")

                    +"Welches Level der Nutzer für diesen Command benötigt."
                }

                button {
                    id = "${it.commandId}-save"
                    className = ClassName("success-button")

                    +"Speichern"

                    onClick = { event ->
                        val name = inputValue("${it.commandId}-name")
                        val response = inputValue("${it.commandId}-response")
                        val levelSelector = selectorElement("${it.commandId}-level")
                        val level = when (levelSelector.selectedIndex) {
                            3 -> UserLevel.BROADCASTER

                            2 -> UserLevel.MODERATOR

                            1 -> UserLevel.VIP

                            else -> UserLevel.USER
                        }

                        val commandData = CommandData(
                            it.commandId,
                            name,
                            response,
                            it.placeholders,
                            level
                        )

                        scope.launch {
                            divElement("${it.commandId}-content").style.display = "none"
                            buttonElement("${it.commandId}-button").classList.remove("active")
                            saveCommand(commandData)
                        }
                    }
                }
            }

        }

        br {}
    }

    br {}
    br {}
    br {}

    ReactHTML.a {
        href = "/"

        button {
            className = ClassName("menu-button home-icon")

            +"Zur Startseite"
        }
    }
}