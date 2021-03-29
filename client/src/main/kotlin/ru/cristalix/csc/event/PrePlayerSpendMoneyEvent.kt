package ru.cristalix.csc.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PrePlayerSpendMoneyEvent(
    val player: Player,
    val cause: PlayerSpendMoneyEvent.Cause,
    var amount: Int,
) : Event() {

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers() = handlerList
}
