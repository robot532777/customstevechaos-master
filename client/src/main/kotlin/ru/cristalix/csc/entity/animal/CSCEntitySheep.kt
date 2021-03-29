package ru.cristalix.csc.entity.animal

import net.minecraft.server.v1_12_R1.Entity
import net.minecraft.server.v1_12_R1.EntitySheep
import net.minecraft.server.v1_12_R1.PathfinderGoalEatTile
import net.minecraft.server.v1_12_R1.World
import ru.cristalix.csc.entity.CSCEntity

class CSCEntitySheep(world: World?) : EntitySheep(world), CSCEntity {
    // avoid NPE in EntitySheep::M
    init {
        bC = PathfinderGoalEatTile(this)
    }

    override fun r() {
        initPathfinders()
    }

    override fun B(entity: Entity): Boolean {
        return attackEntity(entity)
    }
}
