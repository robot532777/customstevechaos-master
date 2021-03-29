package ru.cristalix.csc

import com.google.common.collect.HashMultimap
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import me.stepbystep.api.NMSEntity
import me.stepbystep.api.asNMS
import me.stepbystep.api.cancel
import me.stepbystep.api.getActualDamager
import me.stepbystep.api.item.itemInHand
import net.minecraft.server.v1_12_R1.EntityPlayer
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffectType
import ru.cristalix.csc.enchant.ItemEnchant
import ru.cristalix.csc.enchant.enchants
import ru.cristalix.csc.enchant.type.ArmorEnchant
import ru.cristalix.csc.enchant.type.SwordEnchant

// TODO: move to client module
class CustomEnchantsListener(private val plugin: Plugin) : Listener {

    @Suppress("UNCHECKED_CAST")
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.handle() {
        val damagedEnchants = entity.asNMS().collectEnchants()
        val actualDamager = damager.getActualDamager() as LivingEntity

        // if dodge was applied, do not apply other enchants
        damagedEnchants.removeAll(ArmorEnchant.DODGE).forEach {
            ArmorEnchant.DODGE.applyForDamaged(this, actualDamager, it)
        }
        if (isCancelled) return

        val damagerEnchants = when (val damager = damager) {
            is Projectile -> damager.getMetadata(ARROW_KEY).single().value() as Multimap<ItemEnchant, ByteArray>
            else -> damager.asNMS().collectEnchants()
        }
        if (!damagerEnchants.isEmpty) {
            val playerDamager = actualDamager as Player
            damage = playerDamager.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).baseValue
            damagerEnchants.forEach { enchant, data ->
                enchant.applyForDamager(this, playerDamager, data)
            }
        }

        damagedEnchants.forEach { enchant, data ->
            enchant.applyForDamaged(this, actualDamager, data)
        }
    }

    @EventHandler
    fun EntityShootBowEvent.handle() {
        val shooter = entity as? Player ?: return
        val enchants = shooter.asNMS().collectEnchants()
        projectile.setMetadata(ARROW_KEY, FixedMetadataValue(plugin, enchants))
    }

    @EventHandler
    fun EntityTargetLivingEntityEvent.handle() {
        val entity = entity as? LivingEntity ?: return
        if (entity.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            cancel()
        }
    }

    @EventHandler
    fun EntityDamageEvent.handle() {
        if (cause != EntityDamageEvent.DamageCause.FIRE_TICK) return
        val fireMetadata = entity.getMetadata(SwordEnchant.IGNITE.key).singleOrNull() ?: return
        damage = fireMetadata.asDouble()
    }

    private fun NMSEntity.collectEnchants(): Multimap<ItemEnchant, ByteArray> {
        // cannot return ImmutableMultiMap, because we need to remove elements from it
        if (this !is EntityPlayer) return HashMultimap.create()

        val result = LinkedHashMultimap.create<ItemEnchant, ByteArray>()
        inventory.armor.flatMap { it.enchants.entries }.forEach {
            result.put(it.key, it.value)
        }
        itemInHand.enchants.forEach(result::put)
        return result
    }

    private companion object {
        private const val ARROW_KEY = "customArrow"
    }
}
