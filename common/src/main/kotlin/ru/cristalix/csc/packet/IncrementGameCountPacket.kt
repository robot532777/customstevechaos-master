package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.NoServerData
import me.stepbystep.mgapi.common.packet.Packet
import java.util.*

class IncrementGameCountPacket : Packet<IncrementGameCountPacket.ClientData, NoServerData> {

    init {
        needsResponse = false
    }

    constructor() : super(NoServerData)
    constructor(clientData: ClientData) : super(clientData)

    class ClientData(val playerUUID: UUID) : IClientData
}
