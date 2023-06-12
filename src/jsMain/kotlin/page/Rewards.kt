package page

import csstype.ClassName
import csstype.System
import dataclass.ApiChannelData
import dataclass.PointReward
import dataclass.RewardAction
import dataclass.TwitchReward
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.select
import react.useEffectOnce
import react.useState
import util.buttonElement
import util.deleteReward
import util.divElement
import util.getChannelData
import util.getTwitchRewards
import util.inputElement
import util.inputValue
import util.pElement
import util.saveReward
import util.selectorElement

private val scope = MainScope()

val rewards = FC<Props> {
    var channelData by useState<ApiChannelData?>(null)
    var rewards by useState<MutableList<TwitchReward>?>(null)
    var rewardId = 0

    useEffectOnce {
        scope.launch {
            channelData = getChannelData()
            rewards = getTwitchRewards()
        }
    }

    if (rewards == null) return@FC

    h1 {
        +"Kanalpunkte-Belohnungen verwalten"
    }

    if(rewards?.getOrNull(0)?.rewardId == "error") {
        h2 {
            +"Dieses Feature musst du zusätzlich autorisieren. Nutze hierfür den untenstehenden Button!"
        }

        button {
            className = ClassName("menu-button link-icon")

            +"Autorisieren"

            onClick = {
                window.location.href = "/auth/twitch-rewards"
            }
        }
        return@FC
    }

    button {
        id = "add-button"
        className = ClassName("success-button plus-icon")

        +"Hinzufügen"

        onClick = { event ->
            val newList = channelData?.pointRewards?.toMutableList() ?: mutableListOf()
            val action = RewardAction.QUEUE_SONG
            newList.add(PointReward("reward-id-${rewardId}", "", action, action.defaultResponse))
            rewardId++
            channelData = channelData?.copy(pointRewards = newList)
            event.currentTarget.disabled = true
        }
    }

    br {}
    br {}

    if (channelData?.hasLinkedTwitchRedemptions == false) {
        h2 {
            +"Dieses Feature hast du noch nicht autorisiert. Mit dem untenstehenden Button kannst du dies tun."
        }

        button {
            className = ClassName("menu-button link-item")

            +"Autorisieren"

            onClick = {
                window.location.href = "/auth/twitch-rewards"
            }
        }
        return@FC
    }

    if (rewards != null) {
        channelData?.pointRewards?.forEach { pointReward ->
            div {
                className = ClassName("command")

                button {
                    id = "${pointReward.id}-button"
                    className = ClassName("command-button")

                    p {
                        id = "${pointReward.id}-id-title"

                        +pointReward.id
                    }

                    i {
                        className = ClassName("fa fa-angle-down")
                    }

                    onClick = { event ->
                        val content = divElement("${pointReward.id}-content")

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
                    id = "${pointReward.id}-content"
                    className = ClassName("command-content")

                    p {
                        className = ClassName("title")

                        +"ID"
                    }

                    input {
                        id = "${pointReward.id}-id"
                        type = InputType.text
                        defaultValue = pointReward.id
                    }

                    p {
                        className = ClassName("explanation")

                        +"Um den Command wiederzuerkennen. Muss einmalig sein."
                    }

                    p {
                        className = ClassName("title")

                        +"Twitch Belohnung"
                    }

                    select {
                        id = "${pointReward.id}-twitch-reward"
                        defaultValue = pointReward.twitchRewardId

                        rewards!!.forEach { reward ->
                            option {
                                value = reward.rewardId

                                +reward.rewardName
                            }
                        }
                    }

                    p {
                        className = ClassName("explanation")

                        +"Welche Twitch-Belohnung dies auslöst"
                    }

                    p {
                        className = ClassName("title")

                        +"Aktion"
                    }

                    select {
                        id = "${pointReward.id}-action"
                        className = ClassName("big-select")

                        RewardAction.values().forEach {
                            option {
                                value = it.name

                                +it.germanName
                            }
                        }

                        onChange = {
                            val action = RewardAction.valueOf(it.currentTarget.value)
                            inputElement("${pointReward.id}-response").value = action.defaultResponse
                        }
                    }

                    p {
                        className = ClassName("explanation")

                        +"Was passieren soll"
                    }

                    p {
                        className = ClassName("title")

                        +"Antwort"
                    }

                    input {
                        id = "${pointReward.id}-response"
                        className = ClassName("big-input")
                        type = InputType.text
                        defaultValue = pointReward.response
                    }

                    p {
                        className = ClassName("explanation")

                        if (pointReward.action.placeholders.isEmpty()) {
                            +"Keine Variablen"
                        } else {
                            +pointReward.action.placeholders.joinToString(", ")
                        }
                    }

                    button {
                        className = ClassName("success-button")

                        +"Speichern"

                        onClick = onClick@{
                            val id = inputValue("${pointReward.id}-id")
                            val rewardIdSelector = selectorElement("${pointReward.id}-twitch-reward")
                            val actionSelector = selectorElement("${pointReward.id}-action")
                            val response = inputValue("${pointReward.id}-response")

                            if (id.isEmpty() || rewardIdSelector.value.isEmpty() || actionSelector.value.isEmpty() || response.isEmpty()) {
                                return@onClick
                            }

                            if (id == "reward-id") {

                                return@onClick
                            }

                            scope.launch {
                                saveReward(
                                    PointReward(
                                        id,
                                        rewardIdSelector.value,
                                        RewardAction.valueOf(actionSelector.value),
                                        response
                                    )
                                )

                                if (pointReward.id == "reward-id") {
                                    buttonElement("add-button").disabled = false
                                }

                                pElement("${pointReward.id}-id-title").innerText = id

                                divElement("${pointReward.id}-content").style.display = "none"
                                buttonElement("${pointReward.id}-button").classList.remove("active")
                            }
                        }
                    }

                    br {}
                    br {}

                    button {
                        className = ClassName("fail-button")

                        +"Löschen"

                        onClick = {
                            val id = pElement("${pointReward.id}-id-title").innerText

                            scope.launch {
                                deleteReward(id)
                                channelData = getChannelData()
                            }
                        }
                    }
                }
            }

            br {}
        }
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