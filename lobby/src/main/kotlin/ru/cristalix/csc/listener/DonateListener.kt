package ru.cristalix.csc.listener

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.item.asNewStack
import me.stepbystep.api.item.updateLore
import me.stepbystep.api.menu.MenuItem
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.menuItem
import me.stepbystep.mgapi.lobby.listener.LobbyCustomItemListener
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.core.CoreApi
import ru.cristalix.core.inventories.InventoryService
import ru.cristalix.core.invoice.BukkitInvoiceService
import ru.cristalix.core.invoice.IInvoiceService
import ru.cristalix.core.invoice.Invoice
import ru.cristalix.csc.CustomSteveChaosLobby
import ru.cristalix.csc.PlayerAccount
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.shop.DonateApplyAction
import ru.cristalix.csc.util.compactMenuSlotsIterator
import kotlin.math.roundToInt

class DonateListener(
    private val plugin: CustomSteveChaosLobby,
) {
    init {
        registerServiceIfAbsent { InventoryService(CoreApi.get().platformServer) }
        registerServiceIfAbsent { BukkitInvoiceService(CoreApi.get().socketClient) }

        LobbyCustomItemListener(
            stack = Material.EMERALD.asNewStack(),
            itemTag = "donateItem",
            itemSlot = 2,
            getDisplayName = message0(
                russian = "${GREEN}Донат",
                english = "${GREEN}Donate",
            )
        ) {
            openDonateMenu(it)
        }.register(plugin)
    }

    private fun openDonateMenu(player: Player) {
        val account = PlayerAccount[player]

        buildSharedMenu {
            size = 54
            title = "Донат"

            fun donateItem(type: CSCDonate): MenuItem {
                val stack = type.createDisplayItem(player)
                val sale = plugin.salePercent
                val actualPrice = (type.price * (100 - sale) / 100.0).roundToInt()
                val priceWithWord = Messages.cristalixWithWord(player, actualPrice)

                val status = when {
                    account.hasDonate(type) -> Messages.alreadyBought(player)
                    sale == 0 -> Messages.priceWithoutSale(player, priceWithWord)
                    else -> Messages.priceWithSale(player, type.price, priceWithWord)
                }

                stack.updateLore {
                    it += ""
                    it += status
                }

                return menuItem(stack) {
                    if (account.hasDonate(type)) return@menuItem

                    val invoice = Invoice.builder()
                        .price(actualPrice)
                        .description(type.displayName(player))
                        .build()

                    IInvoiceService.get().bill(player.uniqueId, invoice).thenAccept {
                        if (it.isSuccess) {
                            account.addDonate(type)
                            player.sendMessage(Messages.thanksForSupport)
                        } else {
                            player.sendMessage("$RED${it.error}")
                        }
                    }
                }
            }

            fillGlass(DyeColor.GRAY)

            val slots = compactMenuSlotsIterator()

            CSCDonate.VALUES.filter { it.isInDonateShop }.forEach {
                slots.next() bindTo donateItem(it.getActualDonate(account))
            }
        }.openFor(player)
    }

    private fun PlayerAccount.addDonate(donate: CSCDonate) {
        when (val applyAction = donate.applyAction) {
            DonateApplyAction.AddPermanent -> this += donate
            DonateApplyAction.AddNewYearPack -> {
                this += CSCDonate.NewYearMessagePack
                this += CSCDonate.NewYearPackDonate
                this += CSCDonate.NewYear
            }
            is DonateApplyAction.AddMoney -> balance += applyAction.amount
        }
    }

    private fun CSCDonate.getActualDonate(account: PlayerAccount): CSCDonate {
        if (this !is CSCDonate.UpgradingDonate) return this

        return donates.firstOrNull { !account.hasDonate(it) } ?: donates.last()
    }

    private object Messages {
        val cristalixWithWord = message1<Int>(
            russian = { coins -> "$coins ${coins.wordForNum("кристалик", "кристалика", "кристаликов")}" },
            english = { coins -> "$coins ${if (coins == 1) "crystal" else "crystals"}" }
        )

        val alreadyBought = message0(
            russian = "${GREEN}Куплено",
            english = "${GREEN}Already bought",
        )

        val priceWithoutSale = message1<String>(
            russian = { "${WHITE}Стоимость: $GREEN$it" },
            english = { "${WHITE}Price: $GREEN$it" },
        )

        val priceWithSale = message2<Int, String>(
            russian = { oldPrice, newPrice -> "${WHITE}Стоимость: $GREEN$STRIKETHROUGH$oldPrice$RED$BOLD $newPrice" },
            english = { oldPrice, newPrice -> "${WHITE}Price: $GREEN$STRIKETHROUGH$oldPrice$RED$BOLD $newPrice" },
        )

        val thanksForSupport = message0(
            russian = "${GREEN}Спасибо за поддержку режима!",
            english = "${GREEN}Thanks for supporting the project!"
        )
    }
}
