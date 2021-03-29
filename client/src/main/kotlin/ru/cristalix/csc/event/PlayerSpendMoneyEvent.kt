package ru.cristalix.csc.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerSpendMoneyEvent(
    val player: Player,
    val cause: Cause,
    val amount: Int,
) : Event() {

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers() = handlerList

    enum class Cause {
        UpgradeItem,
    }
}
