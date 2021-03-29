package ru.cristalix.csc.phase

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.ItemTag
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import me.stepbystep.api.menu.openSafeForceFor
import me.stepbystep.mgapi.client.ClientActor
import net.minecraft.server.v1_12_R1.NBTTagInt
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.csc.event.PlayerSelectRandomItemEvent
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.packet.ItemDataPacket
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.*

class LobbyPhase(gameScheduler: GameScheduler) : Phase(gameScheduler) {

    companion object {
        private const val ITEMS_COUNT = 8
        private const val REROLL_ITEM_SLOT = 13
        private val ITEMS_SLOTS = intArrayOf(19, 21, 23, 25, 28, 30, 32, 34)

        private var defaultItems = listOf<NMSItemStack>()

        fun initItems(actor: ClientActor) {
            actor.messageTransport.sendPacket(ItemDataPacket()).thenAccept { dataPacket ->
                defaultItems = dataPacket.serverData.allItems.filter { it.previous == null }.map { it.stack }
            }
        }
    }

    init {
        setup()
    }

    private fun setup() {
        // next wave is 250
        if (gameScheduler.displayWaveIndex == 249) {
            val players = CSCTeam.allAlive()
            val winner = players.minByOrNull { it.totalCurseLevel } ?: error("No living players left")
            gameScheduler.completeGame(winner)
            return
        }

        giveWaveMoney()

        val wave = gameScheduler.waveIndex
        if (wave in 0..1 || wave % 2 != 0) {
            giveItems()
        }

        val phase = ScheduleNextWavePhase(gameScheduler, shouldStartDuel())

        gameScheduler.plugin.runTask {
            gameScheduler.startPhase(phase)
        }
    }

    private fun giveWaveMoney() {
        val money = when (gameScheduler.displayWaveIndex) {
            in 1..3 -> 0
            in 4..8 -> 2
            in 9..16 -> 4
            in 17..24 -> 6
            else -> 8
        }

        CSCPlayer.getLivingPlayers().forEach {
            it.earnedCoins += money
        }
    }

    private fun shouldStartDuel(): Boolean {
        val leftTeams = CSCTeam.allAlive().size
        val wave = gameScheduler.waveIndex

        return when {
            gameScheduler.isGolemWave -> false
            wave <= 1 -> false
            leftTeams == 2 -> wave % 2 == 0
            leftTeams in 3..4 -> wave % 3 != 0
            else -> true
        }
    }

    private fun giveItems() {
        for (player in CSCPlayer.getLivingPlayers()) {
            openItemsMenu(player.asBukkitPlayer(), getRandomItems())
        }
    }

    private fun getRandomItems(): Collection<NMSItemStack> {
        val givenItems = defaultItems.toTypedArray()
        if (givenItems.size <= ITEMS_COUNT) return givenItems.toSet().shuffled()

        val result = arrayListOf<NMSItemStack>()
        val size = givenItems.size
        val random = RANDOM

        while (result.size < ITEMS_COUNT) {
            givenItems.random(random)
            val item = givenItems[random.nextInt(size)]
            if (result.none { it.isSimilarWithoutDisplayAndUUID(item) }) {
                result += item
            }
        }

        return result
    }

    private fun openItemsMenu(player: Player, items: Collection<NMSItemStack>) {
        var closeReason = ItemsInventoryCloseReason.PressEscape
        val cscPlayer = player.asCSCPlayer()
        if (cscPlayer.selectedClass == null) return // class and items menu will constantly reopen themselves

        buildSharedMenu {
            size = 54
            title = Messages.items(player)

            fillGlass(DyeColor.GRAY)

            items.forEachIndexed { index, stack ->
                val shownStack = stack.cloneItemStack().updateLoreIfNeed(player, gameScheduler)
                ITEMS_SLOTS[index] bindTo menuItem(shownStack) {
                    val stackWithSkin = cscPlayer.weaponSkin
                        .getItemWithSkin(shownStack)
                        .updateLoreIfNeed(player, gameScheduler)

                    val whoClicked = whoClicked as Player
                    if (whoClicked.inventory.asNMS().addItem(stackWithSkin)) {
                        if (stackWithSkin.item.asBukkit().isRefillable()) {
                            cscPlayer.refillables.add(stackWithSkin.cloneItemStack())
                        }
                        closeReason = ItemsInventoryCloseReason.TookItem
                        whoClicked.closeInventory()
                    } else {
                        whoClicked.sendMessage(CommonMessages.notEnoughInventorySpace)
                    }
                }
            }

            REROLL_ITEM_SLOT bindTo menuItem(cscPlayer.createRerollItem()) {
                if (cscPlayer.rerollsLeft > 0) {
                    closeReason = ItemsInventoryCloseReason.UsedReroll
                    cscPlayer.rerollsLeft--
                    openItemsMenu(player, getRandomItems())
                }
            }

            onClose {
                val forbidClose = when (val phase = gameScheduler.currentPhase) {
                    is LobbyPhase, is ScheduleNextWavePhase -> closeReason == ItemsInventoryCloseReason.PressEscape
                    is DuelPhase -> !phase.stage.isStarted
                    else -> false
                }

                val message = when {
                    forbidClose -> {
                        openItemsMenu(it, items)
                        Messages.selectItem(player)
                    }
                    closeReason == ItemsInventoryCloseReason.TookItem -> {
                        PlayerSelectRandomItemEvent(player).callEvent()
                        Messages.successfullySelectedItem(player)
                    }
                    closeReason == ItemsInventoryCloseReason.UsedReroll -> ""
                    else -> Messages.didNotSelectItem(player)
                }

                if (message.isNotEmpty()) {
                    it.sendMessage(message)
                }
            }
        }.openSafeForceFor(player, gameScheduler.plugin)
    }

    private fun CSCPlayer.createRerollItem(): NMSItemStack {
        val rerollsColor = if (rerollsLeft > 0) GREEN else RED
        val player = asBukkitPlayer()

        return createNMSItem(
            material = Material.CLAY_BALL,
            displayName = Messages.shuffleItems(player),
            lore = listOfNotNull(
                Messages.rerollsLeft(player, rerollsColor, rerollsLeft),
                if (boughtReroll) null else Messages.buyReroll(player)
            ),
            customTag = ItemTag("thief", NBTTagInt(9))
        )
    }

    private enum class ItemsInventoryCloseReason {
        PressEscape,
        TookItem,
        UsedReroll;
    }

    private object Messages {
        val items = message0(
            russian = "Предметы",
            english = "Items",
        )

        val selectItem = message0(
            russian = "${RED}Выберите предмет",
            english = "${RED}Select item",
        )

        val successfullySelectedItem = message0(
            russian = "${GREEN}Вы успешно выбрали предмет",
            english = "${GREEN}You successfully selected an item",
        )

        val didNotSelectItem = message0(
            russian = "${RED}Вы не выбрали предмет",
            english = "${RED}You didn't select an item",
        )

        val shuffleItems = message0(
            russian = "${GREEN}Перемешать предметы",
            english = "${GREEN}Shuffle items",
        )

        val rerollsLeft = message2<String, Int>(
            russian = { color, count -> "${GRAY}Использований осталось: $color$count" },
            english = { color, count -> "${GRAY}Rerolls left: $color$count" },
        )

        val buyReroll = message0(
            russian = "${RED}Нужно купить реролл в лобби",
            english = "${RED}You need to buy reroll in the lobby",
        )
    }
}
