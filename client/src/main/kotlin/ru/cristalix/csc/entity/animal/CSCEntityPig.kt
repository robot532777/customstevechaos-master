package ru.cristalix.csc.entity.animal

import net.minecraft.server.v1_12_R1.Entity
import net.minecraft.server.v1_12_R1.EntityPig
import net.minecraft.server.v1_12_R1.World
import ru.cristalix.csc.entity.CSCEntity

class CSCEntityPig(world: World?) : EntityPig(world), CSCEntity {
    override fun r() {
        initPathfinders()
    }

    override fun B(entity: Entity): Boolean {
        return attackEntity(entity)
    }
}
