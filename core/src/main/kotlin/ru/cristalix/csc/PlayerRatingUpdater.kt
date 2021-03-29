package ru.cristalix.csc

import com.google.common.collect.HashBasedTable
import me.stepbystep.api.runTaskTimerAsynchronously
import me.stepbystep.mgapi.common.game.GameType
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.PlayerTable
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration
import kotlin.time.minutes

class PlayerRatingUpdater(
    private val dataBase: DataBase,
) : BukkitRunnable() {
    private val players = HashBasedTable.create<GameType, UUID, Int>()
    private val inUpdateLock = ReentrantLock()

    fun start(plugin: CustomSteveChaosCore) = apply {
        runTaskTimerAsynchronously(plugin, Duration.ZERO, 5.minutes)
    }

    override fun run() {
        inUpdateLock.lock()
        try {
            players.clear()

            dataBase.transaction {
                fun addPlaces(gameType: CSCGameType, column: Column<Int>) {
                    var i = 0
                    PlayerTable.selectAll().orderBy(column, SortOrder.DESC).forEach {
                        players.put(gameType, it[PlayerTable.uuid], i++)
                    }
                }
                addPlaces(CSCGameType.Solo, PlayerTable.soloRating)
                addPlaces(CSCGameType.Duo, PlayerTable.duoRating)
            }
        } finally {
            inUpdateLock.unlock()
        }
    }

    fun getPlayerPlace(gameType: GameType, playerUUID: UUID): Int {
        return try {
            inUpdateLock.lock()
            (players[gameType, playerUUID] ?: players.size()) + 1
        } finally {
            inUpdateLock.unlock()
        }
    }
}
