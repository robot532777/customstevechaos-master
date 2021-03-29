package ru.cristalix.csc.entity.animal

import net.minecraft.server.v1_12_R1.Entity
import net.minecraft.server.v1_12_R1.EntityHorse
import net.minecraft.server.v1_12_R1.World
import ru.cristalix.csc.entity.CSCEntity

class CSCEntityHorse(world: World?) : EntityHorse(world), CSCEntity {
    override fun r() {
        initPathfinders()
    }

    override fun B(entity: Entity): Boolean {
        return attackEntity(entity)
    }

    // do not allow passengers
    override fun q(entity: Entity?): Boolean = false
}
