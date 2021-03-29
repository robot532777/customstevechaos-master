package ru.cristalix.csc.enchant.type

import me.stepbystep.api.RANDOM
import me.stepbystep.api.cancel
import me.stepbystep.api.chat.DARK_GRAY
import me.stepbystep.api.chat.GOLD
import me.stepbystep.api.chat.RED
import me.stepbystep.api.inTicksInt
import me.stepbystep.api.item.getArmorType
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.cristalix.csc.enchant.EnchantDataType
import ru.cristalix.csc.enchant.ItemEnchant
import kotlin.time.seconds

enum class ArmorEnchant(
    override val key: String,
    override val dataType: EnchantDataType,
    override val description: String,
    override val icon: String,
) : ItemEnchant {
    DAMAGE(
        "armorDamage",
        EnchantDataType.Integer(Int::toString),
        "Дополнительный урон в ближнем бою",
        SwordEnchant.DAMAGE.icon,
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            if (e.damager == damager) {
                e.damage += rawData.parse<Int>()
            }
        }
    },

    RANGE_DAMAGE(
        "armorRangeDamage",
        EnchantDataType.Integer(Int::toString),
        "Дополнительный урон в дальнем бою",
        "${RED}➹"
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            if (e.damager is Projectile) {
                e.damage += rawData.parse<Int>()
            }
        }
    },

    MORE_HEALTH(
        "armorHealth",
        EnchantDataType.Integer(Int::toString),
        "Дополнительное здоровье",
        "${RED}♥"
    ),

    DODGE(
        "dodge",
        EnchantDataType.Integer { "$it%" },
        "Шанс уворота",
        "${DARK_GRAY}✖"
    ) {
        override fun applyForDamaged(e: EntityDamageByEntityEvent, damager: LivingEntity, rawData: ByteArray) {
            val chance = rawData.parse<Int>()
            if (RANDOM.nextInt(0, 100) >= chance) return
            e.cancel()
        }
    },

    THORNS(
        "thorns",
        EnchantDataType.IntegerArray(2) {
            "Шанс ${it[0]}% отразить ${it[1]}% урона"
        },
        "Шипы",
        "${GOLD}▩"
    ) {
        override fun applyForDamaged(e: EntityDamageByEntityEvent, damager: LivingEntity, rawData: ByteArray) {
            val data = rawData.parse<IntArray>()
            if (RANDOM.nextInt(0, 100) >= data[0]) return
            val percent = data[1] / 100.0
            damager.damage(e.damage * percent)
        }
    },

    BLINDNESS(
        "armorBlindness",
        EnchantDataType.IntegerArray(2) {
            "Шанс ${it[0]}% ослепить врага на ${it[1]} сек."
        },
        "Слепота",
        SwordEnchant.BLINDNESS.icon
    ) {
        override fun applyForDamaged(e: EntityDamageByEntityEvent, damager: LivingEntity, rawData: ByteArray) {
            val data = rawData.parse<IntArray>()
            if (RANDOM.nextInt(0, 100) >= data[0]) return
            val potionEffect = PotionEffect(PotionEffectType.BLINDNESS, data[1].seconds.inTicksInt, 0)
            damager.addPotionEffect(potionEffect)
            (damager as? Monster)?.target = null
        }
    }
    ;


    override fun canBeAppliedTo(type: Material) = type.getArmorType() != null
}
