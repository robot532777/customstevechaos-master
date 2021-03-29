package ru.cristalix.csc.entity

import net.minecraft.server.v1_12_R1.*

class CSCEntityWolf(world: World?) : EntityWolf(world) {
    init {
        isAngry = true
    }

    override fun r() {
        goalSit = PathfinderGoalSit(this)
        goalSelector.a(1, PathfinderGoalFloat(this))
        goalSelector.a(2, goalSit)
        goalSelector.a(4, PathfinderGoalLeapAtTarget(this, 0.4f))
        goalSelector.a(5, PathfinderGoalMeleeAttack(this, 1.0, true))
        goalSelector.a(8, PathfinderGoalRandomStrollLand(this, 1.0))
        goalSelector.a(9, PathfinderGoalBeg(this, 8.0f))
        goalSelector.a(10, PathfinderGoalLookAtPlayer(this, EntityHuman::class.java, 8.0f))
        goalSelector.a(10, PathfinderGoalRandomLookaround(this))
        targetSelector.a(0, PathfinderGoalNearestAttackableTarget(this, EntityHuman::class.java, true))
    }
}