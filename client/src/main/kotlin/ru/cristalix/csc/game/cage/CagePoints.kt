package ru.cristalix.csc.game.cage

import org.bukkit.util.Vector

object CagePoints {
    private fun vector(x: Int = 0, y: Int = 0, z: Int = 0) = Vector(x, y, z)

    private fun <T, R> Iterable<T>.groupWith(other: Iterable<R>): List<Pair<T, R>> {
        return associateWith { other }.toList().flatMap { (x, y) ->
            y.map { x to it }
        }
    }

    val Bottom = arrayOf(
        vector(x = 1, y = -1, z = 1), vector(y = -1, z = 1), vector(x = -1, y = -1, z = 1),
        vector(x = 1, y = -1), vector(y = -1), vector(x = -1, y = -1),
        vector(x = 1, y = -1, z = -1), vector(y = -1, z = -1), vector(x = -1, y = -1, z = -1),
    )

    val Top = Bottom.map { it.clone().setY(3) }.toTypedArray()

    val TopAndBottom = Bottom + Top

    private val CrossBottom = Bottom.filter { it.blockX == 0 || it.blockZ == 0 }.toTypedArray()
    private val CrossTop = Top.filter { it.blockX == 0 || it.blockZ == 0 }.toTypedArray()

    val Walls = (-1..1).groupWith(-1..1)
        .filter { it.first != 0 || it.second != 0 }
        .map { vector(x = it.second, z = it.first) }
        .flatMap { vec ->
            (0..2).map { vector(vec.blockX, it, vec.blockZ) }
        }.toTypedArray()

    val CrossWalls = Walls.filter { it.blockX == 0 || it.blockZ == 0 }.toTypedArray()
    val CornerWalls = (Walls.toList() - CrossWalls.toList()).toTypedArray()

    val Cross = CrossBottom + CrossTop + CrossWalls
    val Fill = Bottom + Top + Walls
    val FillAndCross = Bottom + Top + CrossWalls
}
