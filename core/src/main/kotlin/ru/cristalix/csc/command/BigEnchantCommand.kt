package ru.cristalix.csc.command

import me.stepbystep.api.chat.RED
import me.stepbystep.api.command.*
import org.bukkit.enchantments.Enchantment
import ru.cristalix.core.command.CommandHelper

class BigEnchantCommand {
    init {
        val commandBuilder = CommandHelper.literal("bigenchant")
            .requires(RequirementIsOperator())

        commandBuilder.thenWithParameters(
            stringParameter("тип зачарования"),
            intParameter("уровень зачарования")
        ) {
            it.executesWrapped { ctx ->
                val sender = ctx.sender
                val itemInHand = sender.inventory.itemInMainHand
                if (itemInHand.amount == 0) {
                    sender.sendMessage("${RED}Вы не держите предмет в руке")
                }
                val enchant = Enchantment.getByName(ctx.getArgument("тип зачарования"))
                val level = ctx.getArgument<Int>("уровень зачарования")
                itemInHand.addUnsafeEnchantment(enchant, level)
            }
        }

        commandBuilder.register()
    }
}
