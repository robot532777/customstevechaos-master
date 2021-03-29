package ru.cristalix.csc.command

import me.stepbystep.api.chat.*
import me.stepbystep.api.command.*
import me.stepbystep.api.item.updateLore
import ru.cristalix.core.command.CommandHelper
import ru.cristalix.csc.transformItemInHand
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import ru.cristalix.csc.util.isFrozen

class MakeFrozenItemCommand {
    init {
        CommandHelper.literal("makefrozen")
            .requires(RequirementIsOperator())
            .description("Сделать предмет ледяным")
            .executesWrapped { ctx ->
                ctx.sender.transformItemInHand {
                    it.isFrozen = true
                    CSCWeaponSkin.Default.applySkin(it)
                    it.updateLore { lore ->
                        lore += "${GRAY}Накладывает эффект медлительности на 6 сек."
                    }
                }
            }
            .register()
    }
}
