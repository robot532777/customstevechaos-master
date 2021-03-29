package ru.cristalix.csc.entity.animal

import me.stepbystep.api.RANDOM
import me.stepbystep.api.startMoving
import net.minecraft.server.v1_12_R1.EntityCreature
import net.minecraft.server.v1_12_R1.PathfinderGoal
import net.minecraft.server.v1_12_R1.RandomPositionGenerator
import net.minecraft.server.v1_12_R1.Vec3D

class PathfinderGoalRandomMove(
    private val entity: EntityCreature,
    private val speedMultiplier: Double = 1.0,
    private val chanceOfMoving: Int = 120,
) : PathfinderGoal() {
    private var nextX = 0.0
    private var nextY = 0.0
    private var nextZ = 0.0

    init {
        a(1)
    }

    override fun a(): Boolean {
        if (entity.bW() >= 100) return false
        if (RANDOM.nextInt(chanceOfMoving) != 0) return false
        val nextPoint = getNextPoint() ?: return false

        nextX = nextPoint.x
        nextY = nextPoint.y
        nextZ = nextPoint.z
        return true
    }

    override fun b() = !entity.navigation.o()

    override fun c() {
        entity.startMoving(nextX, nextY, nextZ, speedMultiplier)
    }

    private fun getNextPoint(): Vec3D? {
        return if (entity.isInWater) {
            RandomPositionGenerator.b(entity, 15, 7) ?: RandomPositionGenerator.b(entity, 10, 7)
        } else {
            RandomPositionGenerator.b(entity, 10, 7)
        }
    }
}
