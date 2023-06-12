package util

import dataclass.ChannelData
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo

object Database {
    private lateinit var client: CoroutineClient
    private lateinit var database: CoroutineDatabase
    private lateinit var channelDataCollection: CoroutineCollection<ChannelData>

    operator fun invoke() {
        client = KMongo.createClient(Config.getMongoConnection()).coroutine
        database = client.getDatabase("spotistream")
        channelDataCollection = database.getCollection("channels")
    }

    suspend fun getChannelData() = channelDataCollection.find().toList()

    suspend fun getChannelDataByDiscordId(userId: Long) =
        channelDataCollection.findOne(ChannelData::discordUserId eq userId)

    suspend fun getChannelDataByChannelId(channelId: Int) =
        channelDataCollection.findOne(ChannelData::twitchChannelId eq channelId)

    suspend fun saveChannelData(channelData: ChannelData) = channelDataCollection.save(channelData)
}