package ru.cristalix.csc

import me.stepbystep.api.chat.RED
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.DonateTable
import ru.cristalix.csc.db.table.PlayerTable
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.shop.item.CSCCage
import ru.cristalix.csc.shop.item.CSCItem
import ru.cristalix.csc.shop.item.CSCMessagePack
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import ru.cristalix.csc.util.coinsMessage
import java.util.*
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadWriteProperty

class PlayerAccount(
    val uuid: UUID,
    private val dataBase: DataBase,
    private val boughtDonate: MutableSet<CSCDonate> = hashSetOf(),
    balance: Int,
    selectedCage: CSCCage,
    messagePack: CSCMessagePack,
    weaponSkin: CSCWeaponSkin,
    val maxWave: Int,
    val totalGames: Int,
    val soloRating: Int,
    val duoRating: Int,
) {
    companion object {
        private val players = hashMapOf<UUID, PlayerAccount>()

        fun remove(player: Player) {
            players -= player.uniqueId
        }

        operator fun get(player: Player): PlayerAccount =
            players[player.uniqueId] ?: error("Data for $player was not loaded")

        fun Transaction.load(playerUUID: UUID, dataBase: DataBase) {
            val boughtDonate = CSCDonate.VALUES.filterTo(hashSetOf()) {
                with(DonateTable) { hasDonate(playerUUID, it) }
            }
            val playerData = PlayerTable.select { PlayerTable.uuid eq playerUUID }.single()

            val donate = PlayerAccount(
                uuid = playerUUID,
                dataBase = dataBase,
                boughtDonate = boughtDonate,
                balance = playerData[PlayerTable.balance],
                selectedCage = playerData[PlayerTable.selectedCage],
                messagePack = playerData[PlayerTable.selectedMessagePack],
                weaponSkin = playerData[PlayerTable.selectedWeaponSkin],
                maxWave = playerData[PlayerTable.maxWave],
                totalGames = playerData[PlayerTable.totalGames],
                soloRating = playerData[PlayerTable.soloRating],
                duoRating = playerData[PlayerTable.duoRating],
            )
            players[playerUUID] = donate
        }
    }

    // TODO: dataBaseProperty
    var balance: Int by observable(balance) { _, _, newValue ->
        dataBase.asyncTransaction {
            PlayerTable.update(where = { PlayerTable.uuid eq uuid }) {
                it[PlayerTable.balance] = newValue
            }
        }
    }

    fun removeBalance(amount: Int) {
        check(balance >= amount) { "Not enough balance: $balance to remove $amount for $uuid" }
        balance -= amount
        owner.sendMessage("$RED-$amount ${coinsMessage(owner, amount)}")
    }

    val owner: Player get() = Bukkit.getPlayer(uuid)

    var selectedCage by dataBaseProperty(selectedCage, PlayerTable.selectedCage)
    var messagePack by dataBaseProperty(messagePack, PlayerTable.selectedMessagePack)
    var weaponSkin by dataBaseProperty(weaponSkin, PlayerTable.selectedWeaponSkin)

    operator fun plusAssign(donate: CSCDonate) {
        boughtDonate += donate

        dataBase.asyncTransaction {
            DonateTable.insert {
                it[donateKey] = donate.key
                it[playerUUID] = uuid
            }
        }
    }

    fun isPurchased(item: CSCItem): Boolean {
        val requiredDonate = item.requiredDonate
        return requiredDonate == null || hasDonate(requiredDonate)
    }

    fun hasDonate(donate: CSCDonate): Boolean = donate.isPurchased(boughtDonate)

    private fun <T : Enum<T>> dataBaseProperty(initial: T, column: Column<T>): ReadWriteProperty<Any?, T> =
        observable(initial) { _, _, newValue ->
            dataBase.asyncTransaction {
                PlayerTable.update(where = { PlayerTable.uuid eq uuid }) {
                    it[column] = newValue
                }
            }
        }
}
