package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.IServerData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import java.util.*

class GetSelectedWeaponSkinPacket : Packet<GetSelectedWeaponSkinPacket.ClientData, GetSelectedWeaponSkinPacket.ServerData> {

    constructor(clientData: ClientData) : super(clientData)
    constructor(serverData: ServerData) : super(serverData)

    class ClientData(val playerUUID: UUID) : IClientData
    class ServerData(val weaponSkin: CSCWeaponSkin) : IServerData
}
