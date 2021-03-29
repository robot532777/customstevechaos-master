package ru.cristalix.csc.listener

import me.stepbystep.api.chat.*
import me.stepbystep.api.recordUnique
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import ru.cristalix.core.datasync.EntityDataParameters
import ru.cristalix.core.scoreboard.IScoreboardService
import ru.cristalix.core.scoreboard.SimpleBoardObjective
import ru.cristalix.csc.CSCGameType
import ru.cristalix.csc.PlayerAccount
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.PlayerTable
import ru.cristalix.csc.util.SCOREBOARD_NAME

class LobbyListener(
    private val plugin: Plugin,
    private val database: DataBase,
) : Listener {
    init {
        EntityDataParameters.register()
    }

    @EventHandler
    fun PlayerJoinEvent.handle() {
        database.transaction {
            with(PlayerTable) { insertIfAbsent(player.uniqueId) }
            with(PlayerAccount) { load(player.uniqueId, database) }

            player.updateScoreboard()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerQuitEvent.handle() {
        PlayerAccount.remove(player)
    }

    private fun Player.updateScoreboard() {
        val player = this
        val scoreboard = IScoreboardService.get().getPlayerObjective(uniqueId, "cscBoard")
        val account = PlayerAccount[this]

        fun SimpleBoardObjective.recordData(name: PMessage0, value: () -> Any) {
            record(name(player))
            recordUnique { "$GREEN${value()}" }
            emptyLine()
        }

        fun SimpleBoardObjective.recordData(name: PMessage0, value: Any) {
            record(name(player))
            recordUnique("$GREEN$value")
            emptyLine()
        }

        scoreboard.apply {
            displayName = SCOREBOARD_NAME

            recordData(Messages.coins) { account.balance }
            recordData(Messages.totalGames, account.totalGames)
            record(Messages.rating(player))
            recordData(CSCGameType.Solo.displayName.prefixed(WHITE), account.soloRating)
            recordData(CSCGameType.Duo.displayName.prefixed(WHITE), account.duoRating)
        }

        IScoreboardService.get().setCurrentObjective(uniqueId, "cscBoard")
    }

    private object Messages {
        val coins = message0(
            russian = "Монеты",
            english = "Coins",
        ).prefixed(WHITE)

        val totalGames = message0(
            russian = "Всего игр",
            english = "Total games",
        ).prefixed(WHITE)

        val rating = message0(
            russian = "Рейтинг",
            english = "Rating",
        ).prefixed("          $GREEN")
    }
}
