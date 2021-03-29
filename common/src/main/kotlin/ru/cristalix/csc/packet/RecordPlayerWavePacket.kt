package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.NoServerData
import me.stepbystep.mgapi.common.packet.Packet
import java.util.*

class RecordPlayerWavePacket(clientData: ClientData) :
    Packet<RecordPlayerWavePacket.ClientData, NoServerData>(clientData) {

    class ClientData(val playerUUID: UUID, val wave: Int) : IClientData
}
