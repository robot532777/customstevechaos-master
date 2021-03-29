package ru.cristalix.csc.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerEarnMoneyEvent(
    val player: Player,
    val cause: Cause,
) : Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    var multiplier = 1.0

    override fun getHandlers() = handlerList

    enum class Cause {
        WaveComplete,
        DuelVictory,
        DuelBet,
    }
}
