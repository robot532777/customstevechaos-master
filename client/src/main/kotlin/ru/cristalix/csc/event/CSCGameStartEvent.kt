package ru.cristalix.csc.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ru.cristalix.csc.game.GameScheduler

class CSCGameStartEvent(val gameScheduler: GameScheduler) : Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers() = handlerList
}
