package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.shop.item.CSCMessagePack
import java.util.*

class GetSelectedMessagePackPacket : Packet<GetSelectedMessagePackPacket.ClientData, GetSelectedMessagePackPacket.ServerData> {

    constructor(clientData: ClientData) : super(clientData)
    constructor(serverData: ServerData) : super(serverData)

    class ClientData(val playerUUID: UUID) : IClientData
    class ServerData(val messagePack: CSCMessagePack) : IServerData
}
