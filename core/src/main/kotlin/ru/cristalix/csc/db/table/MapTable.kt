package ru.cristalix.csc.db.table

import org.jetbrains.exposed.dao.id.IntIdTable
import ru.cristalix.csc.CSCGameType
import ru.cristalix.csc.db.column.cscGameType
import ru.cristalix.csc.withTablePrefix

object MapTable : IntIdTable("map") {
    override val tableName = super.tableName.withTablePrefix()

    val name = varchar("name", 36)
    val radius = integer("radius")
    val gameType = cscGameType("gameType").default(CSCGameType.Solo)
}
