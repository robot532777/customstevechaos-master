package ru.cristalix.csc.entity

import net.minecraft.server.v1_12_R1.Entity
import net.minecraft.server.v1_12_R1.EntityIronGolem
import net.minecraft.server.v1_12_R1.SoundEffects
import net.minecraft.server.v1_12_R1.World

class CSCEntityIronGolem(world: World?) : EntityIronGolem(world), CSCEntity {
    override fun r() {
        initPathfinders()
    }

    // prevent player knocking up on hit
    override fun B(entity: Entity): Boolean {
        val result = attackEntity(entity)

        if (result) {
            bx = 10
            world.broadcastEntityEffect(this, 4.toByte())
            a(SoundEffects.dj, 1.0f, 1.0f)
        }

        return result
    }
}
