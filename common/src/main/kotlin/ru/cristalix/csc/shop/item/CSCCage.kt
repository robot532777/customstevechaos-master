package ru.cristalix.csc.shop.item

import me.stepbystep.api.*
import me.stepbystep.api.chat.*
import org.bukkit.Material
import me.stepbystep.api.chat.PMessage0
import me.stepbystep.api.chat.message0
import ru.cristalix.csc.shop.CSCDonate

enum class CSCCage(
    override val displayName: PMessage0,
    override val material: Material,
    override val data: Byte = 0,
) : CSCItem {
    Default(
        displayName = message0(
            "${GREEN}Стандартная клетка",
            "${GREEN}Default cage",
        ),
        material = Material.GLASS,
    ),
    Prison(
        displayName = message0(
            "${GRAY}Тюремная клетка",
            "${GRAY}Prison cage",
        ),
        material = Material.IRON_FENCE,
    ),
    Watermelon(
        displayName = message0(
            "${RED}Арбузная клетка",
            "${RED}Watermelon cage",
        ),
        material = Material.MELON_BLOCK,
    ),
    Golden(
        displayName = message0(
            "${GOLD}Золотая клетка",
            "${GOLD}Golden cage",
        ),
        material = Material.GOLD_BLOCK,
    ),
    Olympus(
        displayName = message0(
            "${WHITE}Олимпийская клетка",
            "${WHITE}Olympic cage",
        ),
        material = Material.QUARTZ,
    ),
    Woodcutter(
        displayName = message0(
            "${YELLOW}Клетка дровосека",
            "${YELLOW}Woodcutter's cage",
        ),
        material = Material.LOG
    ),
    Dragon(
        displayName = message0(
            "${DARK_RED}Драконья клетка",
            "${DARK_RED}Dragon cage",
        ),
        material = Material.OBSIDIAN,
    ),
    Nether(
        displayName = message0(
            "${RED}Адская клетка",
            "${RED}Nether cage",
        ),
        material = Material.NETHERRACK,
    );

    override val description get() = emptyList<PMessage0>()

    override val requiredDonate by lazy {
        CSCDonate.VALUES.filterIsInstance<CSCDonate.CageDonate>().find { it.cage == this }
    }

    companion object {
        fun byName(name: String): CSCCage = values().first { it.name == name }
    }
}
