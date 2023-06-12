package dataclass

enum class UserLevel(val level: Int) {
    BROADCASTER(3),
    MODERATOR(2),
    VIP(1),
    USER(0)
}