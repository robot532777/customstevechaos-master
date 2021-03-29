package ru.cristalix.csc.game.runnable

import me.stepbystep.api.chat.PMessage0
import me.stepbystep.api.runTaskTimer
import me.stepbystep.api.sendProgressCompat
import me.stepbystep.api.ticks
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import ru.cristalix.core.display.IDisplayService
import ru.cristalix.core.display.enums.EnumPosition
import ru.cristalix.core.display.enums.EnumUpdateType
import ru.cristalix.core.display.messages.ProgressMessage
import ru.cristalix.core.formatting.Color
import kotlin.time.Duration

class BossBarRunnable : BukkitRunnable() {
    private var title: PMessage0? = null
    private var timePassed = Duration.ZERO
    private var timeout = Duration.ZERO
    private var color: Color? = null

    private val period = 5.ticks

    fun start(plugin: Plugin) = apply {
        runTaskTimer(plugin, Duration.ZERO, period)
    }

    override fun run() {
        if (title == null) return

        timePassed += period

        if (timePassed >= timeout) {
            broadcastProgressMessage(EnumUpdateType.REMOVE)
            title = null
            timePassed = Duration.ZERO
            timeout = Duration.ZERO
        } else {
            broadcastProgressMessage(EnumUpdateType.ADD)
        }
    }

    fun update(title: PMessage0, timeout: Duration, color: Color) {
        this.title = title
        this.timeout = timeout
        this.color = color
        timePassed = Duration.ZERO
    }

    private fun broadcastProgressMessage(updateType: EnumUpdateType) {
        ProgressMessage.builder()
            .color(color)
            .percent(1 - (timePassed / timeout).toFloat())
            .position(EnumPosition.TOPTOP)
            .updateType(updateType)
            .build()
            .apply {
                val title = title ?: error("No title specified")
                Bukkit.getOnlinePlayers().forEach { player ->
                    IDisplayService.get().sendProgressCompat(player, title, this)
                }
            }
    }
}
