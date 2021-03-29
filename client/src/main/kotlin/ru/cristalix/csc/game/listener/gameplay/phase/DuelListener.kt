package ru.cristalix.csc.game.listener.gameplay.phase

import me.stepbystep.api.runTask
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import ru.cristalix.csc.game.DuelStage
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.phase.DuelPhase
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.asCSCPlayer
import ru.cristalix.csc.util.asCSCPlayerOrNull

class DuelListener(private val duelPhase: DuelPhase, private val gameScheduler: GameScheduler) : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerQuitEvent.handle() {
        if (duelPhase.isParticipant(player)) {
            val cscPlayer = player.asCSCPlayerOrNull() ?: return
            cscPlayer.isAliveOnDuel = false

            if (cscPlayer.team.isNotAliveOnDuel()) {
                duelPhase.interruptDuel()
            }
        } else {
            duelPhase.removeBets(player)
        }
    }

    @EventHandler
    fun PlayerDeathEvent.handle() {
        val cscPlayer = entity.asCSCPlayer()
        cscPlayer.isAliveOnDuel = false

        if (cscPlayer.team.isNotAliveOnDuel()) {
            val participant = duelPhase.findParticipant(cscPlayer.team) ?: return
            val winner = duelPhase.getOtherParticipant(participant)
            duelPhase.finishDuel(winner)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerRespawnEvent.handle() {
        val cscPlayer = player.asCSCPlayer()
        if (!duelPhase.isParticipant(cscPlayer.team)) return
        if (cscPlayer.team.isNotAliveOnDuel()) return
        if (duelPhase.stage != DuelStage.Active) return

        respawnLocation = duelPhase.map.viewersLocation

        gameScheduler.plugin.runTask {
            if (duelPhase.stage == DuelStage.Active) {
                duelPhase.addViewer(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun EntityDamageEvent.handle() {
        val player = entity as? Player ?: return
        val duelParticipant = duelPhase.findParticipant(player) ?: return
        val otherParticipant = duelPhase.getOtherParticipant(duelParticipant)

        otherParticipant.dealtDamage += damage
    }

    private fun CSCTeam.isNotAliveOnDuel(): Boolean = livingPlayers.none { it.isAliveOnDuel }
}
