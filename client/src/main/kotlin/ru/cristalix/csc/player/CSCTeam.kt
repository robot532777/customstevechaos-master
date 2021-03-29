package ru.cristalix.csc.player

import com.destroystokyo.paper.profile.PlayerProfile
import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.broadcast
import me.stepbystep.api.chat.message1
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.createNMSItem
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.GameTypeOptions
import ru.cristalix.csc.map.GameMap
import ru.cristalix.csc.map.RoomData

class CSCTeam(
    val color: TeamColor,
    val room: RoomData,
    private val gameScheduler: GameScheduler,
) {
    var players: List<CSCPlayer> = listOf(); private set
    val livingPlayers: List<CSCPlayer> get() = players.filter { it.isAlive }
    val bukkitPlayers: List<Player> get() = players.mapNotNull { it.asBukkitPlayerOrNull() }
    val livingBukkitPlayers: List<Player> get() = livingPlayers.mapNotNull { it.asBukkitPlayerOrNull() }

    val isAlive: Boolean get() = players.any { it.isAlive }
    val livesLeft: Int get() = players.sumBy { it.livesLeft }
    val totalCurseLevel: Int get() = players.sumBy { it.curse }

    lateinit var lastAddedProfile: PlayerProfile

    fun removeMember(player: CSCPlayer) {
        players = players without player

        if (gameScheduler.isGameRunning) {
            val sendMessage = gameScheduler.gameTypeOptions != GameTypeOptions.Solo
            unregisterIfNeed(sendMessage)
        }
    }

    fun unregisterIfNeed(wasActive: Boolean) {
        if (this in teams && !isAlive) {
            teams -= this
            if (wasActive) {
                Messages.teamEliminated.broadcast(this)
                gameScheduler.saveWave(this)
            }
        }
    }

    fun addMember(player: CSCPlayer) {
        players = when {
            players.isEmpty() -> listOf(player)
            else -> players + player
        }
        lastAddedProfile = player.asBukkitPlayer().playerProfile
    }

    fun createDisplayStack(player: Player): NMSItemStack = createNMSItem(
        material = Material.WOOL,
        data = color.woolData,
        displayName = "${color.chatColor}${color.displayName(player)}",
    )

    private infix fun <T> List<T>.without(element: T): List<T> = when (size) {
        1 -> listOf()
        2 -> {
            val index = if (first() == element) 1 else 0
            listOf(get(index))
        }
        else -> this - element
    }

    companion object {
        private val teams = mutableListOf<CSCTeam>()
        private val freeRooms = mutableListOf<RoomData>()

        fun all(): List<CSCTeam> = teams
        fun allAlive(): List<CSCTeam> = teams.filter { it.isAlive }

        fun resetAll(map: GameMap, gameScheduler: GameScheduler) {
            teams.clear()
            val rooms = map.rooms.shuffled()

            TeamColor.values().forEachIndexed { index, color ->
                teams += CSCTeam(color, rooms[index], gameScheduler)
            }

            freeRooms.clear()
            freeRooms += rooms - teams.map { it.room }
        }

        fun createFakeTeam(gameScheduler: GameScheduler): CSCTeam {
            val color = teams.random().color // shouldn't matter
            val room = freeRooms.removeFirst()
            return CSCTeam(color, room, gameScheduler).also { teams += it }
        }
    }

    private object Messages {
        val teamEliminated = message1<CSCTeam>(
            russian = { "${RED}Команда ${it.color.coloredName.russian} ${RED}выбыла из игры" },
            english = { "${RED}Team ${it.color.coloredName.english} ${RED}is eliminated from the game" },
        )
    }
}
