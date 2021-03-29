package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.NoClientData
import me.stepbystep.mgapi.common.packet.Packet

class SetSalePacket(serverData: ServerData) : Packet<NoClientData, SetSalePacket.ServerData>(serverData) {
    init {
        needsResponse = false
    }

    class ServerData(val salePercent: Int) : IServerData
}
