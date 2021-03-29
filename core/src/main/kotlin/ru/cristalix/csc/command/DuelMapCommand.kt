package ru.cristalix.csc.command

import me.stepbystep.api.asNMS
import me.stepbystep.api.centered
import me.stepbystep.api.chat.GRAY
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.RED
import me.stepbystep.api.command.*
import me.stepbystep.api.materialData
import org.bukkit.Location
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import ru.cristalix.csc.CSCGameType
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.DuelMapTable
import ru.cristalix.csc.map.DuelMap

class DuelMapCommand(
    private val database: DataBase,
    maps: MutableList<DuelMap>,
) : AbstractMapCommand<DuelMap>("duelmap", "Настроить карты дуэлей", maps) {

    override fun UUIDLiteralArgumentBuilder.configure() {
        addChild("create", "Создать карту") { ctx ->
            val defaultLocation = ctx.sender.location.centered()

            database.transaction {
                val id = DuelMapTable.insertAndGetId {
                    it[name] = ""
                    it[firstLocation] = defaultLocation
                    it[secondLocation] = defaultLocation
                    it[viewersLocation] = defaultLocation
                }.value

                val duelMap = DuelMap(
                    id = id,
                    name = "",
                    gameType = null,
                    firstLocation = defaultLocation,
                    secondLocation = defaultLocation,
                    viewersLocation = defaultLocation,
                    displayMaterialData = DuelMapTable.defaultMaterialData,
                )
                maps.add(duelMap)
                selectedMaps[ctx.source] = duelMap
                ctx.sender.sendMessage("${GREEN}Вы успешно создали карту с ID = $id")
            }
        }

        addChild("list", "Посмотреть все карты дуэлей") {
            val sender = it.sender
            if (maps.isNotEmpty()) {
                sender.sendMessage("${GREEN}Все карты дуэлей:")
                for (map in maps) {
                    sender.sendMessage("    $GREEN${map.id} $GRAY- ${map.chatName}")
                }
            } else {
                sender.sendMessage("${RED}Еще не создано ни 1 карты")
            }
        }

        addSelectedMapChild("tpFirst", "Телепортироваться на первую точку") { map, ctx ->
            ctx.sender.teleport(map.firstLocation)
            ctx.sender.sendMessage("${GREEN}Вы телепортировались на первую точку")
        }

        addSelectedMapChild("tpSecond", "Телепортироваться на вторую точку") { map, ctx ->
            ctx.sender.teleport(map.secondLocation)
            ctx.sender.sendMessage("${GREEN}Вы телепортировались на вторую точку")
        }

        addSelectedMapChild("tpView", "Телепортироваться на точку для зрителей") { map, ctx ->
            ctx.sender.teleport(map.viewersLocation)
            ctx.sender.sendMessage("${GREEN}Вы телепортировались на точку для зрителей")
        }

        addBuilderChild(
            "name",
            "Установить имя карты",
            longStringParameter("Имя")
        ) { map, ctx ->
            val newName = ctx.getArgument<String>("Имя")
            val newMap = map.copy(name = newName)
            database.transaction {
                DuelMapTable.update(where = { DuelMapTable.id eq map.id }) {
                    it[name] = newName
                }
            }
            newMap
        }

        addBuilderChild("display", "Установить отображаемый предмет") { map, ctx ->
            val sender = ctx.sender
            val item = sender.inventory.itemInMainHand
            if (item.asNMS().isEmpty) {
                sender.sendMessage("${RED}Возьмите предмет в руку")
                return@addBuilderChild null
            }

            val newMap = map.copy(displayMaterialData = item.materialData)
            database.transaction {
                DuelMapTable.update(where = { DuelMapTable.id eq map.id }) {
                    it[displayMaterial] = item.type
                    it[displayData] = item.durability.toByte()
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
            database.transaction {
                DuelMapTable.update(where = { DuelMapTable.id eq map.id }) {
                    it[DuelMapTable.gameType] = gameType
                }
            }
            newMap
        }

        addChangeLocationChild(
            "firstLocation",
            "Установить спавн первого игрока",
            DuelMapTable.firstLocation
        ) { copy(firstLocation = it) }

        addChangeLocationChild(
            "secondLocation",
            "Установить спавн второго игрока",
            DuelMapTable.secondLocation
        ) { copy(secondLocation = it) }

        addChangeLocationChild(
            "viewersLocation",
            "Установить спавн зрителей",
            DuelMapTable.viewersLocation
        ) { copy(viewersLocation = it) }
    }

    private inline fun UUIDLiteralArgumentBuilder.addChangeLocationChild(
        name: String,
        description: String,
        tableColumn: Column<Location>,
        crossinline copy: DuelMap.(Location) -> DuelMap,
    ) {
        addBuilderChild(name, description) { map, ctx ->
            val location = ctx.sender.location.centered()
            val newMap = copy(map, location)
            database.transaction {
                DuelMapTable.update(where = { DuelMapTable.id eq map.id }) {
                    it[tableColumn] = location
                }
            }
            newMap
        }
    }

    override fun performDelete(map: DuelMap) {
        database.transaction {
            DuelMapTable.deleteIgnoreWhere { DuelMapTable.id eq map.id }
        }
    }
}
