package ru.cristalix.csc.command

import me.stepbystep.api.command.executesWrapped
import me.stepbystep.api.command.register
import me.stepbystep.api.command.sender
import me.stepbystep.mgapi.client.ClientActor
import me.stepbystep.mgapi.common.packet.type.AddPlayerToQueuePacket
import org.bukkit.entity.Player
import ru.cristalix.core.command.CommandHelper

class StartNewGameCommand(private val actor: ClientActor) {
    companion object {
        const val NAME = "startnewgame"
    }

    init {
        CommandHelper.literal(NAME)
            .description("Начать новую игру")
            .hiddenVisibility()
            .executesWrapped {
                it.sender.execute()
            }
            .register()
    }

    private fun Player.execute() {
        actor.sendPlayerToLobby(this)

        val packet = AddPlayerToQueuePacket(AddPlayerToQueuePacket.ClientData(uniqueId, actor.gameType, false))
        actor.messageTransport.sendPacket(packet)
    }
}
