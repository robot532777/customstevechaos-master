package ru.cristalix.csc.command

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.command.*
import org.jetbrains.exposed.sql.*
import ru.cristalix.csc.CSCGameType
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.MapRoomTable
import ru.cristalix.csc.db.table.MapTable
import ru.cristalix.csc.map.GameMap
import ru.cristalix.csc.map.RoomData

class MapCommand(
    private val dataBase: DataBase,
    maps: MutableList<GameMap>,
    private val maxPlayers: Int,
) : AbstractMapCommand<GameMap>("map", "Настроить карты", maps) {

    override fun UUIDLiteralArgumentBuilder.configure() {
        addChild("create", "Создать карту") {
            dataBase.transaction {
                val id = MapTable.insertAndGetId {
                    it[name] = ""
                    it[radius] = 0
                }.value

                val map = GameMap(id, "", CSCGameType.Solo, 0, emptyList())
                maps.add(map)
                selectedMaps[it.source] = map
                it.sender.sendMessage("${GREEN}Вы успешно создали карту с ID = $id")
            }
        }

        addChild("list", "Посмотреть все карты") {
            val player = it.sender
            if (maps.isNotEmpty()) {
                player.sendMessage("${GREEN}Все карты:")
                for ((id, name, gameType, roomRadius, rooms) in maps) {
                    player.sendMessage("    $GREEN$id $GRAY- $GOLD\"$name\" $GREEN(радиус = $roomRadius, точек спавна = ${rooms.size}, тип игры = ${gameType::class.java.simpleName})")
                }
            } else {
                player.sendMessage("${RED}Еще не создано ни 1 карты")
            }
        }

        addSelectedMapChild(
            "tpspawn",
            "Телепортироваться на спавн выбранной карты",
            uIntParameter("номер спавна")
        ) { map, ctx ->
            val index = ctx.getArgument<Int>("номер спавна")
            if (index !in map.rooms.indices) {
                ctx.sender.sendMessage("${RED}Такого спавна не существует (максимальный = ${map.rooms.lastIndex})")
                return@addSelectedMapChild
            }
            val room = map.rooms[index]
            ctx.sender.teleport(room.spawnLocation)
            ctx.sender.sendMessage("${GREEN}Вы телепортировались на спавн $GOLD$index ${GREEN}карты ${map.chatName}")
        }

        addSelectedMapChild(
            "tpmobs",
            "Телепортироваться на точку спавна мобов выбранной карты",
            uIntParameter("номер спавна")
        ) { map, ctx ->
            val index = ctx.getArgument<Int>("номер спавна")
            if (index > map.rooms.lastIndex) {
                ctx.sender.sendMessage("${RED}Такого спавна не существует (максимальный = ${map.rooms.lastIndex})")
                return@addSelectedMapChild
            }
            val room = map.rooms[index]
            ctx.sender.teleport(room.mobsLocation)
            ctx.sender.sendMessage("${GREEN}Вы телепортировались на точку спавна мобов $GOLD$index ${GREEN}карты ${map.chatName}")
        }

        addBuilderChild(
            "name",
            "Установить имя карты",
            longStringParameter("имя"),
        ) { map, ctx ->
            val newName = ctx.getArgument<String>("имя")
            val newMap = map.copy(name = newName)
            dataBase.transaction {
                MapTable.update(where = { MapTable.id eq map.id }) {
                    it[name] = newName
                }
            }
            newMap
        }

        addBuilderChild(
            "radius",
            "Установить радиус комнаты",
            uIntParameter("радиус"),
        ) { map, ctx ->
            val newRadius = ctx.getArgument<Int>("радиус")
            val newRooms = map.rooms.map {
                RoomData(it.spawnLocation, it.mobsLocation, newRadius)
            }
            val newMap = map.copy(roomRadius = newRadius, rooms = newRooms)
            dataBase.transaction {
                MapTable.update(where = { MapTable.id eq map.id }) {
                    it[radius] = newRadius
                }
            }
            newMap
        }

        fun Transaction.insertRoom(roomData: RoomData, map: GameMap) {
            MapRoomTable.insert {
                it[mapID] = map.id
                it[spawnLocation] = roomData.spawnLocation
                it[mobsLocation] = roomData.mobsLocation
            }
        }

        addBuilderChild("addspawn", "Добавить точку спавна игрока") { map, ctx ->
            if (map.rooms.size >= maxPlayers) {
                ctx.sender.sendMessage("${RED}У этой карты уже установлены все точки спавна: ${map.rooms.size}")
                return@addBuilderChild null
            }
            val location = ctx.sender.location.centered()
            val roomData = RoomData(location, location, map.roomRadius)
            val newMap = map.copy(rooms = map.rooms + roomData)
            dataBase.transaction {
                insertRoom(roomData, map)
            }
            newMap
        }

        addBuilderChild(
            "setspawn",
            "Изменить существующую точку спавна игрока",
            intParameter("номер"),
        ) { map, ctx ->
            val index = ctx.getArgument<Int>("номер")
            val room = map.rooms.getOrNull(index)
            if (room == null) {
                ctx.sender.sendMessage("${RED}Арены с таким номером не существует")
                return@addBuilderChild null
            }

            val location = ctx.sender.location.centered()
            val newRoom = RoomData(location, room.mobsLocation, map.roomRadius)
            val newRooms = map.rooms.toMutableList().also {
                it[index] = newRoom
            }
            val newMap = map.copy(rooms = newRooms)
            dataBase.transaction {
                MapRoomTable.deleteWhere { MapRoomTable.mapID eq map.id }
                newRooms.forEach {
                    insertRoom(it, map)
                }
            }
            newMap
        }

        addBuilderChild(
            "addmobs",
            "Установить точку спавна мобов",
            uIntParameter("Номер спавна")
        ) { map, ctx ->
            val index = ctx.getArgument<Int>("Номер спавна")
            if (index >= map.rooms.size) {
                ctx.sender.sendMessage("${RED}Такого спавна не существует (максимум: ${map.rooms.lastIndex})")
                return@addBuilderChild null
            }
            val roomData = map.rooms[index]
            val location = ctx.sender.location.centered()
            val rooms = map.rooms.toMutableList()
            val newRoomData = roomData.copy(mobsLocation = location)
            rooms[index] = newRoomData
            val newMap = map.copy(rooms = rooms)

            dataBase.transaction {
                MapRoomTable.update(where = {
                    (MapRoomTable.mapID eq map.id) and (MapRoomTable.spawnLocation eq roomData.spawnLocation)
                }) {
                    it[mobsLocation] = newRoomData.mobsLocation
                }
            }
            newMap
        }

        addBuilderChild(
            "gametype",
            "Режим для карты",
            stringParameter("тип")
        ) { map, ctx ->
            val gameTypeName = ctx.getArgument<String>("тип")
            val gameType = CSCGameType.fromString(gameTypeName)
            val newMap = map.copy(gameType = gameType)
            dataBase.transaction {
                MapTable.update(where = { MapTable.id eq map.id }) {
                    it[MapTable.gameType] = gameType
                }
            }
            newMap
        }
    }

    override fun performDelete(map: GameMap) {
        dataBase.transaction {
            MapTable.deleteIgnoreWhere { MapTable.id eq map.id }
            MapRoomTable.deleteIgnoreWhere { MapRoomTable.mapID eq map.id }
        }
    }
}
