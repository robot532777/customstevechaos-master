package ru.cristalix.csc.game.listener.gameplay

import me.stepbystep.api.asNMS
import me.stepbystep.api.cancel
import me.stepbystep.api.inTicksInt
import me.stepbystep.api.item.clear
import me.stepbystep.api.runTask
import net.minecraft.server.v1_12_R1.Items
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.util.*
import kotlin.time.seconds

class RefillableItemsListener(private val gameScheduler: GameScheduler) : Listener {
    @EventHandler
    fun PlayerItemDamageEvent.handleBow() {
        if (item.type != Material.BOW) return
        if (item.durability + damage < item.type.maxDurability) return

        player.asCSCPlayer().refillables.bindSlot(player.inventory.heldItemSlot) {
            it.item == Items.BOW
        }
    }

    @EventHandler
    fun PlayerItemDamageEvent.handleShield() {
        damage = 0
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.handle() {
        if (damage <= 0) return
        val player = entity as? Player ?: return
        if (!player.isBlocking) return

        val activeItem = player.asNMS().activeItem
        val newDurability = activeItem.shieldDurability - 1
        activeItem.shieldDurability = newDurability

        gameScheduler.plugin.runTask {
            val durabilityPart = 1f - newDurability.toFloat() / activeItem.maxShieldDurability
            if (durabilityPart < 1) {
                activeItem.damage = (durabilityPart * Material.SHIELD.maxDurability).toInt()
                activeItem.updateLoreIfNeed(player, gameScheduler)
            } else {
                activeItem.clear()

                val cscPlayer = player.asCSCPlayerOrNull() ?: return@runTask
                val slot = player.inventory.heldItemSlot
                cscPlayer.refillables.bindSlot(slot) { it.item == Items.SHIELD }
            }
        }
    }

    @EventHandler
    fun PlayerItemConsumeEvent.handle() {
        val inventory = player.inventory.asNMS()
        player.asCSCPlayer().refillables.bindSlot(inventory.itemInHandIndex, inventory.itemInHand)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerInteractEvent.handle() {
        val cscPlayer = player.asCSCPlayerOrNull() ?: return
        if (item?.type == Material.SPLASH_POTION) {
            val inventory = player.inventory.asNMS()
            cscPlayer.refillables.bindSlot(inventory.itemInHandIndex, inventory.itemInHand)
        }
    }

    @EventHandler
    fun PlayerItemConsumeEvent.handleGapple() {
        if (item.type != Material.GOLDEN_APPLE) return

        gameScheduler.plugin.runTask {
            val regenEffect = player.getPotionEffect(PotionEffectType.REGENERATION) ?: return@runTask
            if (regenEffect.duration > 5.seconds.inTicksInt) return@runTask
            if (regenEffect.amplifier != 1) return@runTask

            val newAmplifier = gameScheduler.waveIndex / 6
            player.removePotionEffect(PotionEffectType.REGENERATION)
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, regenEffect.duration, newAmplifier))
        }
    }

    @EventHandler
    fun InventoryClickEvent.handle() {
        if (isShiftClick && currentItem?.type == Material.SHIELD) {
            cancel()
        }
    }
}
