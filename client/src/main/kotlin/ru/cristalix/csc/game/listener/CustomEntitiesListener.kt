package ru.cristalix.csc.game.listener

import me.stepbystep.api.RANDOM
import me.stepbystep.api.asNMS
import me.stepbystep.api.cancel
import me.stepbystep.api.runTask
import net.minecraft.server.v1_12_R1.EntityArrow
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.plugin.Plugin
import org.spigotmc.event.entity.EntityDismountEvent
import ru.cristalix.csc.entity.CSCEntityBlaze
import ru.cristalix.csc.entity.CSCEntityIronGolem
import ru.cristalix.csc.entity.CSCEntityPigZombie
import ru.cristalix.csc.entity.CSCEntitySpider
import ru.cristalix.csc.util.speed

class CustomEntitiesListener(private val plugin: Plugin) : Listener {
    @EventHandler
    fun EntityCombustByEntityEvent.handle() {
        val projectile = combuster as? Projectile ?: return
        val shooter = projectile.shooter as? Blaze ?: return
        if (shooter.asNMS() is CSCEntityBlaze) {
            duration = 6
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.handlePigZombie() {
        if (damager is Arrow && entity.asNMS() is CSCEntityPigZombie) {
            damage /= 2
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.handleIronGolem() {
        if (damager.asNMS() !is CSCEntityIronGolem) return
        if (RANDOM.nextFloat() >= 0.1f) return
        val player = entity as? Player ?: return

        damage *= 2
        plugin.runTask {
            player.asNMS().motY += 0.5
            player.asNMS().velocityChanged = true
        }
        player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
    }

    @EventHandler
    fun EntityTargetLivingEntityEvent.handle() {
        if (entity !is Player && target !is Player) {
            cancel()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun EntityDamageByEntityEvent.handleFireball() {
        val damager = damager as? Fireball ?: return
        val shooter = damager.shooter as? LivingEntity ?: return

        damage = shooter.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).baseValue
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun ProjectileLaunchEvent.handle() {
        val arrow = entity as? Arrow ?: return
        val shooter = arrow.shooter as? LivingEntity ?: return
        val shooterDamage = shooter.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).baseValue
        val nmsArrow = arrow.asNMS() as EntityArrow

        nmsArrow.damage = when (shooter) {
            is Player -> nmsArrow.damage + shooterDamage
            else -> shooterDamage
        }
    }

    @EventHandler
    fun EntitySpawnEvent.handle() {
        val entity = entity as? Creature ?: return

        entity.isCustomNameVisible = true
    }

    @EventHandler
    fun EntityDismountEvent.handle() {
        val spider = dismounted.asNMS() as? CSCEntitySpider ?: return
        plugin.runTask {
            spider.speed /= spider.appliedSpeedModifier
        }
    }
}
