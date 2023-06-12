package dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PointReward(
    @SerialName("_id") val id: String,
    @SerialName("twitch_reward_id") val twitchRewardId: String,
    val action: RewardAction,
    val response: String
)

enum class RewardAction(val placeholders: MutableList<String>, val germanName: String, val defaultResponse: String) {
    QUEUE_SONG(
        mutableListOf("%NAME%", "%ARTIST%", "%LINK%"),
        "Song in die Warteschlange hinzufügen",
        "Du hast den Song \"%NAME%\" von \"%ARTIST%\" (\"%LINK%\") in die Warteschlange hinzugefügt!"
    ),
    PAUSE_PLAYBACK(mutableListOf(), "Wiedergabe pausieren", "Du hast die Wiedergabe pausiert!"),
    RESUME_PLAYBACK(mutableListOf(), "Wiedergabe starten", "Du hast die Wiedergabe gestartet!"),
    SKIP_SONG(mutableListOf(), "Song überspringen", "Du hast den nächsten Song gestartet!"),
    PREVIOUS_SONG(mutableListOf(), "Vorherigen Song abspielen", "Du hast den vorherigen Song gestartet!"),
    SET_VOLUME(mutableListOf("%VOLUME%"), "Lautstärke setzen", "Du hast die Lautstärke auf %VOLUME%% gesetzt.")
}

@Serializable
data class TwitchReward(
    @SerialName("_id") val rewardId: String,
    @SerialName("reward_name") val rewardName: String
)