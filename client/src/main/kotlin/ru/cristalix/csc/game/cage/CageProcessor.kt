package ru.cristalix.csc.game.cage

import me.stepbystep.api.chat.RED
import me.stepbystep.api.runTaskTimer
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.time.Duration
import kotlin.time.seconds

class CageProcessor(
    private val allPlayerData: List<CagePlayerData>,
    private val plugin: Plugin,
) : BukkitRunnable() {

    private var isStarted = false
    private var secondsLeft = 5

    fun start() {
        isStarted = true

        for ((cscPlayer, spawnLocation) in allPlayerData) {
            val player = cscPlayer.asBukkitPlayerOrNull() ?: continue

            cscPlayer.selectedCage.fill(spawnLocation)
            player.teleport(spawnLocation)
        }

        runTaskTimer(plugin, Duration.ZERO, 1.seconds)
    }

    fun interrupt() {
        if (!isStarted) return

        cancel()

        for ((player, spawnLocation) in allPlayerData) {
            player.selectedCage.clear(spawnLocation)
        }
    }

    override fun run() {
        if (secondsLeft == 0) {
            interrupt()
            return
        }

        for ((player) in allPlayerData) {
            player.asBukkitPlayerOrNull()?.sendTitle("$RED$secondsLeft", "", 0, 20, 0)
        }
        secondsLeft--
    }
}
