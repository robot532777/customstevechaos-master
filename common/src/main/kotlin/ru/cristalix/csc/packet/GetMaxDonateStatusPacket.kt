package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.shop.CSCDonate
import java.util.*

class GetMaxDonateStatusPacket : Packet<GetMaxDonateStatusPacket.ClientData, GetMaxDonateStatusPacket.ServerData> {

    constructor(clientData: ClientData) : super(clientData)
    constructor(serverData: ServerData) : super(serverData)

    class ClientData(val playerUUID: UUID, val donate: CSCDonate.UpgradingDonate) : IClientData
    class ServerData(val maxDonate: CSCDonate?) : IServerData
}
