package ru.cristalix.csc.db.table

import org.jetbrains.exposed.sql.Table
import ru.cristalix.csc.shop.item.CSCClass
import ru.cristalix.csc.withTablePrefix

object ClassSelectionTable : Table() {
    private const val CLASSES_TEXT_SIZE = 200

    override val tableName = super.tableName.withTablePrefix()

    val selectedClass = enumeration("selected", CSCClass::class)
    val allClasses = varchar("allClasses", CLASSES_TEXT_SIZE)
}
