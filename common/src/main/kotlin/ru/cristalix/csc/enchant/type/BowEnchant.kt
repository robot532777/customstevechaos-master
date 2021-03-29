package ru.cristalix.csc.enchant.type

import me.stepbystep.api.chat.YELLOW
import me.stepbystep.api.heal
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.cristalix.csc.enchant.EnchantDataType
import ru.cristalix.csc.enchant.ItemEnchant
import ru.cristalix.csc.enchant.getRandomDamage

enum class BowEnchant(
    override val key: String,
    override val dataType: EnchantDataType,
    override val description: String,
    override val icon: String,
) : ItemEnchant {
    DAMAGE("bowDamage", EnchantDataType.IntegerRange, "Урон", SwordEnchant.DAMAGE.icon) {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            e.damage += getRandomDamage(rawData)
            if (e.entity !is Player) {
                e.damage *= 2
            }
        }
    },

    VAMPIRISM("bowVampirism", EnchantDataType.Integer { "$it%" }, "Вампиризм", SwordEnchant.VAMPIRISM.icon) {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            val percent = rawData.parse<Int>() / 100.0
            damager.heal(e.damage * percent)
        }
    },

    IGNITE(
        "bowIgnite",
        EnchantDataType.IntegerArray(3) {
            "Шанс ${it[0]}% поджечь врага на ${it[1]} сек. (урон мобам ${it[2]}/сек.)"
        },
        "Поджигание",
        SwordEnchant.IGNITE.icon
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            SwordEnchant.IGNITE.applyForDamager(e, damager, rawData)
        }
    },

    POISON(
        "bowPoison",
        EnchantDataType.IntegerArray(3) {
            "Шанс ${it[0]}% наложить отравление ${it[1]} уровня на ${it[2]} сек."
        },
        "Отравление",
        SwordEnchant.POISON.icon
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            SwordEnchant.POISON.applyForDamager(e, damager, rawData)
        }
    },

    KNOCKBACK("bowKnockback", EnchantDataType.Integer(Int::toString), "Откидывание", "${YELLOW}⇨") {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            val level = rawData.parse<Int>()
            val arrow = e.damager as Arrow
            arrow.knockbackStrength = level
        }
    }
    ;

    override fun canBeAppliedTo(type: Material) = type == Material.BOW
}
