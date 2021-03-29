package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.NoClientData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.map.DuelMap

class DuelDataPacket : Packet<NoClientData, DuelDataPacket.ServerData> {

    constructor() : super(NoClientData)
    constructor(serverData: ServerData) : super(serverData)

    class ServerData(val duelMaps: List<DuelMap>) : IServerData
}
