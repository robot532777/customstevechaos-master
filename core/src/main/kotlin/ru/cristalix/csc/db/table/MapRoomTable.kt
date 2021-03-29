package ru.cristalix.csc.db.table

import me.stepbystep.api.db.location
import org.bukkit.Bukkit
import org.bukkit.Location
import org.jetbrains.exposed.sql.Table
import ru.cristalix.csc.withTablePrefix

object MapRoomTable : Table("mapRoom") {
    override val tableName = super.tableName.withTablePrefix()

    val mapID = integer("id")
    val spawnLocation = location("spawnLocation")
    val mobsLocation = location("mobsLocation").clientDefault {
        Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)
    }
}
