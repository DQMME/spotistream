package dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommandData(
    @SerialName("_id") val commandId: String,
    val usage: String,
    val response: String,
    val placeholders: List<String>,
    @SerialName("required_level") val requiredLevel: UserLevel
)
