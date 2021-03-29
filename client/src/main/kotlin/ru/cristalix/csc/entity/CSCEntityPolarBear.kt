package ru.cristalix.csc.entity

import net.minecraft.server.v1_12_R1.*

class CSCEntityPolarBear(world: World?) : EntityPolarBear(world) {

    override fun r() {
        goalSelector.a(0, PathfinderGoalFloat(this))
        goalSelector.a(1, PathfinderGoalPolarBearAttack())
        goalSelector.a(5, PathfinderGoalRandomStroll(this, 1.0))
        goalSelector.a(6, PathfinderGoalLookAtPlayer(this, EntityHuman::class.java, 6.0f))
        goalSelector.a(7, PathfinderGoalRandomLookaround(this))
        targetSelector.a(0, PathfinderGoalNearestAttackableTarget(this, EntityHuman::class.java, true))
    }

    inner class PathfinderGoalPolarBearAttack : PathfinderGoalMeleeAttack(this@CSCEntityPolarBear, 1.25, true) {

        init {
            p(false)
            d()
        }

        override fun a(entity: EntityLiving, dist: Double) {
            val var4 = this.a(entity)
            if (dist <= var4 && c <= 0) {
                c = 20
                b.B(entity)
                p(false)
            } else if (dist <= var4 * 2.0) {
                if (c <= 0) {
                    p(false)
                    c = 20
                }
                if (c <= 10) {
                    p(true)
                    dl()
                }
            } else {
                c = 20
                p(false)
            }
        }

        override fun a(entity: EntityLiving): Double {
            return (4.0f + entity.width).toDouble()
        }
    }
}
