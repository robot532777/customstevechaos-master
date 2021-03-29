package ru.cristalix.csc.game

import me.stepbystep.api.chat.*
import me.stepbystep.api.recordUnique
import me.stepbystep.mgapi.common.game.GameType
import org.bukkit.entity.Player
import ru.cristalix.core.scoreboard.SimpleBoardObjective
import ru.cristalix.csc.CSCGameType
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import kotlin.time.Duration
import kotlin.time.seconds

sealed class GameTypeOptions(
    val additionalWaveDelay: Duration,
    val maxPlayersOnTeam: Int,
    val maxTeamsToCurse: Int,
    val teamsOnCompassPage: Int,
    val additionalDuelTime: Duration,
) {

    val allowChooseTeam: Boolean get() = this != Solo

    abstract val teamCurseMessage: PMessage0
    abstract val selectDuelTeamMessage: PMessage0
    abstract val betOnTeamMessage: PMessage0

    abstract fun broadcastDuelVictoryMessage(team: CSCTeam)
    abstract fun broadcastJackpotMessage(team: CSCTeam, amount: Int)
    abstract fun broadcastWaveCompleteMessage(team: CSCTeam)
    abstract fun getTeamOrPlayerName(team: CSCTeam): PMessage0
    abstract fun getClassPrefixColor(player: CSCPlayer): String
    abstract fun recordTeamsData(objective: SimpleBoardObjective, player: Player)
    abstract fun createTabHandler(gameScheduler: GameScheduler): TabHandler

    abstract class MultiplePlayersGameTypeOptions(
        additionalWaveDelay: Duration,
        maxPlayersOnTeam: Int
    ) : GameTypeOptions(
        additionalWaveDelay = additionalWaveDelay,
        maxPlayersOnTeam = maxPlayersOnTeam,
        maxTeamsToCurse = 3,
        teamsOnCompassPage = 9,
        additionalDuelTime = 15.seconds,
    ) {

        private object Messages {
            val duelVictory = message1<CSCTeam>(
                russian = { "${GREEN}Команда ${it.color.coloredName.russian} ${GREEN}победила" },
                english = { "${GREEN}Team ${it.color.coloredName.english} ${GREEN}has won" },
            )

            val jackpot = message2<CSCTeam, Int>(
                russian = { team, reward ->
                    val teamName = team.color.coloredName.russian
                    "${GREEN}Команда $teamName ${GREEN}сорвала джекпот и получила $GOLD$reward золота"
                },
                english = { team, reward ->
                    val teamName = team.color.coloredName.russian
                    "${GREEN}Team $teamName ${GREEN}hit the jackpot and received $GOLD$reward gold"
                },
            )

            val waveComplete = message1<CSCTeam>(
                russian = { "${GREEN}Команда ${it.color.coloredName.russian} ${GREEN}завершила волну" },
                english = { "${GREEN}Team ${it.color.coloredName.english} ${GREEN}completed wave" },
            )

            val playersAndTeams = message0(
                russian = "${WHITE}Игроков $GRAY- ${WHITE}Команд",
                english = "${WHITE}Players $GRAY- ${WHITE}Teams",
            )
        }

        override val teamCurseMessage = message0(
            russian = "Прокляты!",
            english = "Are cursed!"
        ).prefixed(RED)

        override val selectDuelTeamMessage = message0(
            russian = "Выбрать команду",
            english = "Select team",
        )

        override val betOnTeamMessage = message0(
            russian = "Ставка на команду",
            english = "Team bet",
        )

        override fun broadcastDuelVictoryMessage(team: CSCTeam) {
            Messages.duelVictory.broadcast(team)
        }

        override fun broadcastJackpotMessage(team: CSCTeam, amount: Int) {
            Messages.jackpot.broadcast(team, amount)
        }

        override fun broadcastWaveCompleteMessage(team: CSCTeam) {
            Messages.waveComplete.broadcast(team)
        }

        override fun getTeamOrPlayerName(team: CSCTeam) = team.color.coloredName

        override fun getClassPrefixColor(player: CSCPlayer) = player.team.color.chatColor

        override fun recordTeamsData(objective: SimpleBoardObjective, player: Player) = with(objective) {
            record(Messages.playersAndTeams(player))
            recordUnique { "$GREEN${CSCPlayer.getLivingPlayers().size} $GRAY- $GREEN${CSCTeam.allAlive().size}" }
        }

        override fun createTabHandler(gameScheduler: GameScheduler) = TabHandler.Team(gameScheduler)
    }

    object Solo : GameTypeOptions(
        additionalWaveDelay = Duration.ZERO,
        maxPlayersOnTeam = 1,
        maxTeamsToCurse = 4,
        teamsOnCompassPage = 18,
        additionalDuelTime = Duration.ZERO,
    ) {
        private object Messages {
            val jackpot = message2<String, Int>(
                russian = { name, reward -> "${GREEN}Игрок $GOLD$name ${GREEN}сорвал куш и получил $GOLD$reward золота" },
                english = { name, reward -> "${GREEN}Player $GOLD$name ${GREEN}hit the jackpot and received $GOLD$reward gold" }
            )

            val leftPlayers = message0(
                russian = "Осталось игроков",
                english = "Left players",
            ).prefixed(WHITE)
        }

        override val teamCurseMessage = message0(
            russian = "Проклят!",
            english = "Is cursed!"
        ).prefixed(RED)

        override val selectDuelTeamMessage = message0(
            russian = "Выбор игрока",
            english = "Select player",
        )

        override val betOnTeamMessage = message0(
            russian = "Ставка на игрока",
            english = "Player bet",
        )

        override fun broadcastDuelVictoryMessage(team: CSCTeam) {
            val winner = team.players.single()
            winner.messagePack.wonDuel.broadcast(winner.name)
        }

        override fun broadcastJackpotMessage(team: CSCTeam, amount: Int) {
            Messages.jackpot.broadcast(team.players.single().name, amount)
        }

        override fun broadcastWaveCompleteMessage(team: CSCTeam) {
            val cscPlayer = team.players.single()
            cscPlayer.messagePack.completedWave.broadcast(cscPlayer.name)
        }

        override fun getTeamOrPlayerName(team: CSCTeam) = "$YELLOW$BOLD${team.lastAddedProfile.name}".asPMessage()

        override fun getClassPrefixColor(player: CSCPlayer): String {
            val clazz = player.selectedClass ?: error("No class selected for $player")
            return clazz.wrapped.color
        }

        override fun recordTeamsData(objective: SimpleBoardObjective, player: Player) = with(objective) {
            record(Messages.leftPlayers(player))
            recordUnique { "$GREEN${CSCPlayer.getLivingPlayers().size}" }
        }

        override fun createTabHandler(gameScheduler: GameScheduler) = TabHandler.Solo(gameScheduler)
    }

    object Duo : MultiplePlayersGameTypeOptions(
        additionalWaveDelay = 9.seconds,
        maxPlayersOnTeam = 2,
    )

    companion object {
        fun on(gameType: GameType): GameTypeOptions = when (gameType) {
            CSCGameType.Solo -> Solo
            CSCGameType.Duo -> Duo
            else -> error("Unknown gameType: $gameType of class ${gameType::class.java}")
        }
    }
}
