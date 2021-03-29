package ru.cristalix.csc

import me.stepbystep.api.asNMS
import me.stepbystep.api.chat.RED
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.itemInHand
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import ru.cristalix.csc.db.table.PlayerTable
import java.util.*

private const val PREFIX = ""

fun String.withTablePrefix(): String = "$PREFIX$this"

inline fun Player.transformItemInHand(action: (NMSItemStack) -> Unit) {
    val itemInHand = asNMS().itemInHand
    if (itemInHand.isEmpty) {
        sendMessage("${RED}Возьмите предмет в руку")
        return
    }

    action(itemInHand)
}

inline fun <T> modifyColumnForPlayer(playerUUID: UUID, column: Column<T>, getNewValue: (T) -> T) {
    val query = PlayerTable.select { PlayerTable.uuid eq playerUUID }
    val oldValue = query.single()[column]
    val newValue = getNewValue(oldValue)

    PlayerTable.update(where = { PlayerTable.uuid eq playerUUID }) {
        it[column] = newValue
    }
}
