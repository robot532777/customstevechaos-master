package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.shop.item.CSCCage
import java.util.*

class GetSelectedCagePacket : Packet<GetSelectedCagePacket.ClientData, GetSelectedCagePacket.ServerData> {

    constructor(clientData: ClientData) : super(clientData)
    constructor(serverData: ServerData) : super(serverData)

    class ClientData(val playerUUID: UUID) : IClientData
    class ServerData(val cage: CSCCage) : IServerData
}
