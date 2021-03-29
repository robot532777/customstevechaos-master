package ru.cristalix.csc.shop.item

import me.stepbystep.api.chat.*
import org.bukkit.Material
import ru.cristalix.csc.shop.CSCDonate

enum class CSCClass(
    val color: String,
    val rawName: PMessage0,
    override val material: Material,
    description: List<PMessage0>,
    override val data: Byte = 0,
    val chatName: PMessage0 = rawName,
) : CSCItem {
    Archer(
        color = DARK_GRAY,
        rawName = message0(
            "Лучник",
            "Archer",
        ),
        material = Material.BOW,
        description = listOf(
            message0(
                "+18% урон с луков",
                "+18% arrow damage",
            ),
        ),
    ),
    Swordsman(
        color = DARK_GRAY,
        rawName = message0(
            "Мечник",
            "Swordsman",
        ),
        material = Material.WOOD_SWORD,
        description = listOf(
            message0(
                "+15% урон с мечей",
                "+15% melee damage",
            ),
        ),
    ),
    Berserk(
        color = RED,
        rawName = message0(
            "Берсерк",
            "Berserk",
        ),
        material = Material.DIAMOND_AXE,
        description = listOf(
            message0(
                "Скорость и урон увеличиваются на: ",
                "Speed and damage are increased by: ",
            ),
            message0(
                "  10%, если уровень здоровья меньше 42.5%",
                "  10%, if health is below 42.5%",
            ),
            message0(
                "  15%, если уровень здоровья меньше 22.5%",
                "  15%, if health is below 22.5%",
            ),
            message0(
                "  20%, если уровень здоровья меньше 12.5%",
                "  20%, if health is below 12.5%",
            ),
        ),
    ),
    Lucky(
        color = GREEN,
        rawName = message0(
            "Везунчик",
            "Lucky",
        ),
        material = Material.WATER_LILY,
        description = listOf(
            message0(
                "+25% валюты за волны",
                "+25% money for waves",
            ),
        ),
    ),
    Gladiator(
        color = GOLD,
        rawName = message0(
            "Гладиатор",
            "Gladiator",
        ),
        material = Material.SHIELD,
        description = listOf(
            message0(
                "+45% валюты за победы на дуэлях",
                "+45% money for duel victories",
            ),
        ),
    ),
    Alchemist(
        color = DARK_PURPLE,
        rawName = message0(
            "Алхимик",
            "Alchemist",
        ),
        material = Material.POTION,
        description = listOf(
            message0(
                "Уровень всех выпиваемых зелий увеличен на 1",
                "All consumed potions level increased by 1",
            ),
        ),
    ),
    Titan(
        color = DARK_GREEN,
        rawName = message0(
            "Титан",
            "Titan",
        ),
        material = Material.DIAMOND_CHESTPLATE,
        description = listOf(
            message0(
                "+4 сердца",
                "+4 hearts",
            ),
        ),
    ),
    Trickster(
        color = WHITE,
        rawName = message0(
            "Ловкач",
            "Trickster",
        ),
        material = Material.DIAMOND_BOOTS,
        description = listOf(
            message0(
                "+20% шанс увернуться от атаки",
                "+20% chance to dodge attack",
            ),
        ),
    ),
    WeaponMaster(
        color = RED,
        rawName = message0(
            "Мастер оружия",
            "Weapon master",
        ),
        material = Material.IRON_SWORD,
        description = listOf(
            message0(
                "Шанс 7% нанести х2 урон",
                "7% chance to deal х2 damage",
            ),
        ),
    ),
    Assassin(
        color = GREEN,
        rawName = message0(
            "Ассасин",
            "Assassin",
        ),
        material = Material.FEATHER,
        description = listOf(
            message0(
                "+20% скорости бега",
                "+20% movement speed",
            ),
        ),
    ),
    Gamer(
        color = YELLOW,
        rawName = message0(
            "Азартный",
            "Gambling",
        ),
        material = Material.GOLD_NUGGET,
        description = listOf(
            message0(
                "+20% валюты с выигрышных ставок",
                "+20% money from winning bets",
            ),
        ),
    ),
    Careful(
        color = DARK_GREEN,
        rawName = message0(
            "Бережной",
            "Careful",
        ),
        material = Material.ENDER_CHEST,
        description = listOf(
            message0(
                "Возвращает 40% проигранных ставок",
                "Returns 40% of lost bets",
            ),
        ),
    ),
    Revivalist(
        color = YELLOW,
        rawName = message0(
            "Реинкарнатор",
            "Revivalist",
        ),
        material = Material.TOTEM,
        description = listOf(
            message0(
                "Дает 1 дополнительное перерождение",
                "Grants 1 additional revive",
            ),
        ),
    ),
    Collector(
        color = GOLD,
        rawName = message0(
            "Собиратель",
            "Collector",
        ),
        material = Material.YELLOW_SHULKER_BOX,
        description = listOf(
            message0(
                "+3 монеты за каждую волну, начиная с 3-ей",
                "+3 coins for each wave starting from the 3rd",
            ),
        ),
    ),
    MonsterHunter(
        color = DARK_GRAY,
        rawName = message0(
            "Охотник на монстров",
            "Monster hunter",
        ),
        material = Material.SPIDER_EYE,
        description = listOf(
            message0(
                "+15% урона по монстрам",
                "+15% damage against monsters",
            ),
        ),
    ),
    Vampire(
        color = RED,
        rawName = message0(
            "Вампир",
            "Vampire",
        ),
        material = Material.REDSTONE,
        description = listOf(
            message0(
                "Шанс 12.5% вылечить себя на 25% от нанесенного урона",
                "12.5% chance to heal yourself for 25% of dealt damage",
            ),
        ),
    ),
    Pufferfish(
        color = GRAY,
        rawName = message0(
            "Иглогрив",
            "Bristleback",
        ),
        material = Material.RAW_FISH,
        description = listOf(
            message0(
                "-10% получаемого урона (-37.5% при уроне в спину)",
                "-10% damage taken (-37.5% for back damage)",
            ),
        ),
        data = 3,
    ),
    Killer(
        color = RED,
        rawName = message0(
            "Головорез",
            "Cutthroat",
        ),
        material = Material.DIAMOND_SWORD,
        description = listOf(
            message0(
                "Шанс 1% при ударе убить монстра",
                "1% chance to execute monster on hit",
            ),
        ),
    ),
    TreasureHunter(
        color = GOLD,
        rawName = message0(
            "Охотник за сокровищами",
            "Treasure hunter",
        ),
        material = Material.CHEST,
        description = listOf(
            message0(
                "Шанс выпадения предмета из моба на 100% выше",
                "Chance to drop an item from monster is 100% higher",
            ),
        ),
        chatName = message0(
            "Охотн. за сокров.",
            "Treasure hunter",
        ),
    ),
    Blacksmith(
        color = DARK_GRAY,
        rawName = message0(
            "Кузнец",
            "Blacksmith",
        ),
        material = Material.ANVIL,
        description = listOf(
            message0(
                "Стоимость апгрейдов предметов снижена на 12.5%",
                "Item upgrade cost is reduced by 12.5%",
            ),
        ),
    ),
    ;

    override val displayName = rawName.prefixed(color)

    override val description = description.mapText {
        "$GRAY$it"
    }

    override val requiredDonate by lazy {
        CSCDonate.VALUES.filterIsInstance<CSCDonate.GameClassDonate>().find { it.clazz == this }
    }
}
