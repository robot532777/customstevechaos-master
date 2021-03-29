package ru.cristalix.csc.event

import me.stepbystep.api.item.NMSItemStack
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerEnchantItemEvent(
    val player: Player,
    val newItem: NMSItemStack,
) : Event() {

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers() = handlerList
}
