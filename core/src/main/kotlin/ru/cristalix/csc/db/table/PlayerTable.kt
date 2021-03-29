package ru.cristalix.csc.db.table

import me.stepbystep.api.db.stringEnum
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import ru.cristalix.csc.shop.item.CSCCage
import ru.cristalix.csc.shop.item.CSCMessagePack
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import ru.cristalix.csc.withTablePrefix
import java.util.*

object PlayerTable : Table() {
    override val tableName = super.tableName.withTablePrefix()

    val uuid = uuid("uuid").uniqueIndex()
    val balance = integer("balance").default(0)
    val totalGames = integer("totalGames").default(0)
    val soloRating = integer("rating").default(1000)
    val duoRating = integer("duoRating").default(1000)
    val selectedCage = stringEnum("cage", CSCCage::valueOf).default(CSCCage.Default)
    val selectedMessagePack = stringEnum("messagePack", CSCMessagePack::valueOf).default(CSCMessagePack.Default)
    val selectedWeaponSkin = stringEnum("weaponSkin", CSCWeaponSkin::valueOf).default(CSCWeaponSkin.Default)
    val maxWave = integer("maxWave").default(0)

    @Suppress("unused") // must be called in transaction
    fun Transaction.insertIfAbsent(playerUUID: UUID) {
        if (select { uuid eq playerUUID }.empty()) {
            insert {
                it[uuid] = playerUUID
            }
        }
    }
}
