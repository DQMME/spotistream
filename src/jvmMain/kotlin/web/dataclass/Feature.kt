package web.dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Feature(
    @SerialName("_id") val id: String,
    val status: FeatureStatus
)

enum class FeatureStatus {
    GRANTED,
    PENDING,
    NO_ACCESS
}
