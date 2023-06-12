import dataclass.ChannelData
import util.Config
import util.Database

suspend fun main() {
    Config()
    Database()

    Database.saveChannelData(
        ChannelData(
            discordUserId = 416982681562120212,
            twitchChannelId = 271613708,
            spotifyRefreshToken = "AQA8x_LJhR1VPJxMs1kaKZg54ZvCmCsjwwTlQeVXXZzbiN3Pt9fMYiskhPldHx7WG76QT5wCcqZcYCZRUG-BGLbKUT_QIOT_SZonXFAw6NMInzC59PQbbiVR1Y3JjoRaaU4"
        )
    )
}