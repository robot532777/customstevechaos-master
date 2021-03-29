package ru.cristalix.csc.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ru.cristalix.csc.phase.DuelPhase

class DuelSuccessfullyCompleteEvent(
    val winner: DuelPhase.Participant,
    val duelPhase: DuelPhase,
) : Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers() = handlerList
}
