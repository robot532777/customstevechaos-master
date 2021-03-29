package ru.cristalix.csc.packet

import me.stepbystep.mgapi.common.packet.IClientData
import me.stepbystep.mgapi.common.packet.NoServerData
import me.stepbystep.mgapi.common.packet.Packet
import ru.cristalix.csc.shop.item.CSCClass

class RecordPlayerClassPacket(clientData: ClientData) :
    Packet<RecordPlayerClassPacket.ClientData, NoServerData>(clientData) {

    class ClientData(val selectedClass: CSCClass, val allClasses: List<CSCClass>) : IClientData
}
