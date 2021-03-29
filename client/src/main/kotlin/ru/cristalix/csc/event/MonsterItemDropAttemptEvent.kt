package ru.cristalix.csc.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ru.cristalix.csc.player.CSCPlayer

class MonsterItemDropAttemptEvent(val player: CSCPlayer) : Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    var chancePercents = 0.35

    override fun getHandlers() = handlerList
}
