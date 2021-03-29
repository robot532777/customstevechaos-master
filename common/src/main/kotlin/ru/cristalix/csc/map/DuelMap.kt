package ru.cristalix.csc.map

import me.stepbystep.api.MaterialData
import me.stepbystep.mgapi.common.game.GameType
import org.bukkit.Location

data class DuelMap(
    override val id: Int,
    override val name: String,
    override val gameType: GameType?,
    val firstLocation: Location,
    val secondLocation: Location,
    val viewersLocation: Location,
    val displayMaterialData: MaterialData,
) : AbstractMap()
