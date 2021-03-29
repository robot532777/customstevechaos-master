package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.NoClientData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.map.GameMap

class MapDataPacket : Packet<NoClientData, MapDataPacket.ServerData> {

    constructor() : super(NoClientData)
    constructor(serverData: ServerData) : super(serverData)

    class ServerData(val map: GameMap) : IServerData
}
