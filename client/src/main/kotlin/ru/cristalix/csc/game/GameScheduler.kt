package ru.cristalix.csc.game

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.asBukkitStack
import me.stepbystep.mgapi.client.ClientActor
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import ru.cristalix.core.formatting.Color
import ru.cristalix.core.pvp.CPSLimiter
import ru.cristalix.core.scoreboard.IScoreboardService
import ru.cristalix.csc.CustomSteveChaos
import ru.cristalix.csc.command.StartNewGameCommand
import ru.cristalix.csc.event.CSCGameStartEvent
import ru.cristalix.csc.event.PlayersCreationEvent
import ru.cristalix.csc.game.customitem.CustomItem
import ru.cristalix.csc.game.listener.gameplay.RefillableItemsListener
import ru.cristalix.csc.game.runnable.BossBarRunnable
import ru.cristalix.csc.game.runnable.HidePlayersRunnable
import ru.cristalix.csc.map.DuelMap
import ru.cristalix.csc.map.GameMap
import ru.cristalix.csc.packet.ChangeMoneyPacket
import ru.cristalix.csc.packet.ChangeRatingPacket
import ru.cristalix.csc.packet.IncrementGameCountPacket
import ru.cristalix.csc.packet.RecordPlayerWavePacket
import ru.cristalix.csc.phase.Phase
import ru.cristalix.csc.phase.SpawnMobsPhase
import ru.cristalix.csc.phase.WaitingPhase
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.*
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.seconds

class GameScheduler(
    val actor: ClientActor,
    val plugin: CustomSteveChaos,
    val duelMaps: List<DuelMap>,
    private val map: GameMap,
) {

    val gameTypeOptions = GameTypeOptions.on(actor.gameType)
    val tabHandler = gameTypeOptions.createTabHandler(this)
    var waveIndex = -1; private set
    val displayWaveIndex: Int
        get() = waveIndex + 1

    var currentPhase: Phase = startPhase(WaitingPhase(this))
        private set

    var isGameFinished = false; private set
    val isGameRunning: Boolean
        get() = currentPhase !is WaitingPhase

    private val golemWave = RANDOM.nextInt(19, 30)

    val isGolemWave: Boolean get() = golemWave == waveIndex

    val lastDuels = hashMapOf<CSCTeam, Int>()

    private val teamWaves = linkedMapOf<CSCTeam, Int>()
    private val bossBarRunnable = BossBarRunnable().start(plugin)
    private val hidePlayersRunnable = HidePlayersRunnable(plugin).start()
    private val cpsLimiter = CPSLimiter(plugin, 13)
    private val refillablesListener = RefillableItemsListener(this)

    init {
        val maxTeams = ceil(actor.gameType.maxPlayers.toDouble() / gameTypeOptions.maxPlayersOnTeam)
        require(map.rooms.size >= maxTeams) {
            "Not enough (${map.rooms.size}) RoomData specified for GameType (maxPlayers = ${actor.gameType.maxPlayers})"
        }
        println("Available duelMaps: $duelMaps")
    }

    fun createPlayers() {
        Bukkit.getOnlinePlayers().forEach { player ->
            CSCPlayer(player.uniqueId, this)
        }
        PlayersCreationEvent().callEvent()
    }

    fun start() {
        if (checkMinimumPlayers()) return

        CSCPlayer.getLivingPlayers().forEach { cscPlayer ->
            val player = cscPlayer.asBukkitPlayer()
            player.teleport(cscPlayer.team.room.spawnLocation)
            player.inventory.setItem(17, Material.ARROW.asBukkitStack())

            val packet = IncrementGameCountPacket(IncrementGameCountPacket.ClientData(player.uniqueId))
            actor.messageTransport.sendPacket(packet)
        }

        startPhase(SpawnMobsPhase(this, SpawnMobsPhase.getRandomWave(this), null))
        CSCPlayer.getLivingPlayers().forEach { it.updateScoreboard() }
        clearCustomData()
        CSCGameStartEvent(this).callEvent()

        refillablesListener.register(plugin)
        tabHandler.broadcastScript()
    }

    private fun clearCustomData() {
        CustomItem.VALUES.forEach { it.clearData() }
        Quest.VALUES.forEach { it.clearData() }
    }

    private fun checkMinimumPlayers(): Boolean {
        if (CSCPlayer.getLivingPlayers().size >= 3) return false
        if (actor.gameType.minPlayers <= 3) return false

        Messages.notEnoughPlayers.broadcast()
        Messages.serverRestarting.broadcast(RED)
        Messages.clickableRejoin.broadcast()
        performGameReset()
        return true
    }

    fun startPhase(phase: Phase): Phase {
        @Suppress("RedundantNullableReturnType")
        val oldPhase: Phase? = currentPhase // must be nullable
        oldPhase?.finish()
        currentPhase = phase
        if (phase is SpawnMobsPhase) {
            incrementWaveIndex()
        }
        return phase
    }

    private fun incrementWaveIndex() {
        waveIndex++

        for (player in Bukkit.getOnlinePlayers()) {
            val inventory = player.asNMS().inventory
            for (i in 0 until inventory.size) {
                inventory.getItem(i).updateLoreIfNeed(player, this)
            }
        }
    }

    fun checkGameFinish(): Boolean {
        val aliveTeams = CSCTeam.allAlive()
        if (aliveTeams.size > 1) return false

        completeGame(aliveTeams.firstOrNull())
        return true
    }

    private fun teleportAllToSpawn() {
        for (player in CSCPlayer.getAllPlayers()) {
            teleportAfterWave(player.asBukkitPlayer())
        }
    }

    fun restoreHealth(player: Player) {
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).value
        player.clearPotionEffects()
        player.fireTicks = 0
    }

    fun teleportAfterWave(player: Player, location: Location = actor.spawnLocation) {
        restoreHealth(player)
        player.teleport(location)
    }

    fun saveWave(team: CSCTeam) {
        teamWaves[team] = displayWaveIndex
    }

    fun saveWave(player: Player) {
        val cscPlayer = player.asCSCPlayer()
        if (cscPlayer.hasSavedWave) {
            println("Player $player has already saved wave!")
            Thread.dumpStack()
            return
        }
        if (!cscPlayer.team.isAlive) {
            saveWave(cscPlayer.team)
        }
        cscPlayer.hasSavedWave = true

        val wavePacket = RecordPlayerWavePacket(RecordPlayerWavePacket.ClientData(player.uniqueId, displayWaveIndex))
        actor.messageTransport.sendPacket(wavePacket)

        // update difference for winner (he is alive when this method is called)
        val placeDiff = when {
            CSCPlayer.isAlive(player.uniqueId) -> 1
            else -> 0
        }
        val place = CSCPlayer.getLivingPlayers().size - placeDiff
        cscPlayer.place = place

        player.apply {
            sendMessage("")
            sendMessage(Messages.tookNthPlace, place + 1)
            giveRating(place)
            giveMoney(place, cscPlayer)
            sendMessage("")
            sendMessage(Messages.clickableRejoin)
        }
    }

    private fun Player.giveRating(place: Int) {
        val ratings = intArrayOf(26, 18, 12, 6, 4, 2, 0, 0, -2, -4, -6, -8, -10, -12, -14, -16)
        val rating = ratings[place]

        val packet = ChangeRatingPacket(ChangeRatingPacket.ClientData(uniqueId, rating))
        actor.messageTransport.sendPacket(packet)

        val ratingDiffMessage = when {
            rating > 0 -> "$GREEN+$rating"
            rating == 0 -> "${YELLOW}0"
            else -> "$RED$rating"
        }
        sendMessage(Messages.ratingChanged, ratingDiffMessage)
    }

    private fun Player.giveMoney(place: Int, cscPlayer: CSCPlayer) {
        cscPlayer.earnedCoins += when (place) {
            0 -> 30
            1 -> 20
            2 -> 10
            in 3..5 -> 5
            else -> 0
        }

        val coins = cscPlayer.earnedCoins
        val packet = ChangeMoneyPacket(ChangeMoneyPacket.ClientData(uniqueId, coins))
        actor.messageTransport.sendPacket(packet)

        sendMessage(Messages.receivedCoins, coins)
    }

    fun completeGame(winner: CSCTeam?) {
        if (isGameFinished || !isGameRunning) return

        isGameFinished = true
        winner?.let(::saveWave)
        startPhase(WaitingPhase(this))
        teleportAllToSpawn()
        SpawnMobsPhase.lastWaves.clear()

        Bukkit.getScheduler().cancelTasks(plugin) // cancel all in-game shit
        broadcastTopMessage()
        Messages.serverRestarting.broadcast(GREEN)

        performGameReset()
    }

    private fun performGameReset() {
        cpsLimiter.dispose()
        refillablesListener.unregister()
        hidePlayersRunnable.cancel()

        actor.onGameFinishing()
        plugin.runDelayed(10.seconds) {
            actor.completeGame()
            plugin.runDelayed(3.seconds) {
                HandlerList.unregisterAll(plugin)
                Bukkit.getScheduler().cancelTasks(plugin)
                plugin.onEnable()
            }
        }
    }

    private fun broadcastTopMessage() {
        Messages.topPlayers.broadcast()
        val waves = teamWaves.toList().takeLast(3).asReversed()

        fun broadcastTeam(color: String, index: Int) {
            val teamName = gameTypeOptions.getTeamOrPlayerName(waves[index].first)
            val topMessage = message0(
                russian = "    $color${index + 1}. ${teamName.russian} - $RED${waves[index].second} ${Messages.wave.russian}",
                english = "    $color${index + 1}. ${teamName.english} - $RED${waves[index].second} ${Messages.wave.english}",
            )
            topMessage.broadcast()
        }

        if (waves.isNotEmpty()) {
            broadcastTeam(GOLD, 0)
        }
        if (waves.size >= 2) {
            broadcastTeam(GRAY, 1)
        }
        if (waves.size >= 3) {
            broadcastTeam(AQUA, 2)
        }
    }

    fun updateBossBar(title: PMessage0, timeout: Duration, color: Color) {
        bossBarRunnable.update(title, timeout, color)
    }

    fun getWaveDelay(): Duration = when {
        waveIndex <= 1 -> 20.seconds
        else -> 30.seconds
    } + gameTypeOptions.additionalWaveDelay

    private fun CSCPlayer.updateScoreboard() {
        val player = asBukkitPlayerOrNull() ?: return

        IScoreboardService.get().getPlayerObjective(uuid, "cscBoard").apply {
            displayName = SCOREBOARD_NAME

            record(Messages.gold(player))
            recordUnique { "$GREEN$balance" }
            emptyLine()
            record(Messages.round(player))
            recordUnique { "$RED$displayWaveIndex" }
            emptyLine()
            gameTypeOptions.recordTeamsData(this, player)
        }

        IScoreboardService.get().setCurrentObjective(uuid, "cscBoard")
    }

    private object Messages {
        val notEnoughPlayers = message0(
            russian = "${RED}Игра началась с менее чем 3 игроками",
            english = "${RED}Game started with less than 3 players",
        )

        val serverRestarting = message1<String>(
            russian = { "${it}Сервер будет перезапущен через 10 секунд" },
            english = { "${it}Server will be restarted in 10 seconds" },
        )

        val tookNthPlace = message1<Int>(
            russian = { "${WHITE}Вы заняли $RED$it ${WHITE}место" },
            english = { "${WHITE}You took the $RED$it ${WHITE}place" }
        )

        val ratingChanged = message1<String>(
            russian = { "${WHITE}Рейтинг изменен на $it" },
            english = { "${WHITE}Rating changed by $it" }
        )

        val receivedCoins = message1<Int>(
            russian = { "${WHITE}Вы получили $YELLOW$it ${coinsMessage.russian(it)} ${WHITE}за игру" },
            english = { "${WHITE}You received $YELLOW$it ${coinsMessage.russian(it)} ${WHITE}for game" },
        )

        val topPlayers = message0(
            russian = "${YELLOW}Топ игроков:",
            english = "${YELLOW}Top players:",
        )

        val wave = message0(
            russian = "волна",
            english = "wave",
        )

        val clickableRejoin: PComponentMessage = run {
            fun createComponent(text: String, hint: String): Array<BaseComponent> =
                ComponentBuilder(text)
                    .color(ChatColor.WHITE)
                    .underlined(true)
                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hint)))
                    .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${StartNewGameCommand.NAME}"))
                    .create()

            componentMessage(
                russian = createComponent("Нажмите сюда, чтобы начать новую игру", "Начать новую игру"),
                english = createComponent("Click here to start new game", "Start new game"),
            )
        }

        val gold = message0("${WHITE}Золота", "${WHITE}Gold")
        val round = message0("${WHITE}Раунд", "${WHITE}Round")
    }
}
