package ru.cristalix.csc.entity

import me.stepbystep.api.inTicksInt
import net.minecraft.server.v1_12_R1.*
import kotlin.time.seconds

class CSCEntityCaveSpider(world: World?) : EntityCaveSpider(world) {
    override fun B(entity: Entity?): Boolean {
        return super.B(entity).also {
            if (it && entity is EntityLiving) {
                entity.removeEffect(MobEffects.POISON)
                entity.addEffect(MobEffect(MobEffects.POISON, 8.seconds.inTicksInt, 1))
            }
        }
    }

    // make spider always attack
    override fun aw() = 0F

    // disable jumping
    override fun b(world: World?) = Navigation(this, world)
    override fun m_(): Boolean = false
}
