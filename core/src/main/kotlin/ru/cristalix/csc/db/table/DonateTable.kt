package ru.cristalix.csc.db.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.withTablePrefix
import java.util.*

object DonateTable : Table() {
    override val tableName = super.tableName.withTablePrefix()

    val playerUUID = uuid("uuid")
    val donateKey = varchar("donateKey", 32)

    fun Transaction.hasDonate(playerUUID: UUID, donate: CSCDonate): Boolean =
        select {
            (donateKey eq donate.key) and (DonateTable.playerUUID eq playerUUID)
        }.any()
}
