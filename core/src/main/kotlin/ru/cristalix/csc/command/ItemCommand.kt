package ru.cristalix.csc.command

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.command.*
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.addItemFlag
import me.stepbystep.api.menu.buildSharedMenu
import me.stepbystep.api.menu.items.empty.EmptyClickableMenuItem
import me.stepbystep.api.menu.unusedItem
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.CSCItemStack
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.ItemTable
import ru.cristalix.csc.transformItemInHand
import ru.cristalix.csc.util.maxShieldDurability
import ru.cristalix.csc.util.shieldDurability
import ru.cristalix.csc.uuid
import java.util.*

class ItemCommand(
    private val database: DataBase,
    private val items: MutableList<CSCItemStack>,
) {

    init {
        val commandBuilder = CommandHelper.literal("item")
            .description("Настроить предметы")
            .requires(RequirementIsOperator())

        commandBuilder.addChild(
            "edit",
            "Добавить/убрать предметы",
            uIntParameter("страница")
        ) {
            val page = it.getArgument<Int>("страница")// ?: 0
            val shownItems = items.drop(page * ITEMS_ON_PAGE).take(ITEMS_ON_PAGE)

            val menu = buildSharedMenu {
                title = "Предметы (страница $page)"
                size = ITEMS_ON_PAGE
                cancelClickDefault = false
                allowAllClicks = true

                shownItems.forEachIndexed { index, item ->
                    index bindTo unusedItem(item.stack, true)
                }

                onClose { player ->
                    saveItems(player, page)
                }
            }

            menu.openFor(it.sender)
        }

        commandBuilder.addChild(
            "addUpgrade",
            "Изменить улучшения предмета",
            uIntParameter("цена")
        ) {
            val player = it.sender

            val cscItem = player.inventory.itemInMainHand?.asNMS()?.findCSCItem() ?: run {
                player.sendMessage("${RED}Этому предмету нельзя установить улучшения")
                return@addChild
            }

            val price = it.getArgument<Int>("цена")

            val menu = buildSharedMenu {
                size = 9
                title = "Улучшение предмета"

                fillGlass(DyeColor.GRAY)

                4 bindTo EmptyClickableMenuItem

                onClose { player ->
                    val inventory = player.openInventory.topInventory
                    val newItem = inventory.asNMS().getItem(4).cloneItemStack()
                    if (newItem.isEmpty) {
                        player.sendMessage("${RED}Вы не добавили улучшение предмету")
                        return@onClose
                    }
                    val item = addUpgrade(player, cscItem, newItem, price)
                    player.inventory.asNMS().addItem(item)
                    player.sendMessage("${GREEN}Вам возвращен предмет, который вы установили в качестве улучшения")
                }
            }

            menu.openFor(player)
        }

        commandBuilder.addChild(
            "rename",
            "Переименовать предмет в руке",
            longStringParameter("имя"),
        ) { ctx ->
            ctx.sender.transformItemInHand {
                it.displayName = ChatColor.translateAlternateColorCodes('&', ctx.getArgument("имя"))
                ctx.sender.sendMessage("${GREEN}Вы успешно переименовали предмет")
            }
        }

        commandBuilder.register()
    }

    private fun saveItems(player: Player, page: Int) {
        val inventory = player.openInventory.topInventory.asNMS()
        val contents = inventory.contents.filterNot { it.isEmpty }

        contents.forEach { stack ->
            stack.prepareForAddition()
            if (stack.uuid == null) {
                stack.uuid = UUID.randomUUID()
            }
        }

        val itemsOnPage = contents.map { stack ->
            stack.findCSCItem() ?: CSCItemStack(null, stack, 0, mutableListOf())
        }

        val newItems = items.take(page * ITEMS_ON_PAGE) +
                itemsOnPage +
                items.drop((page + 1) * ITEMS_ON_PAGE)

        items.clear()
        items.addAll(newItems)

        database.transaction {
            ItemTable.deleteAll()
            items.forEach {
                insertItem(it, player)
            }
        }

        player.sendMessage("${GREEN}Вы успешно обновили предметы")
    }

    private fun addUpgrade(player: Player, item: CSCItemStack, stack: NMSItemStack, price: Int): NMSItemStack {
        val newItem = CSCItemStack(item.uuid, stack, price, mutableListOf())
        stack.prepareForAddition()
        stack.uuid = UUID.randomUUID()
        item.upgrades += newItem
        items += newItem

        database.transaction {
            insertItem(newItem, player)
        }

        player.sendMessage("${GREEN}Вы успешно добавили улучшение предмету")
        return stack.cloneItemStack()
    }

    private fun NMSItemStack.prepareForAddition() {
        val material = item.asBukkit()
        if (material != Material.BOW && material != Material.SHIELD) {
            isUnbreakable = true
        }

        if (material == Material.SHIELD) {
            maxShieldDurability = 3
            shieldDurability = maxShieldDurability
        }

        addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
    }

    private fun NMSItemStack.findCSCItem(): CSCItemStack? {
        val uuid = uuid
        return items.find { it.stack.uuid == uuid }
    }

    private fun Transaction.insertItem(item: CSCItemStack, player: Player) {
        val itemUUID = item.uuid ?: run {
            Bukkit.getLogger().severe("${RED}У предмета $item отсутствует UUID")
            player.sendMessage("${RED}Предмет не был сохранен в базу данных")
            return
        }
        ItemTable.insert {
            it[uuid] = itemUUID
            it[previous] = item.previous
            it[price] = item.price
            it[stack] = item.stack.asBukkit()
        }
    }

    private companion object {
        private const val ITEMS_ON_PAGE = 54
    }
}
