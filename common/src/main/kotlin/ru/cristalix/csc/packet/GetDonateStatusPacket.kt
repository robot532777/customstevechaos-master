package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.shop.CSCDonate
import java.util.*

class GetDonateStatusPacket : Packet<GetDonateStatusPacket.ClientData, GetDonateStatusPacket.ServerData> {

    constructor(clientData: ClientData) : super(clientData)
    constructor(serverData: ServerData) : super(serverData)

    class ClientData(val playerUUID: UUID, val donate: CSCDonate) : IClientData
    class ServerData(val hasDonate: Boolean) : IServerData
}
