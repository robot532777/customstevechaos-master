package ru.cristalix.csc.lootbox

import me.stepbystep.api.chat.*
import me.stepbystep.api.menu.*
import me.stepbystep.api.runTaskTimer
import me.stepbystep.api.ticks
import me.stepbystep.mgapi.common.ServerSide
import me.stepbystep.mgapi.common.packet.type.BroadcastMessageToServersPacket
import me.stepbystep.mgapi.lobby.LobbyActor
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.util.Pair
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.scheduler.BukkitRunnable
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.PlayerAccount
import ru.cristalix.csc.shop.CSCDonate
import kotlin.time.Duration

class LootBox private constructor(
    private val actor: LobbyActor,
    private val playerAccount: PlayerAccount,
) : BukkitRunnable() {

    companion object {
        const val PRICE = 2000
        private const val ITERATIONS = 20
        private const val RESULT_SLOT = 13

        private val lootBoxDonates = CSCDonate.VALUES
            .filterIsInstance<CSCDonate.LootBoxDonate>()
            .filter { it.canBeLooted }

        private fun canBeOpened(account: PlayerAccount): Boolean {
            val allowedDrops = lootBoxDonates.filterNot { account.hasDonate(it) }
            val player = account.owner

            val errorMessage = when {
                allowedDrops.isEmpty() -> Messages.everythingOpened(player)
                account.balance < PRICE -> Messages.notEnoughMoney(player)
                else -> null
            }

            if (errorMessage != null) {
                player.sendMessage(errorMessage)
            }

            return errorMessage == null
        }

        operator fun invoke(actor: LobbyActor, playerAccount: PlayerAccount): LootBox? {
            if (!canBeOpened(playerAccount)) return null

            playerAccount.removeBalance(PRICE)
            return LootBox(actor, playerAccount)
        }
    }

    private object Messages {
        val everythingOpened = message0(
            russian = "${RED}У вас уже все открыто!",
            english = "${RED}You have already unlocked everything!",
        )

        val notEnoughMoney = message0(
            russian = "${RED}У вас недостаточно монет",
            english = "${RED}You don't have enough coins",
        )

        val roulette = message0(
            russian = "Рулетка",
            english = "Roulette",
        )
    }

    var onComplete: () -> Unit = {}

    private val allowedDrops = lootBoxDonates.filterNot { playerAccount.hasDonate(it) }.toEnumeratedDistribution()
    private val menu = createMenu()
    private var ticksPassed = 0
    private var isCompleted = false

    init {
        menu.openFor(playerAccount.owner)
    }

    fun start() {
        runTaskTimer(actor.plugin, Duration.ZERO, 5.ticks)
    }

    fun interrupt() {
        giveReward(getRandomDrop())
        cancel()
    }

    override fun run() {
        for (slot in 10..17) {
            menu.setMenuItem(slot - 1, menu.getItemAt(slot))
        }
        menu.setMenuItem(17, getRandomDrop().asMenuItem())

        ticksPassed++
        if (ticksPassed >= ITERATIONS) {
            val item = menu.getItemAt(RESULT_SLOT) as DonateMenuItem
            giveReward(item.donate)
            cancel()
        }
    }

    private fun List<CSCDonate.LootBoxDonate>.toEnumeratedDistribution(): EnumeratedDistribution<CSCDonate.LootBoxDonate> {
        check(isNotEmpty()) { "Drops cannot be empty" }
        return EnumeratedDistribution(map { Pair.create(it, it.chancePercent.toDouble()) })
    }

    private fun createMenu(): Menu = buildSharedMenu {
        size = 27
        title = Messages.roulette(playerAccount.owner)
        allowModifyingItems = true

        ((0..3) + (5..8) + (18..21) + (23..26)).toIntArray() bindTo unusedItem(
            material = Material.STAINED_GLASS_PANE,
            displayName = " ",
            data = Color.GREEN.woolData.toByte(),
        )

        intArrayOf(4, 22) bindTo unusedItem(
            material = Material.STAINED_GLASS_PANE,
            displayName = " ",
            data = Color.RED.woolData.toByte(),
        )

        for (slot in 9..17) {
            slot bindTo getRandomDrop().asMenuItem()
        }

        onClose {
            if (!isCompleted) {
                menu.openSafeFor(playerAccount.owner, actor.plugin)
            }
        }
    }

    private fun getRandomDrop() = allowedDrops.sample()

    private fun giveReward(reward: CSCDonate) {
        playerAccount += reward
        val owner = playerAccount.owner
        val rewardName = reward.displayName(owner)
        owner.sendMessage("${GREEN}Вам выпала награда: $YELLOW$rewardName$GRAY!")
        broadcastReward(reward.displayName)
        isCompleted = true
        onComplete()
    }

    private fun broadcastReward(rewardName: PMessage0) {
        val playerName = playerAccount.owner.name
        val message = message0(
            russian = "${YELLOW}Игрок $RED$playerName ${YELLOW}получил награду с лутбокса: $GREEN${rewardName.russian}",
            english = "${YELLOW}Player $RED$playerName ${YELLOW}received drop from lootbox: $GREEN${rewardName.english}",
        )
        val packetData = BroadcastMessageToServersPacket.ClientData(message, ServerSide.Lobby)
        val packet = BroadcastMessageToServersPacket(packetData)

        actor.messageTransport.sendPacket(packet)
    }

    private fun CSCDonate.LootBoxDonate.asMenuItem() = DonateMenuItem(playerAccount.owner, this)

    private class DonateMenuItem(
        player: Player,
        val donate: CSCDonate.LootBoxDonate,
    ) : MenuItem(donate.createDisplayItem(player)) {
        override fun onClick(e: InventoryClickEvent) {
            // nothing
        }
    }
}
