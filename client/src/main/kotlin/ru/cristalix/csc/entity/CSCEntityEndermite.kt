package ru.cristalix.csc.entity

import me.stepbystep.api.inTicksInt
import net.minecraft.server.v1_12_R1.*
import kotlin.time.seconds

class CSCEntityEndermite(world: World?) : EntityEndermite(world) {
    override fun B(entity: Entity?): Boolean {
        return super.B(entity).also {
            if (it && entity is EntityLiving) {
                entity.addEffect(MobEffect(MobEffects.BLINDNESS, 3.seconds.inTicksInt, 1))
            }
        }
    }
}
