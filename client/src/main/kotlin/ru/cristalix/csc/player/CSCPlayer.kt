package ru.cristalix.csc.player

import com.google.gson.JsonObject
import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.mgapi.common.packet.Packet
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import ru.cristalix.core.tab.ITabService
import ru.cristalix.csc.enchant.enchants
import ru.cristalix.csc.enchant.type.ArmorEnchant
import ru.cristalix.csc.event.PlayerEarnMoneyEvent
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.`class`.PlayerClassData
import ru.cristalix.csc.game.`class`.SelectedClass
import ru.cristalix.csc.game.cage.CageType
import ru.cristalix.csc.game.runnable.RebirthParticlesRunnable
import ru.cristalix.csc.packet.*
import ru.cristalix.csc.phase.ScheduleNextWavePhase
import ru.cristalix.csc.phase.WaitingPhase
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.shop.item.CSCMessagePack
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import ru.cristalix.csc.util.BalanceMessageFormatter
import ru.cristalix.csc.util.CommonMessages
import ru.cristalix.csc.util.duelPhase
import ru.cristalix.csc.util.updateLoreIfNeed
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

data class CSCPlayer(
    val uuid: UUID,
    val gameScheduler: GameScheduler,
) {
    companion object {
        private val allPlayers = hashMapOf<UUID, CSCPlayer>()

        fun getOrNull(uuid: UUID): CSCPlayer? = allPlayers[uuid]
        fun get(uuid: UUID): CSCPlayer = getOrNull(uuid) ?: error("CSCPlayer for $uuid not created")
        fun remove(uuid: UUID) {
            allPlayers.remove(uuid)
        }

        fun getAllPlayers(): Collection<CSCPlayer> = allPlayers.values
        fun getLivingPlayers(): Collection<CSCPlayer> = getAllPlayers().filter { it.isAlive }

        fun isAlive(uuid: UUID): Boolean = getOrNull(uuid)?.isAlive == true

        private val defaultGoldFormat: BalanceMessageFormatter = run {
            fun format(balanceWord: String, oldBalance: Int, newBalance: Int): String {
                val color = if (newBalance > oldBalance) GREEN else RED
                val diffSign = if (newBalance > oldBalance) "+" else ""
                return "$GREEN$balanceWord: $GOLD$newBalance $color($diffSign${newBalance - oldBalance})"
            }

            message2(
                russian = { old, new -> format(CommonMessages.balanceWord.russian, old, new) },
                english = { old, new -> format(CommonMessages.balanceWord.english, old, new) },
            )
        }
    }

    private object Messages {
        val curseIncreased = message1<Int>(
            russian = { "${RED}Ваш уровень проклятья увеличен до $GOLD$it$RED." },
            english = { "${RED}Your curse level was increased to $GOLD$it$RED." }
        )

        val curseIncreasedDescription = message1<Int>(
            russian = {
                "${RED}Вы наносите мобам на $GOLD$it% ${RED}меньше урона, и они наносят вам на $GOLD$it% ${RED}больше урона"
            },
            english = {
                "${RED}You deal $GOLD$it% ${RED}less damage to mobs and they do $GOLD$it% ${RED}more damage to you"
            },
        )
    }

    init {
        allPlayers[uuid] = this
        ITabService.get().update(asBukkitPlayer())
    }

    val name: String = asBukkitPlayer().name
    var maxLives = 3
    var livesLeft = maxLives
    var hasSavedWave = false
    var place = -1

    var balance = 0; private set
    var additionalWorth = 0

    var curse = 0; private set

    val isAlive: Boolean get() = livesLeft > 0 && uuid in allPlayers

    var selectedClass: SelectedClass? = null
    var selectedCage: CageType = CageType.Default; private set
    var messagePack: CSCMessagePack = CSCMessagePack.Default; private set
    var weaponSkin: CSCWeaponSkin = CSCWeaponSkin.Default; private set

    var rebirthRunnable: RebirthParticlesRunnable? = null

    var additionalHealthFromBook = 0
    var otherAdditionalHealth = 0
    var additionalDamage: Int by Delegates.observable(0) { _, _, _ ->
        val player = asBukkitPlayerOrNull() ?: return@observable
        val inventory = player.asNMS().inventory
        for (slot in 0 until inventory.size) {
            inventory.getItem(slot).updateLoreIfNeed(player, gameScheduler)
        }
    }
    var additionalRegeneration = 0

    var duelWins = 0
    var duelLoses = 0
    var isAliveOnDuel = false

    var rerollsLeft = 0
    var boughtReroll = false

    var earnedCoins = 0

    val refillables = PlayerRefillables(this)

    private lateinit var _team: CSCTeam
    val team: CSCTeam get() = _team

    private val isOffline: Boolean
        get() = Bukkit.getPlayer(uuid) == null

    init {
        loadDonate()
    }

    fun asBukkitPlayer(): Player = Bukkit.getPlayer(uuid)
    fun isOnline(): Boolean = asBukkitPlayerOrNull() != null
    fun asBukkitPlayerOrNull(): Player? = Bukkit.getPlayer(uuid)

    fun updateTeam(newTeam: CSCTeam) {
        if (hasTeam()) {
            team.removeMember(this)
        }
        _team = newTeam
        newTeam.addMember(this)
        ITabService.get().update(asBukkitPlayer())
    }

    fun hasTeam(): Boolean = ::_team.isInitialized

    fun forceDeath() {
        livesLeft = 1
        removeLife()
    }

    fun addCurse() {
        if (curse >= 6) return

        curse++
        val cursePercent = curse * 15
        val player = asBukkitPlayer()
        player.sendMessage(Messages.curseIncreased, curse)
        player.sendMessage(Messages.curseIncreasedDescription, cursePercent)
    }

    fun changeGold(
        difference: Int,
        cause: PlayerEarnMoneyEvent.Cause? = null,
        messageFormatter: BalanceMessageFormatter = defaultGoldFormat,
    ) {
        if (difference == 0) return
        if (!isAlive) return

        val player = asBukkitPlayerOrNull() ?: return
        val multiplier = when {
            cause != null -> {
                val event = PlayerEarnMoneyEvent(asBukkitPlayer(), cause)
                event.callEvent()
                event.multiplier
            }
            else -> 1.0
        }
        val actualBalance = balance + (difference * multiplier).toInt()

        asBukkitPlayer().sendMessage(messageFormatter(player, balance, actualBalance))
        balance = actualBalance
        gameScheduler.tabHandler.updatePlayer(this)
    }

    private fun getTotalWorth(): Int {
        val duelBets = gameScheduler.duelPhase?.let { duelPhase ->
            listOf(duelPhase.first, duelPhase.second).sumBy { it.bets.getOrDefault(uuid, 0) }
        } ?: 0
        return balance + additionalWorth + duelBets
    }

    fun createTabDescriptor(): JsonObject = JsonObject().apply {
        addProperty("uuid", uuid.toString())
        addProperty("name", name)
        addProperty("worth", getTotalWorth())
        addProperty("livesLeft", livesLeft)
        addProperty("place", place)
    }

    fun addLife() {
        livesLeft++
        maxLives++
    }

    fun removeLife() {
        rebirthRunnable?.cancel()
        livesLeft--
        val player = asBukkitPlayer()
        if (isAlive) {
            messagePack.lostLife.broadcast(player.name, livesLeft, maxLives)
            rebirthRunnable = RebirthParticlesRunnable(this).start()
        } else {
            gameScheduler.saveWave(player)
            messagePack.lostGame.broadcast(player.name)

            gameScheduler.plugin.runDelayed(10) {
                player.gameMode = GameMode.SPECTATOR
            }
        }
        gameScheduler.tabHandler.updatePlayer(this)
    }

    fun isDeathBuffActive(): Boolean = rebirthRunnable != null

    fun updateMaxHealth() {
        if (isOffline) return

        fun NMSItemStack.getAdditionalHealth(): Int {
            val rawData = enchants[ArmorEnchant.MORE_HEALTH] ?: return 0
            return ArmorEnchant.MORE_HEALTH.dataType.parse(rawData) as Int
        }

        val waveIndex = gameScheduler.waveIndex
        val player = asBukkitPlayer()
        val armorHealth = player.asNMS().inventory.armor.sumBy {
            if (it.isEmpty) 0 else it.getAdditionalHealth()
        }
        val oldMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).value
        val maxHealth = 20.0 + (waveIndex + 1) * 2 + armorHealth + additionalHealthFromBook + otherAdditionalHealth
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).baseValue = maxHealth

        val currentPhase = gameScheduler.currentPhase
        if (currentPhase is ScheduleNextWavePhase || currentPhase is WaitingPhase) {
            player.health = maxHealth
        } else if (player.health != 0.0) {
            player.health += maxHealth - oldMaxHealth
        }
    }

    fun loadClassData(): CompletableFuture<PlayerClassData> {
        val future = CompletableFuture<PlayerClassData>()
        val allClassFutures = SelectedClass.VALUES
            .filter { it.requiredDonate != null }
            .associateWith {
                val requiredDonate = it.requiredDonate ?: error("No required donate found for $it")
                val packet = GetDonateStatusPacket(GetDonateStatusPacket.ClientData(uuid, requiredDonate))
                gameScheduler.actor.messageTransport.sendPacket(packet)
            }

        val hasMoreSelectionFuture = gameScheduler.actor.messageTransport.sendPacket(
            GetDonateStatusPacket(GetDonateStatusPacket.ClientData(uuid, CSCDonate.ClassSelection))
        )

        CompletableFuture.allOf(*allClassFutures.values.toTypedArray(), hasMoreSelectionFuture).thenAccept {
            val classes = ArrayList<SelectedClass>()
            allClassFutures
                .filter { it.value.getNow().serverData.hasDonate }
                .forEach {
                    classes += it.key
                }

            val hasMoreSelection = hasMoreSelectionFuture.getNow().serverData.hasDonate
            val classData = PlayerClassData(hasMoreSelection, classes)

            future.complete(classData)
        }

        return future
    }

    private fun loadDonate() {
        fun <T : Packet<*, *>> T.sendAndAccept(callback: (T) -> Unit) {
            gameScheduler.actor.messageTransport.sendPacket(this).thenAccept(callback)
        }

        GetMaxDonateStatusPacket(GetMaxDonateStatusPacket.ClientData(uuid, CSCDonate.RerollsDonate)).sendAndAccept {
            val donate = it.serverData.maxDonate ?: return@sendAndAccept

            boughtReroll = true
            rerollsLeft = when (donate) {
                CSCDonate.Reroll -> 2
                CSCDonate.Reroll3 -> 3
                else -> error("Unknown reroll donate: $donate")
            }
        }

        GetSelectedCagePacket(GetSelectedCagePacket.ClientData(uuid)).sendAndAccept {
            selectedCage = CageType.get(it.serverData.cage)
        }

        GetSelectedMessagePackPacket(GetSelectedMessagePackPacket.ClientData(uuid)).sendAndAccept {
            messagePack = it.serverData.messagePack
        }

        GetSelectedWeaponSkinPacket(GetSelectedWeaponSkinPacket.ClientData(uuid)).sendAndAccept {
            weaponSkin = it.serverData.weaponSkin
        }
    }
}
