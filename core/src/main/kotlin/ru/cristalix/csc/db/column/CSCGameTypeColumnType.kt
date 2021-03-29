package ru.cristalix.csc.db.column

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import ru.cristalix.csc.CSCGameType

class CSCGameTypeColumnType : VarCharColumnType(10) {
    override fun valueFromDB(value: Any): CSCGameType = when (value) {
        is CSCGameType -> value
        is String -> CSCGameType.fromString(value)
        is ByteArray -> valueFromDB(String(value))
        else -> error("Unexpected type for CSCGameTypeColumnType: $value")
    }

    override fun notNullValueToDB(value: Any): String {
        if (value !is CSCGameType) error("Unexpected type for CSCGameTypeColumnType: $value")

        return value::class.java.simpleName
    }

    override fun nonNullValueToString(value: Any): String = buildString {
        append('\'')
        append(notNullValueToDB(value))
        append('\'')
    }
}

fun Table.cscGameType(name: String): Column<CSCGameType> = registerColumn(name, CSCGameTypeColumnType())
