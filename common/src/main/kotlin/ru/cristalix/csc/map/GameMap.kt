package ru.cristalix.csc.map

import me.stepbystep.api.objects.BoundingBox
import me.stepbystep.mgapi.common.game.GameType
import org.bukkit.Location

data class GameMap(
    override val id: Int,
    override val name: String,
    override val gameType: GameType,
    val roomRadius: Int,
    val rooms: List<RoomData>,
) : AbstractMap()

data class RoomData(
    val spawnLocation: Location,
    val mobsLocation: Location,
    val border: BoundingBox,
) {
    constructor(spawnLocation: Location, mobsLocation: Location, radius: Int) :
            this(spawnLocation, mobsLocation, BoundingBox(spawnLocation, radius).copy(minY = 0.0, maxY = 256.0))
}
