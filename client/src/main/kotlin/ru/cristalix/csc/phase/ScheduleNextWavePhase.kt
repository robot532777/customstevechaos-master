package ru.cristalix.csc.phase

import me.stepbystep.api.chat.WHITE
import me.stepbystep.api.chat.YELLOW
import me.stepbystep.api.runDelayed
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.Wave
import me.stepbystep.api.chat.message0
import me.stepbystep.api.chat.message2

class ScheduleNextWavePhase(
    gameScheduler: GameScheduler,
    startDuel: Boolean,
) : Phase(gameScheduler) {

    val duelPhase: DuelPhase? = if (startDuel) DuelPhase(gameScheduler) else null
    private val roundStartMessage = message2<Int, Wave>(
        russian = { index, wave -> "${YELLOW}До начала раунда $index $WHITE${wave.displayName.russian}" },
        english = { index, wave -> "${YELLOW}Before start of round $index $WHITE${wave.displayName.russian}" }
    )

    init {
        val wave = SpawnMobsPhase.getRandomWave(gameScheduler)

        gameScheduler.plugin.runDelayed(gameScheduler.getWaveDelay()) {
            gameScheduler.startPhase(SpawnMobsPhase(gameScheduler, wave, duelPhase))
        }

        val bossBarTitle = message0(
            russian = roundStartMessage.russian(gameScheduler.displayWaveIndex + 1, wave),
            english = roundStartMessage.english(gameScheduler.displayWaveIndex + 1, wave),
        )
        gameScheduler.updateBossBar(bossBarTitle, gameScheduler.getWaveDelay(), Color.ORANGE)
    }
}
