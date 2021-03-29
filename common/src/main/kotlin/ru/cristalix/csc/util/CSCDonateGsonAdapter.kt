package ru.cristalix.csc.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import me.stepbystep.mgapi.common.Actor
import me.stepbystep.mgapi.common.packet.serializer.gson.GsonPacketSerializer
import ru.cristalix.csc.shop.CSCDonate

class CSCDonateGsonAdapter : TypeAdapter<CSCDonate>() {
    override fun write(writer: JsonWriter, donate: CSCDonate) {
        writer.value(donate.key)
    }

    override fun read(reader: JsonReader): CSCDonate {
        val key = reader.nextString()
        return CSCDonate.byKey(key) ?: error("No CSCDonate found for $key")
    }

    companion object {
        fun init(actor: Actor<*>) {
            val packetSerializer = actor.packetSerializer
            if (packetSerializer !is GsonPacketSerializer) error("$packetSerializer is not GsonPacketSerializer")

            packetSerializer.updateGson {
                val donateGsonAdapter = CSCDonateGsonAdapter().nullSafe()
                it.registerTypeAdapter(CSCDonate::class.java, donateGsonAdapter)
                it.registerTypeAdapter(CSCDonate.UpgradingDonate::class.java, donateGsonAdapter)
            }
        }
    }
}
