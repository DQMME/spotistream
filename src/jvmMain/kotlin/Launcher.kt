import bot.SpotistreamBot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import util.Cache
import util.Config
import util.Database
import util.ioScope
import web.launchServer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

suspend fun main() {
    Config()
    Database()

    ioScope.launch {
        while (isActive) {
            println("---")
            println("Refreshing Cache")
            println("---")
            Cache.refresh()
            delay(15.minutes)
        }
    }

    ioScope.launch {
        Cache.updateChannels()
        Cache.refresh()
        delay(5.seconds)
        SpotistreamBot.start()
    }

    launchServer()
}