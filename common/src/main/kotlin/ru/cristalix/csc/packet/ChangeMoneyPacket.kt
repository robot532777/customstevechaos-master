package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.NoServerData
import me.stepbystep.mgapi.common.packet.Packet
import java.util.*

class ChangeMoneyPacket(clientData: ClientData) : Packet<ChangeMoneyPacket.ClientData, NoServerData>(clientData) {
    init {
        needsResponse = false
    }

    class ClientData(val playerUUID: UUID, val difference: Int) : IClientData
}
