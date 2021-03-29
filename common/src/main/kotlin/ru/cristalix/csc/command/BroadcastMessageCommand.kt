package ru.cristalix.csc.command

import me.stepbystep.api.chat.GOLD
import me.stepbystep.api.chat.GRAY
import me.stepbystep.api.chat.message0
import me.stepbystep.api.command.*
import me.stepbystep.mgapi.common.ServerSide
import me.stepbystep.mgapi.common.packet.type.BroadcastMessagePacket
import me.stepbystep.mgapi.common.packet.type.BroadcastMessageToServersPacket
import me.stepbystep.mgapi.core.CoreActor
import me.stepbystep.mgapi.minecraft.MinecraftActor
import org.bukkit.ChatColor
import ru.cristalix.core.command.CommandHelper

class BroadcastMessageCommand private constructor(private val textAction: (String) -> Unit) {
    init {
        val commandBuilder = CommandHelper
            .literal("cscbroadcast")
            .requires(RequirementIsOperator())

        commandBuilder.thenWithParameters(longStringParameter("сообщение")) {
            it.executesWrapped { ctx ->
                val text = ChatColor.translateAlternateColorCodes('&', ctx.getArgument("сообщение"))
                val textWithPrefix = "$GRAY[${GOLD}CSC$GRAY] $text"
                textAction(textWithPrefix)
            }
        }

        commandBuilder.register()
    }

    companion object {
        operator fun invoke(actor: CoreActor) {
            BroadcastMessageCommand {
                val message = message0(russian = it, english = it)
                val data = BroadcastMessagePacket.ServerData(message)
                val packet = BroadcastMessagePacket(data)
                ServerSide.values().forEach { side ->
                    actor.messageTransport.broadcastPacket(packet, side)
                }
            }
        }

        operator fun invoke(actor: MinecraftActor) {
            BroadcastMessageCommand {
                val message = message0(russian = it, english = it)
                val data = BroadcastMessageToServersPacket.ClientData(message, *ServerSide.values())
                val packet = BroadcastMessageToServersPacket(data)
                actor.messageTransport.sendPacket(packet)
            }
        }
    }
}
