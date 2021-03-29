package ru.cristalix.csc.game.listener.gameplay

import me.stepbystep.api.addItem
import me.stepbystep.api.asNMS
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.*
import me.stepbystep.api.menu.*
import me.stepbystep.api.runTask
import net.minecraft.server.v1_12_R1.EntityItem
import net.minecraft.server.v1_12_R1.NBTTagString
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import ru.cristalix.core.formatting.Color
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.util.*
import java.util.*

class TransferItemListener(private val gameScheduler: GameScheduler) : Listener {
    companion object {
        fun markTransferable(stack: NMSItemStack) {
            stack.isTransferable = true
            stack.updateLore { lore ->
                lore += Messages.transferItemLore.map { it.russian }
            }
        }
    }

    // TODO: check if this works
    @EventHandler
    fun EntityPickupItemEvent.handle() {
        val player = entity as? Player ?: return
        val entityItem = item.asNMS() as EntityItem
        val stack = entityItem.itemStack
        if (!stack.isTransferable) return

        stack.translateLore(player)
    }

    @EventHandler
    fun PlayerInteractEntityEvent.handle() {
        if (hand != EquipmentSlot.HAND) return
        if (!player.isSneaking) return
        if (player.hasOpenedMenu<TransferMenuHolder>()) return

        val itemInHand = player.asNMS().itemInHand
        if (!itemInHand.isTransferable) return

        val cscPlayer = player.asCSCPlayer()
        val otherPlayer = rightClicked as? Player ?: return
        if (otherPlayer.asCSCPlayer().team != cscPlayer.team) return

        val transactionUUID = UUID.randomUUID().toString()
        var isTransactionActive = true

        itemInHand.updateTransferTransactions {
            it.add(NBTTagString(transactionUUID))
        }

        buildSharedMenu {
            size = 9
            title = Messages.itemTransfer(player)
            createMenuHolder = { TransferMenuHolder(otherPlayer.uniqueId, it) }

            onClose { player ->
                if (!isTransactionActive) return@onClose
                isTransactionActive = false

                var hasTransferredItem = false
                player.forEachTransferredSlot {
                    getItemOrCursor(it).removeTransaction(transactionUUID)
                    hasTransferredItem = true
                }
                if (hasTransferredItem) {
                    player.sendMessage(Messages.transferCancelled)
                }
            }

            val addedStack = itemInHand.cloneItemStack()
            2 bindTo menuItem(
                material = Material.STAINED_GLASS_PANE,
                displayName = Messages.confirm(player),
                data = Color.GREEN.woolData.toByte(),
                clickAction = {
                    gameScheduler.plugin.runTask {
                        if (!isTransactionActive) return@runTask
                        isTransactionActive = false

                        onTransferAttempt(player, otherPlayer, addedStack, transactionUUID)
                    }
                },
            )

            4 bindTo unusedItem(itemInHand.cloneItemStack())

            6 bindTo menuItem(
                material = Material.STAINED_GLASS_PANE,
                displayName = Messages.cancel(player),
                data = Color.RED.woolData.toByte(),
            ) {
                player.closeInventory()
            }
        }.openFor(player)
    }

    private fun NMSItemStack.addTransaction(uuid: UUID) {
        accessTag {
            setBoolean(uuid.toString(), true)
        }
    }

    private fun NMSItemStack.hasTransaction(uuid: UUID): Boolean =
        tag?.getBoolean(uuid.toString()) == true

    private fun NMSItemStack.removeTransaction(transactionUUID: String) {
        updateTransferTransactions { transactions ->
            transactions.list.removeIf { element ->
                element is NBTTagString && element.data == transactionUUID
            }
        }
    }

    private fun onTransferAttempt(player: Player, target: Player, addedStack: NMSItemStack, transactionUUID: String) {
        addedStack.translateLore(target)
        addedStack.removeTransaction(transactionUUID)
        if (target.asNMS().inventory.addItem(addedStack)) {
            player.forEachTransferredSlot {
                setItemOrCursor(it, NMSItemStack.a)
            }
            player.closeInventory()
            player.sendMessage(Messages.successfulTransferFrom)
            target.sendMessage(Messages.successfulTransferTo, player.name)
        } else {
            player.sendMessage(Messages.unsuccessfulTransferFrom)
            target.sendMessage(Messages.unsuccessfulTransferTo, player.name)
        }
    }

    private fun NMSItemStack.translateLore(receiver: Player) {
        updateLore { lore ->
            val translatedLore = Messages.transferItemLore(receiver)
            val startIndex = lore.size - translatedLore.size
            translatedLore.forEachIndexed { index, line ->
                lore[startIndex + index] = line
            }
        }
    }

    private inline fun Player.forEachTransferredSlot(action: NMSPlayerInventory.(Int) -> Unit) {
        val inventory = inventory.asNMS()
        for (i in CURSOR_SLOT until inventory.size) {
            if (inventory.getItemOrCursor(i).isCurrentlyTransferred) {
                inventory.action(i)
            }
        }
        updateInventory()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerQuitEvent.handle() {
        getOnlinePlayersWithOpenedMenu<TransferMenuHolder> {
            it.targetUUID == player.uniqueId
        }.forEach {
            it.sendMessage(Messages.quitGame)
            it.closeInventory()
        }
    }

    private class TransferMenuHolder(
        val targetUUID: UUID,
        menu: Menu,
    ) : MenuHolder(menu)

    private object Messages {
        val quitGame = message0(
            russian = "Игрок, которому вы передаете предмет, вышел из сети",
            english = "Player you are giving the item to has quit the game",
        ).prefixed(RED)

        val successfulTransferFrom = message0(
            russian = "Передача предмета прошла успешно",
            english = "You successfully transferred item",
        ).prefixed(GREEN)

        val successfulTransferTo = message1<String>(
            russian = { "${GREEN}Игрок $GOLD$it ${GREEN}передал вам предмет" },
            english = { "${GREEN}Player $GOLD$it ${GREEN}gave you an item" },
        )

        val unsuccessfulTransferFrom = message0(
            russian = "У игрока заполнен инвентарь",
            english = "Player has full inventory",
        ).prefixed(RED)

        val unsuccessfulTransferTo = message1<String>(
            russian = { "${RED}Игрок $GOLD$it ${RED}пытается передать вам предмет в заполненный инвентарь" },
            english = { "${RED}Player $GOLD$it ${RED}is trying to transfer an item into your full inventory" },
        )

        val confirm = message0(
            russian = "Подтвердить",
            english = "Confirm",
        ).prefixed(GREEN)

        val cancel = message0(
            russian = "Отменить",
            english = "Cancel",
        ).prefixed(RED)

        val transferCancelled = message0(
            russian = "Передача предмета отменена",
            english = "Item transfer has been cancelled",
        ).prefixed(RED)

        val itemTransfer = message0(
            russian = "Передача предмета",
            english = "Item transfer",
        )

        val transferItemLore = listOf(
            message0(
                russian = "Чтобы передать союзнику,",
                english = "Use shift + right-click on teammate"
            ).prefixed(DARK_GRAY),
            message0(
                russian = "нажмите шифт + ПКМ на него",
                english = "to transfer this item to him",
            ).prefixed(DARK_GRAY),
        )
    }
}
