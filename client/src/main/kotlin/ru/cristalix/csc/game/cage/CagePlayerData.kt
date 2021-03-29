package ru.cristalix.csc.game.cage

import org.bukkit.Location
import ru.cristalix.csc.player.CSCPlayer

data class CagePlayerData(
    val player: CSCPlayer,
    val spawnLocation: Location,
)
