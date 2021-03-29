package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.game.GameType
import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.Packet
import java.util.*

class GetPlayerPlacePacket : Packet<GetPlayerPlacePacket.ClientData, GetPlayerPlacePacket.ServerData> {

    constructor(clientData: ClientData) : super(clientData)
    constructor(serverData: ServerData) : super(serverData)

    data class ClientData(val playerUUID: UUID, val gameType: GameType) : IClientData
    class ServerData(val place: Int) : IServerData
}
