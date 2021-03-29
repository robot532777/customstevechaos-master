package ru.cristalix.csc.entity

import me.stepbystep.api.inTicksInt
import net.minecraft.server.v1_12_R1.*
import kotlin.time.seconds

class CSCEntitySkeletonWither(world: World?) : EntitySkeletonWither(world) {
    override fun B(entity: Entity?): Boolean {
        return super.B(entity).also {
            if (it && entity is EntityLiving) {
                entity.removeEffect(MobEffects.WITHER)
                entity.addEffect(MobEffect(MobEffects.WITHER, 5.seconds.inTicksInt, 0))
            }
        }
    }
}