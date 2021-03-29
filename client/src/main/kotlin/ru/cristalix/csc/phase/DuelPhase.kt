package ru.cristalix.csc.phase

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.asNewStack
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.*
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.core.internal.BukkitInternals
import ru.cristalix.csc.command.duel.DuelCommand
import ru.cristalix.csc.command.duel.DuelMenuHolder
import ru.cristalix.csc.event.DuelSuccessfullyCompleteEvent
import ru.cristalix.csc.event.PlayerEarnMoneyEvent
import ru.cristalix.csc.game.DuelStage
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.cage.CagePlayerData
import ru.cristalix.csc.game.cage.CageProcessor
import ru.cristalix.csc.game.listener.gameplay.phase.DuelListener
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.*
import java.util.*
import kotlin.time.Duration
import kotlin.time.seconds

class DuelPhase(gameScheduler: GameScheduler) : Phase(gameScheduler) {
    val first: Participant
    val second: Participant

    private val viewers = arrayListOf<Player>()

    val map = gameScheduler.duelMaps.random()
    private val duelListener = DuelListener(this, gameScheduler)
    private val cageProcessor: CageProcessor

    var stage = DuelStage.BeforeStart
        private set

    val isActive: Boolean get() = stage == DuelStage.Active

    init {
        val players = selectTeams()
        first = Participant(players.first)
        second = Participant(players.second)
        duelListener.register(gameScheduler.plugin)
        scheduleDuelStart()
        listOf(first.team, second.team).forEach {
            gameScheduler.lastDuels[it] = gameScheduler.waveIndex
        }

        cageProcessor = CageProcessor(createCageData(), gameScheduler.plugin)

        forEachParticipant {
            it.isAliveOnDuel = true
        }
    }

    override fun finish() {
        duelListener.unregister()
    }

    fun interruptDuel() {
        if (stage == DuelStage.Finished) return

        Messages.duelInterrupted.broadcast()
        onDuelComplete()
        cageProcessor.interrupt()

        listOf(first, second).flatMap { it.bets.entries }.forEach {
            val player = it.key.asCSCPlayer()
            player.changeGold(it.value)
        }
    }

    fun finishDuel(winnerParticipant: Participant) {
        val winnerTeam = winnerParticipant.team
        val otherParticipant = getOtherParticipant(winnerParticipant)
        gameScheduler.gameTypeOptions.broadcastDuelVictoryMessage(winnerTeam)

        winnerTeam.livingBukkitPlayers.forEach { winner ->
            gameScheduler.plugin.titleHandler.sendTitle(
                player = winner,
                stack = Material.IRON_SWORD.asNewStack(),
                title = Messages.victory(winner),
            )
        }

        otherParticipant.team.livingPlayers.forEach { otherCSCPlayer ->
            otherCSCPlayer.duelLoses++
            val otherPlayer = otherCSCPlayer.asBukkitPlayerOrNull() ?: return@forEach
            gameScheduler.plugin.titleHandler.sendTitle(
                player = otherPlayer,
                stack = Material.MAGMA_CREAM.asNewStack(),
                title = Messages.defeat(otherPlayer),
            )
        }

        val winnerBet = winnerParticipant.bets.values.sum()
        val winnerReward = getWinnerReward(winnerParticipant)
        winnerTeam.livingPlayers.forEach {
            it.changeGold(winnerReward, PlayerEarnMoneyEvent.Cause.DuelVictory)
            it.duelWins++
        }

        val leftReward = listOf(first, second).flatMap { it.bets.values }.sum()

        broadcastMessage("")
        if (winnerParticipant.bets.isEmpty())
            Messages.noWinningBets.broadcast()
        else
            Messages.winningBets.broadcast()

        fun forEachBet(action: (CSCPlayer, Int) -> Unit) {
            winnerParticipant.bets.forEach { (uuid, bet) ->
                val part = bet.toDouble() / winnerBet
                val player = CSCPlayer.getOrNull(uuid)?.takeIf { it.isAlive } ?: return@forEach
                val amount = (leftReward * part + bet * 0.3).toInt()
                action(player, amount)
            }
        }

        forEachBet { cscPlayer, amount ->
            broadcastMessage("    $GOLD${cscPlayer.name} $GRAY- $GREEN$amount")
            if (cscPlayer.isAlive) {
                cscPlayer.asBukkitPlayer().sendTitle(
                    "$GREEN+$amount золота",
                    "",
                    5,
                    40,
                    5
                )
            }
        }
        broadcastMessage("")

        forEachBet { cscPlayer, amount ->
            cscPlayer.changeGold(amount, PlayerEarnMoneyEvent.Cause.DuelBet)
        }

        otherParticipant.bets.forEach { (uuid, amount) ->
            val cscPlayer = CSCPlayer.getOrNull(uuid) ?: return@forEach
            if (cscPlayer.isAlive) {
                cscPlayer.asBukkitPlayer().sendTitle(
                    "$RED-$amount золота",
                    "",
                    5,
                    40,
                    5
                )
            }
        }

        DuelSuccessfullyCompleteEvent(winnerParticipant, this).callEvent()
        onDuelComplete()
        broadcastWinnerInfo(winnerTeam)

        if (CSCTeam.allAlive().size <= gameScheduler.gameTypeOptions.maxTeamsToCurse) {
            val anyCursed = otherParticipant.team.livingPlayers.any { otherPlayer ->
                val addCurse = otherPlayer.livesLeft > 1
                if (addCurse) {
                    otherPlayer.removeLife()
                } else {
                    otherPlayer.addCurse()
                }
                addCurse
            }

            if (anyCursed) {
                Bukkit.getOnlinePlayers().forEach {
                    gameScheduler.plugin.titleHandler.sendTitle(
                        player = it,
                        stack = Material.NETHER_STALK.asNewStack(),
                        title = gameScheduler.gameTypeOptions.getTeamOrPlayerName(otherParticipant.team)(it),
                        subTitle = gameScheduler.gameTypeOptions.teamCurseMessage(it),
                    )
                }
            }
        }

        val phase = gameScheduler.currentPhase as SpawnMobsPhase
        phase.checkCompletion()

        forEachParticipant {
            it.refillables.updateInventory()
        }
    }

    private fun getWinnerReward(winner: Participant): Int {
        val winnerBets = winner.bets.values.sum()
        val oppositeBets = getOtherParticipant(winner).bets.values.sum()
        val oppositeBetsPart = when (val allBets = winnerBets + oppositeBets) {
            0 -> 0.0
            else -> oppositeBets.toDouble() / allBets
        }

        val waveReward = 150 + gameScheduler.displayWaveIndex * 25
        val betsReward = oppositeBets * 0.4
        val hasJackpot = oppositeBetsPart > 0.9
        val multiplier = if (hasJackpot) 1.4 else 1.0
        val reward = ((waveReward + betsReward) * multiplier).toInt()

        if (hasJackpot) {
            gameScheduler.gameTypeOptions.broadcastJackpotMessage(winner.team, reward)
        }

        return reward
    }

    private fun onDuelComplete() {
        stage = DuelStage.Finished
        closeInventories()
        restoreHealth()
        teleportPlayersBack()
    }

    fun removeBets(player: Player) {
        first.bets.remove(player.uniqueId)
        second.bets.remove(player.uniqueId)
    }

    fun getOtherParticipant(participant: Participant): Participant =
        if (participant == first) second else first

    fun findParticipant(player: Player): Participant? {
        val cscPlayer = player.asCSCPlayerOrNull() ?: return null

        return findParticipant(cscPlayer.team)
    }

    fun findParticipant(team: CSCTeam): Participant? = when (team) {
        first.team -> first
        second.team -> second
        else -> null
    }

    fun isParticipant(player: Player): Boolean = findParticipant(player) != null
    fun isParticipant(team: CSCTeam): Boolean = findParticipant(team) != null

    private fun selectTeams(): Pair<CSCTeam, CSCTeam> {
        val allTeams = CSCTeam.allAlive()

        fun CSCTeam.getDuelChance(): Double {
            val lastDuel = gameScheduler.lastDuels[this] ?: return 100.0
            val difference = gameScheduler.waveIndex - lastDuel
            val halfPlayers = allTeams.size / 2
            if (difference >= halfPlayers) return 100.0

            val inversedChance = 80.0 * (halfPlayers - difference + 1) / (halfPlayers * 1.5) + 10
            return (100 - inversedChance).coerceAtLeast(0.0)
        }

        val chances = allTeams.map { ApachePair.create(it, it.getDuelChance()) }
        val distribution = EnumeratedDistribution(chances)

        val first = distribution.sample()
        var second = first
        while (second == first) {
            second = distribution.sample()
        }

        return first to second
    }

    private fun createCageData(): List<CagePlayerData> {
        fun Participant.createCageData(spawnLocation: Location): List<CagePlayerData> =
            team.getSpawnLocations(spawnLocation).map { (player, loc) -> CagePlayerData(player, loc) }

        return first.createCageData(map.firstLocation) + second.createCageData(map.secondLocation)
    }

    private fun teleportPlayersBack() {
        // make sure all players respawn
        gameScheduler.plugin.runDelayed(5.ticks) {
            forEachParticipant {
                gameScheduler.teleportAfterWave(it.asBukkitPlayer())
            }

            viewers.forEach(gameScheduler::teleportAfterWave)
        }
    }

    private fun restoreHealth() {
        forEachParticipant {
            gameScheduler.restoreHealth(it.asBukkitPlayer())
        }
    }

    private fun scheduleDuelStart() {
        broadcastMessage("")
        Messages.duel.broadcast()
        createNicknameMessage().broadcast()
        Messages.clickableDuel.broadcast()
        Messages.duelMap.broadcast(map.name)
        broadcastMessage("")

        gameScheduler.plugin.runDelayed(WINNER_MESSAGE_DURATION, ::broadcastBetsInfo)

        gameScheduler.plugin.runDelayed(gameScheduler.getWaveDelay()) {
            if (stage == DuelStage.Finished) return@runDelayed

            stage = DuelStage.AboutToStart
            closeInventories()
            broadcastBetsInfo()
            startDuel()
        }
    }

    private fun startDuel() {
        if (stage == DuelStage.Finished) return

        stage = DuelStage.Active
        cageProcessor.start()
        Messages.duelBegun.broadcast()

        val duelDuration = DEFAULT_DUEL_DURATION + gameScheduler.gameTypeOptions.additionalDuelTime
        gameScheduler.plugin.runDelayed(duelDuration) {
            val phase = gameScheduler.currentPhase as? SpawnMobsPhase ?: return@runDelayed
            if (phase.duelPhase != this) return@runDelayed
            if (stage == DuelStage.Finished) return@runDelayed

            val winner = listOf(first, second).shuffled()
                .maxByOrNull { it.dealtDamage } ?: error("Duel list is empty")

            finishDuel(winner)
        }
    }

    private fun closeInventories() {
        getOnlinePlayersWithOpenedMenu<DuelMenuHolder>().forEach {
            it.closeInventory()
        }
    }

    private fun createNicknameMessage(): PMessage0 {
        val firstName = gameScheduler.gameTypeOptions.getTeamOrPlayerName(first.team)
        val secondName = gameScheduler.gameTypeOptions.getTeamOrPlayerName(second.team)

        return message0(
            russian = "$RED${firstName.russian} $WHITE${Messages.versus.russian} $RED${secondName.russian}",
            english = "$RED${firstName.english} $WHITE${Messages.versus.english} $RED${secondName.english}",
        )
    }

    fun addViewer(player: Player) {
        gameScheduler.teleportAfterWave(player, map.viewersLocation)
        viewers += player
    }

    fun broadcastBetsInfo() {
        if (lastWinnerInfo + WINNER_MESSAGE_DURATION > timeNow()) return

        fun MutableList<String>.addSeparator(receiver: Player) {
            val text = when (stage) {
                DuelStage.BeforeStart -> Messages.versus(receiver)
                else -> ""
            }
            add(text)
        }

        broadcastDuelInfo(showToAll = false, enable = true) { result, receiver ->
            fun Participant.writeText(result: MutableList<String>) {
                result += gameScheduler.gameTypeOptions.getTeamOrPlayerName(team)(receiver)

                if (stage != DuelStage.BeforeStart) {
                    for ((uuid, bet) in bets) {
                        val name = Bukkit.getPlayer(uuid).name
                        result += "$GREEN$name $WHITE- $bet"
                    }
                }
            }

            first.writeText(result)
            result.addSeparator(receiver)
            second.writeText(result)
        }
    }

    private fun broadcastWinnerInfo(winner: CSCTeam) {
        broadcastDuelInfo(showToAll = true, enable = true) { result, receiver ->
            result += Messages.duelWinner(receiver)
            winner.bukkitPlayers.forEach {
                result += "$RED${it.name.toUpperCase()}"
            }
        }
        gameScheduler.plugin.runDelayed(WINNER_MESSAGE_DURATION) {
            broadcastDuelInfo(showToAll = true, enable = false) { _, _ ->
                // nothing, just hide message
            }
        }
        lastWinnerInfo = timeNow()
    }

    private inline fun broadcastDuelInfo(showToAll: Boolean, enable: Boolean, writeMessage: (MutableList<String>, Player) -> Unit) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (showToAll || !isParticipant(player)) {
                val result = mutableListOf<String>()
                writeMessage(result, player)

                if (Bukkit.getOnlineMode()) {
                    result.forEach(player::sendMessage)
                } else {
                    val buf = Unpooled.buffer()
                    buf.writeBoolean(enable)
                    buf.writeInt(result.size)
                    result.forEach { buf.writeJSString(it) }

                    BukkitInternals.internals().sendPluginMessage(player, "stepbystep:duelinfo", buf)
                }
            }
        }
    }

    private fun ByteBuf.writeJSString(text: String) {
        writeInt(text.length)
        text.forEach { writeInt(it.toInt()) }
    }

    private inline fun forEachParticipant(action: (CSCPlayer) -> Unit) {
        listOf(first, second).flatMap { it.team.livingPlayers }.forEach {
            if (it.isOnline()) {
                action(it)
            }
        }
    }

    data class Participant(val team: CSCTeam) {
        val bets = hashMapOf<UUID, Int>()
        var dealtDamage = 0.0
    }

    private companion object {
        private val DEFAULT_DUEL_DURATION = 50.seconds
        private val WINNER_MESSAGE_DURATION = 4.seconds
        private val DUEL_MESSAGE_MARGIN = " ".repeat(25)

        private var lastWinnerInfo = Duration.ZERO
    }

    private object Messages {
        val duelInterrupted = message0(
            russian = "${RED}Дуэль была прервана",
            english = "${RED}Duel was interrupted",
        )

        val victory = message0(
            russian = "${GREEN}Победа",
            english = "${GREEN}Victory",
        )

        val defeat = message0(
            russian = "${RED}Поражение",
            english = "${RED}Defeat",
        )

        val noWinningBets = message0(
            russian = "${RED}Никто не сделал выигрышную ставку",
            english = "${RED}Nobody made winning bet",
        )

        val winningBets = message0(
            russian = "${GREEN}Ставки выиграли:",
            english = "${GREEN}Winning bets:",
        )

        val duel = message0(
            russian = "$DUEL_MESSAGE_MARGIN${RED}Дуэль!",
            english = "$DUEL_MESSAGE_MARGIN${RED}Duel!"
        )

        val duelMap = message1<String>(
            russian = { "${WHITE}Арена: $YELLOW$it" },
            english = { "${WHITE}Arena: $YELLOW$it" },
        )

        val duelBegun = message0(
            russian = "${GREEN}Дуэль началась",
            english = "${GREEN}Duel has begun",
        )

        val clickableDuel: PComponentMessage = run {
            fun createComponent(text: String, hint: String): Array<BaseComponent> =
                ComponentBuilder(text)
                    .color(ChatColor.YELLOW)
                    .underlined(true)
                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hint)))
                    .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, DuelCommand.NAME_WITH_SLASH))
                    .create()

            componentMessage(
                russian = createComponent("Нажми сюда, чтобы сделать ставку и просмотреть инвентари игроков", "Открыть меню"),
                english = createComponent("Click here to make bet and view player inventories", "Open menu"),
            )
        }

        val versus = message0(
            russian = "${RED}против",
            english = "${RED}vs",
        )

        val duelWinner = message0(
            russian = "${GREEN}Победитель дуэли:",
            english = "${GREEN}Duel winner:",
        )
    }
}
