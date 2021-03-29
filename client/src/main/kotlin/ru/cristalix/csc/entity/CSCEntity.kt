package ru.cristalix.csc.entity

import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.bukkit.event.entity.EntityCombustByEntityEvent
import ru.cristalix.csc.entity.animal.PathfinderGoalRandomMove

interface CSCEntity {
    @JvmDefault
    fun initPathfinders() {
        require(this is EntityCreature) { "$this is not EntityCreature" }
        clearPathfinders()

        registerTargetPathfinder()
        goalSelector.a(1, PathfinderGoalMeleeAttack(this, 1.0, true))
        goalSelector.a(0, PathfinderGoalRandomMove(this))
    }

    fun registerTargetPathfinder() {
        require(this is EntityCreature) { "$this is not EntityCreature" }

        val targetPathfinder = PathfinderGoalNearestAttackableTarget(this, EntityPlayer::class.java, true)
        targetSelector.a(0, targetPathfinder)
    }

    fun clearPathfinders() {
        require(this is EntityCreature) { "$this is not EntityCreature" }

        val methodProfiler = world.methodProfiler
        goalSelector = PathfinderGoalSelector(methodProfiler)
        targetSelector = PathfinderGoalSelector(methodProfiler)
    }

    fun attackEntity(target: Entity): Boolean {
        require(this is EntityCreature) { "$this is not EntityCreature" }

        var f = getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).value.toFloat()
        var i = 0
        if (target is EntityLiving) {
            f += EnchantmentManager.a(itemInMainHand, target.monsterType)
            i += EnchantmentManager.b(this)
        }
        val flag = target.damageEntity(DamageSource.mobAttack(this), f)
        if (flag) {
            if (i > 0 && target is EntityLiving) {
                target.a(
                    this, i.toFloat() * 0.5f, MathHelper.sin(yaw * 0.017453292f).toDouble(),
                    (-MathHelper.cos(yaw * 0.017453292f)).toDouble()
                )
                motX *= 0.6
                motZ *= 0.6
            }
            val j = EnchantmentManager.getFireAspectEnchantmentLevel(this)
            if (j > 0) {
                val combustEvent = EntityCombustByEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), j * 4)
                Bukkit.getPluginManager().callEvent(combustEvent)
                if (!combustEvent.isCancelled) {
                    target.setOnFire(combustEvent.duration)
                }
            }
            if (target is EntityHuman) {
                val itemInHand = itemInMainHand
                val targetItem = if (target.isHandRaised) target.cJ() else ItemStack.a

                if (!itemInHand.isEmpty && !targetItem.isEmpty && itemInHand.getItem() is ItemAxe && targetItem.getItem() === Items.SHIELD) {
                    val f1 = 0.25f + EnchantmentManager.getDigSpeedEnchantmentLevel(this).toFloat() * 0.05f
                    if (random.nextFloat() < f1) {
                        target.cooldownTracker.a(Items.SHIELD, 100)
                        world.broadcastEntityEffect(target, 30.toByte())
                    }
                }
            }
            a(this, target)
        }

        return flag
    }
}
