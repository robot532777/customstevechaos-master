package ru.cristalix.csc.shop.item

import me.stepbystep.api.asBukkit
import me.stepbystep.api.chat.*
import me.stepbystep.api.displayName
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.accessTag
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.cristalix.csc.shop.CSCDonate
import ru.cristalix.csc.util.isFrozen
import ru.cristalix.csc.util.mixNewYearColors

enum class CSCWeaponSkin(
    override val displayName: PMessage0,
    override val material: Material = Material.IRON_SWORD,
    override val data: Byte = 0,
    private val displayTag: String? = null,
    private val skins: Map<Material, Skin>,
) : CSCItem {

    Default(
        displayName = message0(
            "${GRAY}Стандартный пак оружия",
            "${GRAY}Default weapon pack"
        ),
        skins = emptyMap(),
    ),
    Daggers(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Ножи",
            "${GRAY}Weapon pack ${GREEN}Daggers"
        ),
        displayTag = "iron_dagger",
        skins = SkinUtil.getSkinsMap(
            tag = "dagger",
            displayNames = listOf(
                "Деревянный нож", "Каменный нож", "Золотой нож",
                "Железный нож", "Алмазный нож", "Нож заморозки",
            )
        ),
    ),
    Axes(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Топоры",
            "${GRAY}Weapon pack ${GREEN}Axes"
        ),
        displayTag = "iron_battleaxe",
        skins = SkinUtil.getSkinsMap(
            tag = "battleaxe",
            displayNames = listOf(
                "Деревянный топор", "Каменный топор", "Золотой топор",
                "Железный топор", "Алмазный топор", "Топор заморозки",
            )
        ),
    ),
    Broadswords(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Палаши",
            "${GRAY}Weapon pack ${GREEN}Broadswords"
        ),
        displayTag = "iron_broadsword",
        skins = SkinUtil.getSkinsMap(
            tag = "broadsword",
            displayNames = listOf(
                "Деревянный палаш", "Каменный палаш", "Золотой палаш",
                "Железный палаш", "Алмазный палаш", "Палаш заморозки",
            )
        ),
    ),
    Glaives(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Глефы",
            "${GRAY}Weapon pack ${GREEN}Glaives"
        ),
        displayTag = "iron_glaive",
        skins = SkinUtil.getSkinsMap(
            tag = "glaive",
            displayNames = listOf(
                "Деревянная глефа", "Каменная глефа", "Золотая глефа",
                "Железная глефа", "Алмазная глефа", "Глефа заморозки",
            )
        ),
    ),
    Halberds(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Алебарды",
            "${GRAY}Weapon pack ${GREEN}Halberds"
        ),
        displayTag = "iron_halberd",
        skins = SkinUtil.getSkinsMap(
            tag = "halberd",
            displayNames = listOf(
                "Деревянная алебарда", "Каменная алебарда", "Золотая алебарда",
                "Железная алебарда", "Алмазная алебарда", "Алебарда заморозки",
            )
        ),
    ),
    Maces(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Булавы",
            "${GRAY}Weapon pack ${GREEN}Maces"
        ),
        displayTag = "iron_mace",
        skins = SkinUtil.getSkinsMap(
            tag = "mace",
            displayNames = listOf(
                "Деревянная булава", "Каменная булава", "Золотая булава",
                "Железная булава", "Алмазная булава", "Булава заморозки",
            )
        ),
    ),
    Scythes(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Косы",
            "${GRAY}Weapon pack ${GREEN}Scythes"
        ),
        displayTag = "iron_scythe",
        skins = SkinUtil.getSkinsMap(
            tag = "scythe",
            displayNames = listOf(
                "Деревянная коса", "Каменная коса", "Золотая коса",
                "Железная коса", "Алмазная коса", "Коса заморозки",
            )
        ),
    ),
    Spears(
        displayName = message0(
            "${GRAY}Пак оружия ${GREEN}Копья",
            "${GRAY}Weapon pack ${GREEN}Spears"
        ),
        displayTag = "iron_spear",
        skins = SkinUtil.getSkinsMap(
            tag = "spear",
            displayNames = listOf(
                "Деревянное копьё", "Каменное копьё", "Золотое копьё",
                "Железное копьё", "Алмазное копьё", "Копьё заморозки",
            )
        ),
    ),
    NewYear(
        displayName = message0(
            russian = "${GRAY}Пак оружия ${"Новогодний".mixNewYearColors()}",
            english = "${GRAY}Weapon pack ${"New Year".mixNewYearColors()}"
        ),
        displayTag = "xsmas_iron",
        skins = mapOf(
            Material.WOOD_SWORD to Skin("xsmas_wood", "Новогодний деревянный меч"),
            Material.STONE_SWORD to Skin("xsmas_stone", "Новогодний каменный меч"),
            Material.GOLD_SWORD to Skin("xsmas_gold", "Новогодний золотой меч"),
            Material.IRON_SWORD to Skin("xsmas_iron", "Новогодний железный меч"),
            Material.DIAMOND_SWORD to Skin("xsmas_diamond", "Новогодний алмазный меч"),
            SkinUtil.frozenSkinMaterial to Skin("xsmas_freez", "Новогодний меч заморозки"),
        )
    )
    ;

    private val frostSkin: Skin = skins.toMutableMap().remove(SkinUtil.frozenSkinMaterial)
        ?: Skin(SkinUtil.FROZEN_SKIN_DEFAULT_TAG, SkinUtil.FROZEN_SKIN_DEFAULT_NAME)

    override fun createDisplayItem(player: Player): NMSItemStack {
        val result = super.createDisplayItem(player)
        val itemTag = displayTag ?: return result

        result.accessTag {
            setString(WEAPON_SKIN_TAG, itemTag)
        }
        return result
    }

    override val requiredDonate by lazy {
        CSCDonate.VALUES.filterIsInstance<CSCDonate.WeaponSkinDonate>().find { it.weaponSkin == this }
    }

    override val description get() = emptyList<PMessage0>()

    fun applySkin(item: NMSItemStack) {
        val material = item.item.asBukkit()
        val isFrozen = item.isFrozen

        val skin = when {
            isFrozen -> frostSkin
            else -> skins[material]
        } ?: return

        item.displayName = skin.displayName
        item.accessTag {
            setString(WEAPON_SKIN_TAG, skin.tag)
        }
    }

    fun getItemWithSkin(item: NMSItemStack): NMSItemStack = item.cloneItemStack().apply(::applySkin)

    data class Skin(
        val tag: String,
        val displayName: String,
    )

    // cannot use companion object here
    private object SkinUtil {
        const val FROZEN_SKIN_DEFAULT_TAG = "frost_sword"
        const val FROZEN_SKIN_DEFAULT_NAME = "${WHITE}Меч заморозки"
        val frozenSkinMaterial = Material.ICE

        fun getSkinsMap(tag: String, displayNames: List<String>): Map<Material, Skin> {
            val items = listOf(
                "wooden_" to Material.WOOD_SWORD,
                "stone_" to Material.STONE_SWORD,
                "golden_" to Material.GOLD_SWORD,
                "iron_" to Material.IRON_SWORD,
                "diamond_" to Material.DIAMOND_SWORD,
                "frost_" to frozenSkinMaterial,
            )

            return displayNames.mapIndexed { index, name ->
                val (prefix, material) = items[index]
                material to Skin("$prefix$tag", "$WHITE$name")
            }.toMap()
        }
    }

    companion object {
        const val WEAPON_SKIN_TAG = "weapons"
    }
}
