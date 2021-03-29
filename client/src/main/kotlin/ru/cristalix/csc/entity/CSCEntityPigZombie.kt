package ru.cristalix.csc.entity

import net.minecraft.server.v1_12_R1.EntityPigZombie
import net.minecraft.server.v1_12_R1.World

class CSCEntityPigZombie(world: World?) : EntityPigZombie(world), CSCEntity {
    override fun do_() {
        registerTargetPathfinder()
    }
}
