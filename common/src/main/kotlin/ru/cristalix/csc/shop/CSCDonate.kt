package ru.cristalix.csc.shop

import me.stepbystep.api.chat.*
import me.stepbystep.api.displayName
import me.stepbystep.api.getDeclaredObjectsOfType
import me.stepbystep.api.item.ItemTag
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.lore
import net.minecraft.server.v1_12_R1.NBTTagInt
import net.minecraft.server.v1_12_R1.NBTTagString
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import ru.cristalix.csc.shop.item.*
import ru.cristalix.csc.util.mixNewYearColors

sealed class CSCDonate(
    val price: Int,
    val displayName: PMessage0,
    val applyAction: DonateApplyAction,
) {
    companion object {
        private val foreverDonateMessage = message0(
            "${YELLOW}Покупается навсегда!",
            "${YELLOW}Applies forever!",
        )

        val VALUES by getDeclaredObjectsOfType<CSCDonate>()

        private val BY_KEY by lazy {
            VALUES.associateBy { it.key }
        }

        fun byKey(key: String) = BY_KEY[key]
    }

    val key: String = javaClass.simpleName
    open val isInDonateShop: Boolean get() = false

    open fun isPurchased(donates: Set<CSCDonate>): Boolean = this in donates

    abstract fun createDisplayItem(player: Player): NMSItemStack

    open class LootBoxDonate(
        price: Int = Int.MAX_VALUE,
        applyAction: DonateApplyAction,
        private val item: CSCItem,
        val chancePercent: Int,
    ) : CSCDonate(price, item.displayName, applyAction) {
        open val canBeLooted: Boolean get() = true

        override fun createDisplayItem(player: Player) = item.createDisplayItem(player)
    }

    open class UpgradingDonate(val donates: List<CSCDonate>) :
        CSCDonate(Int.MAX_VALUE, message0("", ""), DonateApplyAction.AddPermanent) {

        override val isInDonateShop get() = true

        override fun createDisplayItem(player: Player): NMSItemStack {
            throw UnsupportedOperationException("Cannot create display item for UpgradingDonate")
        }
    }

    open class GameClassDonate(val clazz: CSCClass) : LootBoxDonate(
        applyAction = DonateApplyAction.AddPermanent,
        item = clazz,
        chancePercent = 100,
    )

    open class CageDonate(val cage: CSCCage) : LootBoxDonate(
        applyAction = DonateApplyAction.AddPermanent,
        item = cage,
        chancePercent = 100,
    )

    open class MoneyDonate(price: Int, amount: Int, private val tag: String) : CSCDonate(
        price = price,
        displayName = message0(
            "$GOLD$amount ${GREEN}монет",
            "$GOLD$amount ${GREEN}coins",
        ),
        applyAction = DonateApplyAction.AddMoney(amount),
    ) {
        override val isInDonateShop get() = true

        override fun createDisplayItem(player: Player) = createNMSItem(
            material = Material.CLAY_BALL,
            displayName = displayName(player),
            customTag = ItemTag("other", NBTTagString(tag)),
        )
    }

    interface MessagePackDonate {
        val messagePack: CSCMessagePack
    }

    open class MessagePackLootBoxDonate(override val messagePack: CSCMessagePack) : LootBoxDonate(
        applyAction = DonateApplyAction.AddPermanent,
        item = messagePack,
        chancePercent = 50,
    ), MessagePackDonate

    open class WeaponSkinDonate(val weaponSkin: CSCWeaponSkin) : LootBoxDonate(
        applyAction = DonateApplyAction.AddPermanent,
        item = weaponSkin,
        chancePercent = 25,
    ) {
        protected open val isUnlockedByAllSkins: Boolean get() = true

        override fun isPurchased(donates: Set<CSCDonate>): Boolean =
            super.isPurchased(donates) || (isUnlockedByAllSkins && BuyAllSkinsDonate in donates)
    }

    object Reroll : CSCDonate(
        price = 150,
        displayName = message0(
            "${GREEN}Перемешать предметы",
            "${GREEN}Shuffle items",
        ),
        applyAction = DonateApplyAction.AddPermanent,
    ) {
        private val loreMessage = message0(
            "${YELLOW}Дает возможность 2 раза за игру перемешать предметы",
            "${YELLOW}Enables you to shuffle items twice per game"
        )

        override fun createDisplayItem(player: Player) = createNMSItem(
            material = Material.CLAY_BALL,
            customTag = ItemTag("thief", NBTTagInt(9)),
            displayName = displayName(player),
            lore = listOf(
                foreverDonateMessage(player),
                loreMessage(player),
            ),
        )
    }

    object Reroll3 : CSCDonate(
        price = 499,
        displayName = Reroll.displayName,
        applyAction = DonateApplyAction.AddPermanent,
    ) {
        private val loreMessage1 = message0(
            "${YELLOW}Дает возможность еще 1 раз (всего 3 раза)",
            "${YELLOW}Enables you to shuffle items",
        )

        private val loreMessage2 = message0(
            "${YELLOW}за игру перемешать предметы.",
            "${YELLOW}once more (totally 3)",
        )

        override fun createDisplayItem(player: Player) = createNMSItem(
            material = Material.CLAY_BALL,
            customTag = ItemTag("thief", NBTTagInt(9)),
            displayName = displayName(player),
            lore = listOf(
                foreverDonateMessage(player),
                loreMessage1(player),
                loreMessage2(player),
            ),
            invisibleEnchant = true,
        )
    }

    object RerollsDonate : UpgradingDonate(listOf(Reroll, Reroll3))

    object ClassSelection : CSCDonate(
        price = 100,
        displayName = message0(
            "${GREEN}5 классов",
            "${GREEN}5 classes",
        ),
        applyAction = DonateApplyAction.AddPermanent,
    ) {
        override val isInDonateShop get() = true

        private val loreMessage = message0(
            "${YELLOW}Вам будет даваться на выбор 5 классов вместо 3",
            "${YELLOW}You will be given a choice of 5 classes instead of 3"
        )

        override fun createDisplayItem(player: Player) = createNMSItem(
            material = Material.TOTEM,
            displayName = displayName(player),
            lore = listOf(
                foreverDonateMessage(player),
                loreMessage(player)
            ),
        )
    }

    object Berserk : GameClassDonate(CSCClass.Berserk)
    object Careful : GameClassDonate(CSCClass.Careful)
    object Revivalist : GameClassDonate(CSCClass.Revivalist)
    object Collector : GameClassDonate(CSCClass.Collector)
    object MonsterHunter : GameClassDonate(CSCClass.MonsterHunter)
    object Vampire : GameClassDonate(CSCClass.Vampire)
    object Pufferfish : GameClassDonate(CSCClass.Pufferfish)
    object Killer : GameClassDonate(CSCClass.Killer)
    object TreasureHunter : GameClassDonate(CSCClass.TreasureHunter)
    object Blacksmith : GameClassDonate(CSCClass.Blacksmith)

    object Prison : CageDonate(CSCCage.Prison)
    object Watermelon : CageDonate(CSCCage.Watermelon)
    object Golden : CageDonate(CSCCage.Golden)
    object Olympus : CageDonate(CSCCage.Olympus)
    object Woodcutter : CageDonate(CSCCage.Woodcutter)
    object Dragon : CageDonate(CSCCage.Dragon)
    object Nether : CageDonate(CSCCage.Nether)

    object SmallMoneyDonate : MoneyDonate(120, 3000, "coin")
    object MediumMoneyDonate : MoneyDonate(240, 7000, "coin2")
    object BigMoneyDonate : MoneyDonate(480, 15000, "coin4")

    object DemasterMessagePack : MessagePackLootBoxDonate(CSCMessagePack.Demaster)
    object UkraineMessagePack : MessagePackLootBoxDonate(CSCMessagePack.Ukraine)
    object HalloweenMessagePack :
        CSCDonate(300, CSCMessagePack.Halloween.displayName, DonateApplyAction.AddPermanent),
        MessagePackDonate {
        override val messagePack get() = CSCMessagePack.Halloween
        override fun createDisplayItem(player: Player) = messagePack.createDisplayItem(player)
    }

    object DemonHunterMessagepack : MessagePackLootBoxDonate(CSCMessagePack.DemonHunter)
    object NewYearMessagePack :
        CSCDonate(Int.MAX_VALUE, CSCMessagePack.NewYear.displayName, DonateApplyAction.AddPermanent),
            MessagePackDonate
    {
        override val messagePack: CSCMessagePack get() = CSCMessagePack.NewYear
        override fun createDisplayItem(player: Player) = CSCMessagePack.NewYear.createDisplayItem(player)
    }

    object Daggers : WeaponSkinDonate(CSCWeaponSkin.Daggers)
    object Axes : WeaponSkinDonate(CSCWeaponSkin.Axes)
    object Broadswords : WeaponSkinDonate(CSCWeaponSkin.Broadswords)
    object Glaives : WeaponSkinDonate(CSCWeaponSkin.Glaives)
    object Halberds : WeaponSkinDonate(CSCWeaponSkin.Halberds)
    object Maces : WeaponSkinDonate(CSCWeaponSkin.Maces)
    object Scythes : WeaponSkinDonate(CSCWeaponSkin.Scythes)
    object Spears : WeaponSkinDonate(CSCWeaponSkin.Spears)
    object NewYear : WeaponSkinDonate(CSCWeaponSkin.NewYear) {
        override val canBeLooted: Boolean get() = false
        override val isUnlockedByAllSkins: Boolean get() = false
    }

    object BuyAllSkinsDonate : CSCDonate(
        price = 900,
        displayName = message0(
            russian = "${GREEN}Разблокировать все скины",
            english = "${GREEN}Unlock all skins",
        ),
        applyAction = DonateApplyAction.AddPermanent,
    ) {
        override val isInDonateShop get() = true

        private val loreMessage = message0(
            russian = "${YELLOW}Разблокирует все скины, которые существуют",
            english = "${YELLOW}Unlocks all existing skins",
        )

        override fun createDisplayItem(player: Player): NMSItemStack = createNMSItem(
            material = Material.DIAMOND_SWORD,
            lore = listOf(loreMessage(player)),
            flags = ItemFlag.values(),
        ).also {
            CSCWeaponSkin.Axes.applySkin(it)
            it.displayName = displayName(player)
        }
    }

    object NewYearPackDonate : CSCDonate(
        price = 350,
        displayName = message0(
            russian = "Новогодний набор".mixNewYearColors(),
            english = "New Year pack".mixNewYearColors(),
        ),
        applyAction = DonateApplyAction.AddNewYearPack,
    ) {
        override val isInDonateShop: Boolean get() = true

        private val lore = listOf(
            message0(
                russian = "${GRAY}Включает в себя набор сообщений и пак оружия",
                english = "${GRAY}Includes messages and weapon pack",
            ),
            message0(
                russian = "$RED${BOLD}Можно купить до 14.01.2021",
                english = "$RED${BOLD}Can be bought until 14.01.2021",
            )
        )

        override fun createDisplayItem(player: Player) = NewYearMessagePack.createDisplayItem(player).also {
            it.displayName = displayName(player)
            it.lore = lore(player).toMutableList()
        }
    }
}
