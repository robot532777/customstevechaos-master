package ru.cristalix.csc.runnable

import me.stepbystep.api.runTaskTimer
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.time.Duration
import kotlin.time.seconds

class UpdateGameHologramsRunnable : BukkitRunnable() {
    fun start(plugin: Plugin) {
        runTaskTimer(plugin, Duration.ZERO, 15.seconds)
    }

    override fun run() {

    }
}
