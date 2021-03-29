package ru.cristalix.csc.entity

import net.minecraft.server.v1_12_R1.EntitySpider
import net.minecraft.server.v1_12_R1.Navigation
import net.minecraft.server.v1_12_R1.World
import ru.cristalix.csc.util.speed

class CSCEntitySpider(world: World?) : EntitySpider(world) {
    var appliedSpeedModifier: Double = 1.0; private set

    // make spider always attack
    override fun aw() = 0F

    // disable jumping
    override fun b(world: World) = Navigation(this, world)
    override fun m_(): Boolean = false

    fun modifySpeed(modifier: Double) {
        speed *= modifier
        appliedSpeedModifier *= modifier
    }
}
