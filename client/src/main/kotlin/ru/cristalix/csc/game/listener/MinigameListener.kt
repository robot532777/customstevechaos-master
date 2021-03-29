package ru.cristalix.csc.game.listener

import me.stepbystep.api.*
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.itemInHand
import me.stepbystep.mgapi.client.event.CountdownTickEvent
import me.stepbystep.mgapi.client.event.GameForcedStopEvent
import me.stepbystep.mgapi.client.event.GameStartEvent
import net.citizensnpcs.api.CitizensAPI
import net.minecraft.server.v1_12_R1.Items
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.cristalix.core.display.IDisplayService
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.phase.ScheduleNextWavePhase
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.DEFAULT_SPEED
import ru.cristalix.csc.util.asCSCPlayerOrNull
import ru.cristalix.csc.util.getInstantHealthRegeneration
import ru.cristalix.csc.util.isFrozen
import kotlin.time.seconds

class MinigameListener(
    private val gameScheduler: GameScheduler,
) : Listener {

    private val duelScriptMessage = gameScheduler.plugin.loadJavaScriptResource("duelscript.js")
    private val healthScriptMessage = gameScheduler.plugin.loadJavaScriptResource("health_bars.bundle.js")
    private var alreadyCreatedPlayers = false

    @EventHandler
    fun CountdownTickEvent.handle() {
        if (alreadyCreatedPlayers) return

        val secondsLeft = gameScheduler.actor.countdownSeconds - tickOrdinal

        if (secondsLeft <= 15) {
            alreadyCreatedPlayers = true
            gameScheduler.createPlayers()
        }
    }

    @EventHandler
    fun GameStartEvent.handle() {
        gameScheduler.start()
    }

    @EventHandler
    fun GameForcedStopEvent.handle() {
        if (gameScheduler.isGameRunning) {
            val winner = CSCTeam.allAlive().maxByOrNull { it.livesLeft } ?: error("No living players left")
            gameScheduler.completeGame(winner)
        }
    }

    @EventHandler
    fun PlayerJoinEvent.handle() {
        with(player) {
            getAttribute(Attribute.GENERIC_MAX_HEALTH).baseValue = 20.0
            getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).baseValue = 2.0
            walkSpeed = DEFAULT_SPEED
            health = 20.0
            displayName = name
            IDisplayService.get().sendScripts(uniqueId, duelScriptMessage)
            IDisplayService.get().sendScripts(uniqueId, healthScriptMessage)

            gameMode = when {
                CSCPlayer.getLivingPlayers().isNotEmpty() -> GameMode.SPECTATOR
                else -> GameMode.ADVENTURE
            }

            if (gameScheduler.isGameRunning) {
                gameScheduler.tabHandler.sendScript(this)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerQuitEvent.handleTeamQuit() {
        player.asCSCPlayerOrNull()?.let {
            if (it.hasTeam()) {
                it.team.removeMember(it)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerQuitEvent.handle() {
        val cscPlayer = player.asCSCPlayerOrNull()

        if (gameScheduler.actor.isGameStarted) {
            if (cscPlayer != null && cscPlayer.livesLeft > 0) {
                cscPlayer.forceDeath()
            }
            gameScheduler.plugin.runTask(gameScheduler::checkGameFinish)
        }

        player.inventory.clear()
        player.enderChest.clear()
        player.clearPotionEffects()

        gameScheduler.plugin.runDelayed(5.ticks) {
            val playerDataFolder = Bukkit.getWorld("world").worldFolder
                .resolve("playerdata").resolve("${player.uniqueId}.dat")

            if (playerDataFolder.exists()) {
                playerDataFolder.delete()
            }
        }

        CSCPlayer.remove(player.uniqueId)
    }

    @EventHandler
    fun PlayerItemConsumeEvent.handle() {
        if (gameScheduler.currentPhase is ScheduleNextWavePhase) {
            cancel()
            return
        }

        val meta = item.itemMeta as? PotionMeta ?: return
        item = item.apply {
            val effects = meta.customEffects
            meta.clearCustomEffects()
            effects.forEach {
                meta.addCustomEffect(it.withParticles(false), true)
            }
            itemMeta = meta
        }
    }

    @EventHandler
    fun PotionSplashEvent.handle() {
        if (gameScheduler.currentPhase is ScheduleNextWavePhase) {
            cancel()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun PlayerItemConsumeEvent.removeBottle() {
        gameScheduler.plugin.runTask {
            val nmsPlayer = player.asNMS()
            if (nmsPlayer.itemInHand.item == Items.GLASS_BOTTLE) {
                nmsPlayer.itemInHand = NMSItemStack.a
            }
        }
    }

    @EventHandler
    fun ChunkLoadEvent.handle() {
        chunk.entities
            .filterNot(CitizensAPI.getNPCRegistry()::isNPC)
            .forEach(Entity::remove)
    }

    @EventHandler
    fun InventoryClickEvent.handle() {
        if (clickedInventory is CraftingInventory) {
            cancel()
        }
    }

    @EventHandler
    fun InventoryDragEvent.handle() {
        if (inventory is CraftingInventory && rawSlots.any { it <= 4 }) {
            cancel()
        }
    }

    @EventHandler
    fun EntityRegainHealthEvent.handle() {
        if (regainReason != EntityRegainHealthEvent.RegainReason.MAGIC) return
        if (entity !is Player) return

        amount = gameScheduler.getInstantHealthRegeneration(amount)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.handle() {
        val whoDamaged = damager as? Player ?: return
        val entity = entity as? LivingEntity ?: return

        if (whoDamaged.asNMS().itemInHand.isFrozen) {
            entity.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 6.seconds.inTicksInt, 0))
        }
    }
}
