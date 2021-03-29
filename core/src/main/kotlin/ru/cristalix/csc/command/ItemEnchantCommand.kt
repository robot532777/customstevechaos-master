package ru.cristalix.csc.command

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import me.stepbystep.api.command.*
import me.stepbystep.api.item.itemInHand
import me.stepbystep.api.item.updateLore
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.enchant.ItemEnchant
import ru.cristalix.csc.enchant.enchants

class ItemEnchantCommand {
    init {
        val commandBuilder = CommandHelper.literal("itemenchant")
            .description("Изменить зачарования у предмета")
            .requires(RequirementIsOperator())

        commandBuilder.addChild(
            "set",
            "Установить предмету зачарование",
            stringParameter("ключ"),
            stringParameter("значение"),
            intParameter("номер строки")
        ) {
            val sender = it.sender
            val stack = sender.asNMS().itemInHand
            val itemType = stack.item.asBukkit()
            val key = it.getArgument<String>("ключ")

            val enchant = ItemEnchant.byKeyOrNull(key) ?: run {
                sender.sendMessage("${RED}Такого зачарования не существует")
                sender.sendMessage("${RED}Все зачарования для этого предмета: ${
                    ItemEnchant.all().filter { enchant -> enchant.canBeAppliedTo(itemType) }
                        .joinToString(transform = ItemEnchant::key)
                }")
                return@addChild
            }

            val dataType = enchant.dataType
            val data = it.getArgument<String>("значение")
            val parsedData = dataType.parseOrNull(data)
            if (parsedData == null) {
                sender.sendMessage("${RED}Это зачарование не поддерживает такое значение")
                return@addChild
            }

            if (!enchant.canBeAppliedTo(itemType)) {
                sender.sendMessage("${RED}На этот предмет нельзя наложить такое зачарование")
                return@addChild
            }

            val newStack = stack.cloneItemStack()
            val loreIndex = it.getArgument<Int>("номер строки")
            val encodedData = dataType.encode(parsedData)

            newStack.enchants += enchant to encodedData
            newStack.updateLore { lore ->
                lore.setOrAdd(loreIndex, enchant.getLoreInfo(encodedData))
            }
            sender.asNMS().itemInHand = newStack
            sender.sendMessage("${GREEN}Вы успешно добавили зачарование предмету")
        }

        commandBuilder.register()
    }
}
