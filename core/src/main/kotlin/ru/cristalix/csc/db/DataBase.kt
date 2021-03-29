package ru.cristalix.csc.db

import me.stepbystep.api.asNMS
import me.stepbystep.api.objects.ExposedDatabase
import me.stepbystep.api.to
import me.stepbystep.mgapi.lobby.minigameConfig
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import ru.cristalix.csc.CSCItemStack
import ru.cristalix.csc.db.table.*
import ru.cristalix.csc.map.DuelMap
import ru.cristalix.csc.map.GameMap
import ru.cristalix.csc.map.RoomData
import ru.cristalix.csc.uuid
import java.util.*

class DataBase(plugin: Plugin) : ExposedDatabase(plugin, plugin.minigameConfig) {
    fun selectMaps(): List<GameMap> = withTransaction {
        MapTable.selectAll().map {
            GameMap(
                id = it[MapTable.id].value,
                name = it[MapTable.name],
                gameType = it[MapTable.gameType],
                roomRadius = it[MapTable.radius],
                rooms = selectRooms(it[MapTable.id].value, it[MapTable.radius]),
            )
        }
    }

    fun selectDuelMaps(): List<DuelMap> = withTransaction {
        DuelMapTable.selectAll().map {
            DuelMap(
                id = it[DuelMapTable.id].value,
                name = it[DuelMapTable.name],
                gameType = it[DuelMapTable.gameType],
                firstLocation = it[DuelMapTable.firstLocation],
                secondLocation = it[DuelMapTable.secondLocation],
                viewersLocation = it[DuelMapTable.viewersLocation],
                displayMaterialData = it[DuelMapTable.displayMaterial] to it[DuelMapTable.displayData],
            )
        }
    }

    fun selectItems(): List<CSCItemStack> = withTransaction {
        val result = arrayListOf<CSCItemStack>()
        ItemTable.selectAll().forEach {
            val stack = it[ItemTable.stack].asNMS()
            val price = it[ItemTable.price]
            val previousItem = result.findPreviousItem(it[ItemTable.previous]) // check that item exists
            val item = CSCItemStack(previousItem?.uuid, stack, price, mutableListOf())
            previousItem?.upgrades?.add(item)
            result.add(item)
        }
        result
    }

    private fun List<CSCItemStack>.findPreviousItem(uuid: UUID?): CSCItemStack? {
        if (uuid == null) return null
        val result = find { it.uuid == uuid }
        if (result == null) {
            Bukkit.getLogger().severe("Item with uuid = $uuid not found in $this (size = $size)")
        }
        return result
    }

    private fun selectRooms(id: Int, radius: Int): List<RoomData> {
        return MapRoomTable.select { MapRoomTable.mapID eq id }.map {
            val spawnLocation = it[MapRoomTable.spawnLocation]
            val mobsLocation = it[MapRoomTable.mobsLocation]
            RoomData(spawnLocation, mobsLocation, radius)
        }
    }

    override fun getTables() = listOf(
        DuelMapTable, ItemTable, MapRoomTable, MapTable, PlayerTable, DonateTable, ClassSelectionTable,
    )
}
