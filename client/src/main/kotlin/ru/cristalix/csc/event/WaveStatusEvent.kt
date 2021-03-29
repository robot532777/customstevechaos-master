package ru.cristalix.csc.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ru.cristalix.csc.game.Wave

open class WaveStatusEvent(
    val displayWaveIndex: Int,
    val status: Status,
) : Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers() = handlerList

    enum class Status {
        Start,
        MobsSpawn,
        Complete,
    }

    class Start(
        val wave: Wave,
        displayWaveIndex: Int,
    ) : WaveStatusEvent(displayWaveIndex, Status.Start)
}
