package ru.cristalix.csc.game.listener.gameplay.phase

import me.stepbystep.api.*
import org.bukkit.entity.Creature
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import ru.cristalix.csc.event.MonsterItemDropAttemptEvent
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.GameTypeOptions
import ru.cristalix.csc.game.customitem.CustomItem
import ru.cristalix.csc.game.listener.gameplay.TransferItemListener
import ru.cristalix.csc.phase.SpawnMobsPhase
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.asCSCPlayer
import ru.cristalix.csc.util.asCSCPlayerOrNull
import kotlin.time.seconds

class MobsListener(
    private val gameScheduler: GameScheduler,
    phase: SpawnMobsPhase,
) : Listener {

    val leftTeams = CSCTeam.all()
        .filterNot { phase.duelPhase?.isParticipant(it) == true }
        .filter { it.isAlive }
        .toMutableSet()

    // needs some startup delay, because mobs don't spawn immediately
    val checkTaskID = gameScheduler.plugin.runRepeating(
        (SpawnMobsPhase.SPAWN_DELAY_SECONDS + 1).seconds, 1.seconds, ::checkTeams
    )

    @EventHandler
    fun PlayerDeathEvent.handle() {
        val cscPlayer = entity.asCSCPlayerOrNull() ?: return
        if (cscPlayer.team !in leftTeams) return
        cscPlayer.removeLife()

        gameScheduler.plugin.runDelayed(10.ticks) {
            if (!cscPlayer.team.isAlive) {
                clearArea(cscPlayer.team)
            }
            gameScheduler.checkGameFinish()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityDamageByEntityEvent.handle() {
        val phase = gameScheduler.currentPhase as? SpawnMobsPhase ?: return
        applyCurseIfNeed()
        if (entity !is LivingEntity || entity is Player) return
        if (!phase.applyThorns) return

        val actualDamager = damager.getActualDamager() as? Player ?: return
        actualDamager.damage(damage)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun PlayerRespawnEvent.handle() {
        val cscPlayer = player.asCSCPlayer()
        respawnLocation = if (cscPlayer.team in leftTeams)
            cscPlayer.team.room.spawnLocation
        else
            gameScheduler.actor.spawnLocation
    }

    @EventHandler
    fun PlayerQuitEvent.handle() {
        val team = player.asCSCPlayer().team
        if (!team.isAlive) {
            clearArea(team)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun EntityDeathEvent.handle() {
        val entity = entity
        if (entity is Player) return
        if (entity !is Creature) return

        val killer = entity.killer ?: return
        val cscKiller = killer.asCSCPlayerOrNull() ?: return
        val event = MonsterItemDropAttemptEvent(cscKiller).also { it.callEvent() }
        val chancePercents = event.chancePercents

        listOf(
            CustomItem.DamageBook,
            CustomItem.HealthBook,
            CustomItem.RegenerationBook,
            CustomItem.UpgradeBook,
            CustomItem.Banana,
            CustomItem.GoldenCoins,
        ).forEach { item ->
            if (RANDOM.nextDouble() * 100 < chancePercents) {
                val stack = item.createStack(killer)
                if (gameScheduler.gameTypeOptions != GameTypeOptions.Solo) {
                    TransferItemListener.markTransferable(stack)
                }
                drops += stack.asCraftMirror()
            }
        }
    }

    fun printPlayers() {
        println(leftTeams.joinToString { it.room.border.livingEntities().joinToString() })
    }

    fun clearArea(team: CSCTeam) {
        leftTeams -= team
        team.room.border.livingEntities().forEach {
            if (it !is Player) {
                it.remove()
            }
        }
    }

    private fun EntityDamageByEntityEvent.applyCurseIfNeed() {
        val actualDamager = damager.getActualDamager()
        val entity = entity

        if (entity !is LivingEntity || actualDamager !is LivingEntity) return

        if (entity is Player && actualDamager !is Player) {
            val cscPlayer = entity.asCSCPlayer()
            damage *= 1 + cscPlayer.curse * 0.15

            if (cscPlayer.isDeathBuffActive()) {
                damage *= 0.1
            }
        } else if (entity !is Player && actualDamager is Player) {
            val cscPlayer = actualDamager.asCSCPlayer()
            damage *= 1 - cscPlayer.curse * 0.15

            if (cscPlayer.isDeathBuffActive()) {
                damage *= 2
            }
        }
    }

    private fun checkTeams() {
        val phase = gameScheduler.currentPhase as? SpawnMobsPhase ?: return

        for (team in leftTeams.toSet()) {
            if (!team.isAlive) continue
            val noLeftEntities = team.room.border.livingEntities().none { it !is Player }

            if (noLeftEntities) {
                gameScheduler.gameTypeOptions.broadcastWaveCompleteMessage(team)
                team.livingBukkitPlayers.forEach(gameScheduler::restoreHealth)
                clearArea(team)
                phase.updateTeamWave(team)
                printPlayers()

                gameScheduler.plugin.runDelayed(2.seconds) {
                    team.livingBukkitPlayers.forEach(phase::onWaveComplete)
                }
            }
        }

        phase.checkCompletion()
    }
}
