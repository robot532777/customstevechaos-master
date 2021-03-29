package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.NoServerData
import me.stepbystep.mgapi.common.packet.Packet
import java.util.*

class ChangePointsPacket(clientData: ClientData) : Packet<ChangePointsPacket.ClientData, NoServerData>(clientData) {
    init {
        needsResponse = false
    }

    class ClientData(val playerUUID: UUID, val amount: Int) : IClientData
}
