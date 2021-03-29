package ru.cristalix.csc.phase

import com.google.common.base.Predicate
import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.event.PlayerEarnMoneyEvent
import ru.cristalix.csc.event.WaveStatusEvent
import ru.cristalix.csc.game.DuelStage
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.Wave
import ru.cristalix.csc.game.listener.gameplay.phase.MobsListener
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.CommonMessages
import ru.cristalix.csc.util.asCSCPlayer
import ru.cristalix.csc.util.getBukkitSpawnLocations
import ru.cristalix.csc.util.getSpawnLocations
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.seconds

class SpawnMobsPhase(
    gameScheduler: GameScheduler,
    wave: Wave,
    val duelPhase: DuelPhase?,
) : Phase(gameScheduler) {

    companion object {
        const val SPAWN_DELAY_SECONDS = 3

        private const val ROW_COUNT = 3
        private val BUFF_DELAY = 60.seconds
        private val THORNS_DELAY = 100.seconds
        private val DEATH_DELAY = 40.seconds

        val lastWaves = ArrayList<Wave>()

        fun getRandomWave(gameScheduler: GameScheduler): Wave =
            when {
                gameScheduler.isGolemWave -> Wave.IronGolem
                else -> (Wave.values().toList() - lastWaves)
                    .filter { gameScheduler.displayWaveIndex >= it.requiredWave }
                    .random()
            }
    }

    private object Messages {
        val waveStarted = message2<Int, Wave>(
            russian = { index, wave -> "${GREEN}Волна $GOLD#$index ${wave.displayName.russian} ${GREEN}началась" },
            english = { index, wave -> "${GREEN}Wave $GOLD#$index ${wave.displayName.english} ${GREEN}started" }
        )

        val monsterAppearDelay = message0(
            russian = "${GOLD}Монстры появятся через $SPAWN_DELAY_SECONDS секунды",
            english = "${GOLD}Monsters will spawn in $SPAWN_DELAY_SECONDS seconds",
        )

        val nthWave = message1<Int>(
            russian = { "${GREEN}Волна $GOLD#$it" },
            english = { "${GREEN}Wave $GOLD#$it" },
        )

        val beforeRoundBuff = message0(
            russian = "${YELLOW}До усиления раунда",
            english = "${YELLOW}Before round buff",
        )

        val beforeMobThorns = message0(
            russian = "${YELLOW}До того, как мобы начнут отражать урон",
            english = "${YELLOW}Before mobs start reflecting damage",
        )

        val beforeAllDeath = message0(
            russian = "${YELLOW}До смерти всех игроков, которые не убили мобов",
            english = "${YELLOW}Before the death of all players who are still fighting mobs",
        )

        val waveLastsFor = message1<Int>(
            russian = { "${RED}С начала волны прошло $it секунд" },
            english = { "$RED$it seconds passed since beginning of wave" },
        )

        val monsterBuff = message0(
            russian = "${RED}Теперь каждую секунду мобы будут становиться сильнее и быстрее",
            english = "${RED}Now every second mobs will become stronger and faster",
        )

        val monsterThorns = message0(
            russian = "${RED}Теперь мобы будут отражать полученный урон",
            english = "${RED}Now mobs will reflect taken damage",
        )

        val fightingPlayersDeath = message0(
            russian = "${RED}Все игроки, которые не убили мобов, выбывают из игры",
            english = "${RED}Every player who is still fighting mobs is eliminated from the game",
        )

        val waveFinished = message0(
            russian = "${GREEN}Волна закончилась",
            english = "${GREEN}Wave finished",
        )
    }

    var applyThorns = false
        private set

    private val mobsListener = MobsListener(gameScheduler, this)

    private var teamsCompletedWave = 0
    private val waveCompletionOrder = hashMapOf<CSCTeam, Int>()

    private val printTaskID = gameScheduler.plugin.runRepeating(Duration.ZERO, 10.seconds) {
        mobsListener.printPlayers()
    }

    val leftTeams: Set<CSCTeam> get() = mobsListener.leftTeams

    init {
        mobsListener.register(gameScheduler.plugin)

        if (lastWaves.size >= 2) {
            lastWaves.removeFirst()
        }

        lastWaves += wave
        spawnMobs(wave)
    }

    override fun finish() {
        CSCTeam.all().forEach { it.giveWaveReward() }

        CSCPlayer.getLivingPlayers().forEach { cscPlayer ->
            val player = cscPlayer.asBukkitPlayer()
            cscPlayer.updateMaxHealth()
            player.clearPotionEffects()
            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).value
        }

        WaveStatusEvent(gameScheduler.displayWaveIndex, WaveStatusEvent.Status.Complete).callEvent()

        HandlerList.unregisterAll(mobsListener)
        cancelTask(printTaskID)
        cancelTask(mobsListener.checkTaskID)
        duelPhase?.finish()
    }

    private fun CSCTeam.giveWaveReward() {
        val completedIndex = waveCompletionOrder[this] ?: -1
        val rawWaveReward = 200 + 40 * gameScheduler.waveIndex

        val multiplier = when (val completionIndex = waveCompletionOrder[this]) {
            null -> {
                livingPlayers.forEach { it.changeGold(rawWaveReward) }
                return
            }
            in 0..9 -> (10 - completionIndex) / 100.0
            else -> (8 - completedIndex) / 100.0
        }

        val rewardDifference = (rawWaveReward * multiplier).toInt()
        val actualWaveReward = rawWaveReward + rewardDifference

        fun getMessage(balanceWord: String, newBalance: Int): String {
            val diffPrefix = when {
                multiplier > 0.0 -> "$GREEN+"
                else -> RED
            }
            return "$GREEN$balanceWord: $GOLD$newBalance $GRAY($GREEN+$rawWaveReward $diffPrefix $rewardDifference$GRAY)"
        }

        val messageFormatter = message2<Int, Int>(
            russian = { _, newBalance -> getMessage(CommonMessages.balanceWord.russian, newBalance) },
            english = { _, newBalance -> getMessage(CommonMessages.balanceWord.english, newBalance) },
        )

        livingPlayers.forEach {
            it.changeGold(
                actualWaveReward,
                messageFormatter = messageFormatter,
                cause = PlayerEarnMoneyEvent.Cause.WaveComplete
            )
        }
    }

    private fun spawnMobs(wave: Wave) {
        val displayWave = gameScheduler.displayWaveIndex + 1

        Messages.waveStarted.broadcast(displayWave, wave)
        Messages.monsterAppearDelay.broadcast()
        Bukkit.getOnlinePlayers().forEach {
            it.sendTitle(
                Messages.nthWave(it, displayWave),
                "$GOLD${wave.displayName(it)}",
                5,
                20,
                5
            )
        }

        WaveStatusEvent.Start(wave, gameScheduler.displayWaveIndex).callEvent()

        mobsListener.leftTeams.forEach { team ->
            team.getBukkitSpawnLocations(team.room.spawnLocation).forEach { (player, loc) ->
                player.teleport(loc)
            }
        }

        gameScheduler.plugin.runDelayed(SPAWN_DELAY_SECONDS.seconds) {
            mobsListener.leftTeams.forEach {
                spawnEntities(wave, it)
            }
            WaveStatusEvent(gameScheduler.displayWaveIndex, WaveStatusEvent.Status.MobsSpawn).callEvent()
        }

        gameScheduler.updateBossBar(Messages.beforeRoundBuff, BUFF_DELAY, Color.RED)
        gameScheduler.plugin.runDelayed(BUFF_DELAY) {
            if (gameScheduler.currentPhase != this) return@runDelayed

            Messages.waveLastsFor.broadcast(BUFF_DELAY.toInt(TimeUnit.SECONDS))
            Messages.monsterBuff.broadcast()
            gameScheduler.updateBossBar(Messages.beforeMobThorns, THORNS_DELAY, Color.RED)
            startBuffRunnable()

            gameScheduler.plugin.runDelayed(THORNS_DELAY) thorns@{
                if (gameScheduler.currentPhase != this) return@thorns

                applyThorns = true
                Messages.waveLastsFor.broadcast((BUFF_DELAY + THORNS_DELAY).toInt(TimeUnit.SECONDS))
                Messages.monsterThorns.broadcast()
                gameScheduler.updateBossBar(Messages.beforeAllDeath, DEATH_DELAY, Color.RED)
                startDeathRunnable()
            }
        }
    }

    fun updateTeamWave(team: CSCTeam) {
        waveCompletionOrder[team] = teamsCompletedWave++
    }

    fun onWaveComplete(player: Player) {
        player.asCSCPlayer().refillables.updateInventory()

        if (duelPhase?.stage == DuelStage.Active) {
            duelPhase.addViewer(player)
        } else {
            gameScheduler.teleportAfterWave(player)
        }
    }

    fun checkCompletion() {
        val duelStage = duelPhase?.stage
        val isDuelFinished = duelStage == null || duelStage == DuelStage.Finished

        if (mobsListener.leftTeams.isEmpty() && isDuelFinished) {
            Messages.waveFinished.broadcast()
            gameScheduler.startPhase(LobbyPhase(gameScheduler))
        }
    }

    private fun startBuffRunnable() {
        var speedMultiplier = 1.0
        var damageMultiplier = 1.0

        var taskID = -1
        taskID = gameScheduler.plugin.runRepeating(Duration.ZERO, 1.seconds) {
            if (gameScheduler.currentPhase != this) {
                cancelTask(taskID)
                return@runRepeating
            }

            mobsListener.leftTeams
                .flatMap { it.room.border.livingEntities() }
                .forEach { entity ->
                    if (entity is Player) return@forEach
                    entity.modifyAttribute(
                        Attribute.GENERIC_ATTACK_DAMAGE,
                        damageMultiplier,
                        damageMultiplier + 0.1
                    )
                    if (speedMultiplier < 3.0) {
                        entity.modifyAttribute(
                            Attribute.GENERIC_MOVEMENT_SPEED,
                            speedMultiplier,
                            speedMultiplier + 0.01
                        )
                    }
                }

            damageMultiplier += 0.1
            if (speedMultiplier < 3.0) {
                speedMultiplier += 0.01
            }
        }
    }

    private fun startDeathRunnable() {
        gameScheduler.plugin.runDelayed(DEATH_DELAY) {
            if (gameScheduler.currentPhase != this) return@runDelayed

            Messages.waveLastsFor.broadcast((BUFF_DELAY + THORNS_DELAY + DEATH_DELAY).toInt(DurationUnit.SECONDS))
            Messages.fightingPlayersDeath.broadcast()
            val killedTeams = mobsListener.leftTeams.toSet()
                .sortedByDescending { team -> team.livingPlayers.sumBy { it.curse } }
            for (team in killedTeams) {
                team.livingPlayers.forEach { it.forceDeath() }
                mobsListener.clearArea(team)
                if (gameScheduler.checkGameFinish()) return@runDelayed
            }

            checkCompletion()
        }
    }

    private fun LivingEntity.modifyAttribute(attribute: Attribute, oldMultiplier: Double, newMultiplier: Double) {
        getAttribute(attribute).let {
            it.baseValue = (it.baseValue / oldMultiplier) * newMultiplier
        }
    }

    private fun spawnEntities(wave: Wave, team: CSCTeam) {
        val startLocation = team.room.mobsLocation
        val allEntities = (0 until gameScheduler.gameTypeOptions.maxPlayersOnTeam).flatMap {
            wave.createEntities(startLocation.world.asNMS(), gameScheduler.waveIndex)
        }
        val byWidth = splitByWidth(allEntities)
        val direction = EnumDirection.fromAngle(startLocation.yaw.toDouble())

        byWidth.forEachIndexed { index, entities ->
            if (entities.isEmpty()) return@forEachIndexed

            gameScheduler.plugin.runDelayed(index.seconds) {
                val livingPlayers = team.livingBukkitPlayers
                if (livingPlayers.isEmpty()) return@runDelayed

                val entitySpawnLocation = startLocation.clone().add(
                    direction.adjacentX.toDouble() * (index + 3),
                    0.0,
                    direction.adjacentZ.toDouble() * (index + 3),
                )
                val spawnLocations = entities.getSpawnLocations(entitySpawnLocation, entities.first().width.toDouble())
                var entityIndex = 0
                spawnLocations.forEach { (entity, loc) ->
                    entity.prepareAndSpawnAt(loc)
                    entity.setGoalTarget(livingPlayers[entityIndex % livingPlayers.size].asNMS(), null, false)
                    entityIndex++
                }
            }
        }
    }

    private fun EntityInsentient.prepareAndSpawnAt(location: Location) {
        val targetSelector = targetSelector
        val modifiedTargetSelectors = targetSelector.b.mapTo(HashSet()) {
            val pathfinderGoal = it.a
            val newPathfinderGoal = if (pathfinderGoal is PathfinderGoalNearestAttackableTarget<*>) {
                @Suppress("UNCHECKED_CAST")
                val predicate = pathfinderGoal.c as Predicate<EntityLiving>

                PathfinderGoalNearestAttackableTarget(
                    pathfinderGoal.e, pathfinderGoal.a, 0, pathfinderGoal.f,
                    (pathfinderGoal as PathfinderGoalTarget).a, predicate
                )
            } else {
                pathfinderGoal
            }

            targetSelector.PathfinderGoalSelectorItem(it.b, newPathfinderGoal)
        }
        targetSelector.b.clear()
        targetSelector.b.addAll(modifiedTargetSelectors)

        spawnAt(location)
    }

    private fun splitByWidth(entities: List<EntityInsentient>): List<List<EntityInsentient>> {
        val sumWidth = entities.sumByDouble { it.width.toDouble() }
        val eachWidth = sumWidth / ROW_COUNT
        val result = ArrayList<List<EntityInsentient>>(ROW_COUNT)
        var currentWidth = 0.0
        var currentList = ArrayList<EntityInsentient>()

        for (entity in entities) {
            currentList.add(entity)
            currentWidth += entity.width
            if (currentWidth >= eachWidth) {
                result.add(currentList)
                currentWidth = 0.0
                currentList = ArrayList()
            }
        }

        if (currentList.isNotEmpty()) {
            result.add(currentList)
        }

        return result
    }
}
