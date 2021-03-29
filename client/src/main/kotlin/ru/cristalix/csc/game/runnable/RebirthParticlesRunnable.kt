package ru.cristalix.csc.game.runnable

import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.message0
import me.stepbystep.api.chat.sendMessage
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.scheduler.BukkitRunnable
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.util.spawnParticlesAround

class RebirthParticlesRunnable(private val cscPlayer: CSCPlayer) : BukkitRunnable() {
    private companion object {
        private const val TOTAL_TICKS = 5 * 4 // 4 times in each of 5 seconds

        private val whiteBlockFaces = (0 until 4).map { BlockFace.values()[it] }.toTypedArray()
        private const val WHITE_RED = 255.0 / 255
        private const val WHITE_GREEN = 255.0 / 255
        private const val WHITE_BLUE = 255.0 / 255

        private val yellowBlockFaces = (6 until 10).map { BlockFace.values()[it] }.toTypedArray()
        private const val YELLOW_RED = 250.0 / 255
        private const val YELLOW_GREEN = 230.0 / 255
        private const val YELLOW_BLUE = 118.0 / 255
    }

    fun start() = apply {
        runTaskTimer(cscPlayer.gameScheduler.plugin, 0, 20 / 4)
        cscPlayer.asBukkitPlayer().sendMessage(Messages.buffStart)
    }

    private var ticksPassed = 0

    override fun run() {
        val player = cscPlayer.asBukkitPlayerOrNull()
        if (player == null) {
            cancel()
            return
        }

        player.spawnParticlesAround(Particle.REDSTONE, whiteBlockFaces, WHITE_RED, WHITE_GREEN, WHITE_BLUE, 0)
        player.spawnParticlesAround(Particle.REDSTONE, yellowBlockFaces, YELLOW_RED, YELLOW_GREEN, YELLOW_BLUE, 0)

        ticksPassed++
        if (ticksPassed >= TOTAL_TICKS) {
            cancel()
        }
    }

    override fun cancel() {
        super.cancel()

        cscPlayer.asBukkitPlayerOrNull()?.sendMessage(Messages.buffEnd)
        cscPlayer.rebirthRunnable = null
    }

    private object Messages {
        val buffStart = message0(
            russian = "${GREEN}Следующие 5 секунд вы получаете на 90% меньше и наносите в 2 раза больше урона",
            english = "${GREEN}For the next 5 seconds, you take 90% less damage and deal 2x more damage",
        )

        val buffEnd = message0(
            russian = "${RED}Бафф после смерти закончился",
            english = "${RED}After death buff is over",
        )
    }
}
