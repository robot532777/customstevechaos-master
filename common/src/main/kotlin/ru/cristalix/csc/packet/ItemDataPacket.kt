package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.NoClientData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.CSCItemStack

class ItemDataPacket : Packet<NoClientData, ItemDataPacket.ServerData> {

    constructor() : super(NoClientData)
    constructor(serverData: ServerData) : super(serverData)

    class ServerData(val allItems: List<CSCItemStack>) : IServerData
}
