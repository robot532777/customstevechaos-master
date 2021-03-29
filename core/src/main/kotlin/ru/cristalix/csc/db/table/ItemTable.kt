package ru.cristalix.csc.db.table

import me.stepbystep.api.db.itemStack
import org.jetbrains.exposed.dao.id.IntIdTable
import ru.cristalix.csc.withTablePrefix

object ItemTable : IntIdTable() {
    override val tableName = super.tableName.withTablePrefix()

    val uuid = uuid("uuid").uniqueIndex()
    val previous = uuid("previous").nullable()
    val price = integer("price").default(0)
    val stack = itemStack("stack")
}
