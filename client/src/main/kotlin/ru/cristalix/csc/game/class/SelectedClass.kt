package ru.cristalix.csc.game.`class`

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import net.minecraft.server.v1_12_R1.MobEffect
import net.minecraft.server.v1_12_R1.PotionRegistry
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_12_R1.potion.CraftPotionUtil
import org.bukkit.entity.Arrow
import org.bukkit.entity.Creature
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.Plugin
import ru.cristalix.csc.event.*
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.shop.item.CSCClass
import ru.cristalix.csc.util.DEFAULT_SPEED
import kotlin.math.roundToInt
import kotlin.reflect.full.isSubclassOf

sealed class SelectedClass(
    val wrapped: CSCClass,
    val requiredDonate: CSCDonate? = null
) : Listener {
    companion object {
        val VALUES by lazy {
            SelectedClass::class.nestedClasses
                .filter { it.isSubclassOf(SelectedClass::class) }
                .mapNotNull { it.objectInstance as SelectedClass? }
        }
    }

    open fun setup(plugin: Plugin) {
        register(plugin)
    }

    open fun onSelect(cscPlayer: CSCPlayer) {
        // nothing
    }

    protected fun Entity.hasWrongClass(): Boolean {
        if (this !is Player) return true
        val cscPlayer = CSCPlayer.getOrNull(uniqueId)
        return cscPlayer == null || cscPlayer.hasWrongClass()
    }

    protected fun CSCPlayer.hasWrongClass(): Boolean = selectedClass != this@SelectedClass

    open class SelectedDonateClass(donate: CSCDonate.GameClassDonate) : SelectedClass(donate.clazz, donate)

    object Archer : SelectedClass(CSCClass.Archer) {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (damager !is Arrow) return
            if (damager.getActualDamager().hasWrongClass()) return

            damage *= 1.18
        }
    }

    object Swordsman : SelectedClass(CSCClass.Swordsman) {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (damager.hasWrongClass()) return

            damage *= 1.15
        }
    }

    object Berserk : SelectedDonateClass(CSCDonate.Berserk) {
        override fun setup(plugin: Plugin) {
            super.setup(plugin)

            plugin.runRepeating(0, 10) {
                for (player in CSCPlayer.getLivingPlayers()) {
                    player.applyStatsIfNeed()
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun EntityDamageByEntityEvent.handle() {
            val player = damager.getActualDamager() as? Player ?: return
            if (player.hasWrongClass()) return

            damage *= player.getStatsMultiplier()
        }

        private fun CSCPlayer.applyStatsIfNeed() {
            if (hasWrongClass()) return

            val player = asBukkitPlayer()
            player.walkSpeed = DEFAULT_SPEED * player.getStatsMultiplier()
        }

        private fun Player.getStatsMultiplier(): Float {
            val healthPart = player.health / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).value
            return when {
                healthPart > 0.425 -> 1.0f
                healthPart > 0.225 -> 1.1f
                healthPart > 0.125 -> 1.15f
                else -> 1.2f
            }
        }
    }

    object Lucky : SelectedClass(CSCClass.Lucky) {
        @EventHandler
        fun PlayerEarnMoneyEvent.handle() {
            if (player.hasWrongClass()) return
            if (cause != PlayerEarnMoneyEvent.Cause.WaveComplete) return

            multiplier = 1.25
        }
    }

    object Gladiator : SelectedClass(CSCClass.Gladiator) {
        @EventHandler
        fun PlayerEarnMoneyEvent.handle() {
            if (player.hasWrongClass()) return
            if (cause != PlayerEarnMoneyEvent.Cause.DuelVictory) return

            multiplier = 1.45
        }
    }

    object Alchemist : SelectedClass(CSCClass.Alchemist) {
        private lateinit var plugin: Plugin

        override fun setup(plugin: Plugin) {
            super.setup(plugin)
            this.plugin = plugin
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun PlayerItemConsumeEvent.handle() {
            if (player.hasWrongClass()) return

            plugin.runTask {
                val meta = item.itemMeta as? PotionMeta ?: return@runTask
                val key = CraftPotionUtil.fromBukkit(meta.basePotionData)
                val potionRegistry = PotionRegistry.a(key) ?: error("Unknown potion: $key")
                val nmsPlayer = player.asNMS()

                potionRegistry.e.forEach {
                    val newEffect = MobEffect(it.b, it.duration - 1, it.amplification + 1, it.ambient, it.h)
                    nmsPlayer.addEffect(newEffect)
                }
            }
        }
    }

    object Titan : SelectedClass(CSCClass.Titan) {
        override fun onSelect(cscPlayer: CSCPlayer) {
            cscPlayer.otherAdditionalHealth += 8
            cscPlayer.updateMaxHealth()
        }
    }

    object Trickster : SelectedClass(CSCClass.Trickster) {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (entity.hasWrongClass()) return
            if (RANDOM.chanceGoodEnough(0.2f)) {
                cancel()
            }
        }
    }

    object WeaponMaster : SelectedClass(CSCClass.WeaponMaster) {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (damager.getActualDamager().hasWrongClass()) return
            if (RANDOM.chanceGoodEnough(0.07f)) {
                damage *= 2
            }
        }
    }

    object Assassin : SelectedClass(
        CSCClass.Assassin
    ) {
        override fun onSelect(cscPlayer: CSCPlayer) {
            cscPlayer.asBukkitPlayer().walkSpeed = DEFAULT_SPEED * 1.2f
        }
    }

    object Gamer : SelectedClass(CSCClass.Gamer) {
        @EventHandler
        fun PlayerEarnMoneyEvent.handle() {
            if (player.hasWrongClass()) return
            if (cause != PlayerEarnMoneyEvent.Cause.DuelBet) return

            multiplier = 1.2
        }
    }

    object Careful : SelectedDonateClass(CSCDonate.Careful) {
        private val returnedWord = message0(
            russian = "Возвращено",
            english = "Returned",
        )

        @EventHandler
        fun DuelSuccessfullyCompleteEvent.handle() {
            for ((uuid, bet) in duelPhase.getOtherParticipant(winner).bets) {
                val cscPlayer = CSCPlayer.getOrNull(uuid) ?: continue
                if (!cscPlayer.isAlive) continue

                cscPlayer.handleWrongBet(bet)
            }
        }

        private fun CSCPlayer.handleWrongBet(bet: Int) {
            if (hasWrongClass()) return

            val returnedSum = (bet * 0.4).toInt()
            if (returnedSum == 0) return

            val bukkitPlayer = asBukkitPlayer()
            bukkitPlayer.sendMessage("$GREEN${returnedWord(bukkitPlayer)}: $returnedSum")
            changeGold(returnedSum)
        }
    }

    object Revivalist : SelectedDonateClass(CSCDonate.Revivalist) {
        override fun onSelect(cscPlayer: CSCPlayer) {
            cscPlayer.addLife()
        }
    }

    object Collector : SelectedDonateClass(CSCDonate.Collector) {
        @EventHandler
        fun WaveStatusEvent.handle() {
            if (status != WaveStatusEvent.Status.Complete) return
            if (displayWaveIndex < 3) return

            for (player in CSCPlayer.getLivingPlayers()) {
                if (player.hasWrongClass()) continue
                player.earnedCoins += 3
            }
        }
    }

    object MonsterHunter : SelectedDonateClass(CSCDonate.MonsterHunter) {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            val damager = damager.getActualDamager() as? Player ?: return
            if (damager.hasWrongClass()) return
            if (entity !is Creature) return

            damage *= 1.15
        }
    }

    object Vampire : SelectedDonateClass(CSCDonate.Vampire) {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (RANDOM.nextFloat() >= 0.125f) return
            val damager = damager.getActualDamager() as? Player ?: return
            if (damager.hasWrongClass()) return
            if (damager.isDead) return

            damager.heal(damage * 0.25)
        }
    }

    object Pufferfish : SelectedDonateClass(CSCDonate.Pufferfish) {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        fun EntityDamageByEntityEvent.handle() {
            if (entity.hasWrongClass()) return

            val damagerVector = damager.location.toVector().subtract(entity.location.toVector())
            val entityVector = entity.location.direction
            val angle = Math.toDegrees(damagerVector.angle(entityVector).toDouble())

            damage *= if (angle > 135) 0.625 else 0.9
        }
    }

    object Killer : SelectedDonateClass(CSCDonate.Killer) {
        private val criticalStrikeMessage = message0(
            russian = "${RED}Критический удар!",
            english = "${RED}Critical hit!"
        )
        @EventHandler
        fun EntityDamageByEntityEvent.handle() {
            val damager = damager.getActualDamager() as? Player ?: return
            if (RANDOM.nextFloat() >= 0.01f) return
            if (damager.hasWrongClass()) return

            val entity = entity
            if (entity !is Creature) return

            entity.health = 0.0
            damager.sendMessage(criticalStrikeMessage)
        }
    }

    object TreasureHunter : SelectedDonateClass(CSCDonate.TreasureHunter) {
        @EventHandler
        fun MonsterItemDropAttemptEvent.handle() {
            if (player.hasWrongClass()) return

            chancePercents *= 2
        }
    }

    object Blacksmith : SelectedDonateClass(CSCDonate.Blacksmith) {
        @EventHandler
        fun PrePlayerSpendMoneyEvent.handle() {
            if (cause != PlayerSpendMoneyEvent.Cause.UpgradeItem) return
            if (player.hasWrongClass()) return

            amount = (amount * 0.875).roundToInt()
        }
    }
}
