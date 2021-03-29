package ru.cristalix.csc.enchant.type

import me.stepbystep.api.RANDOM
import me.stepbystep.api.asNMS
import me.stepbystep.api.chat.*
import me.stepbystep.api.heal
import me.stepbystep.api.inTicksInt
import me.stepbystep.api.item.itemInHand
import org.bukkit.Material
import org.bukkit.entity.Creature
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.cristalix.csc.enchant.EnchantDataType
import ru.cristalix.csc.enchant.ItemEnchant
import ru.cristalix.csc.enchant.getEnchant
import ru.cristalix.csc.enchant.getRandomDamage
import kotlin.time.seconds

enum class SwordEnchant(
    override val key: String,
    override val dataType: EnchantDataType,
    override val description: String,
    override val icon: String,
) : ItemEnchant {

    DAMAGE("damage", EnchantDataType.IntegerRange, "Урон", "${RED}⚔") {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            e.damage += getRandomDamage(rawData)
        }
    },

    MONSTER_DAMAGE("monsterDamage", EnchantDataType.Integer { "$it%" }, "Урон по крипам", "$RED☠") {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            if (e.entity is Player) return
            val percent = rawData.parse<Int>() / 100.0
            e.damage *= percent
        }
    },

    PLAYER_DAMAGE("playerDamage", EnchantDataType.Integer { "$it%" }, "Урон по игрокам", "$RED☻") {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            if (e.entity !is Player) return
            val percent = rawData.parse<Int>() / 100.0
            e.damage *= percent
        }
    },

    CRIT_CHANCE(
        "critChance",
        EnchantDataType.IntegerArray(2) {
            "Шанс ${it[0]}% нанести при ударе ${it[1]}% урона"
        },
        "Шанс крит. атаки",
        "${YELLOW}ϟ"
    ) {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            val data = rawData.parse<IntArray>()
            if (RANDOM.nextInt(0, 100) < data[0]) {
                e.damage *= data[1] / 100.0
            }
        }
    },

    VAMPIRISM("vampirism", EnchantDataType.Integer { "$it%" }, "Вампиризм", "${RED}۩") {
        override fun applyForDamager(
            e: EntityDamageByEntityEvent,
            damager: Player,
            rawData: ByteArray,
        ) {
            val percent = rawData.parse<Int>() / 100.0
            damager.heal(e.damage * percent)
        }
    },

    COUNTER_HIT(
        "counterHit",
        EnchantDataType.IntegerArray(2) {
            "Шанс ${it[0]}% нанести ${it[1] / 100.0}x урона контратакой"
        },
        "Контратака",
        "${GOLD}▨"
    ) {
        override fun applyForDamaged(e: EntityDamageByEntityEvent, damager: LivingEntity, rawData: ByteArray) {
            val data = rawData.parse<IntArray>()
            if (RANDOM.nextInt(0, 100) >= data[0]) return

            val entity = e.entity as Player
            val damageEnchant = entity.asNMS().itemInHand.getEnchant(DAMAGE) ?: return
            val damage = getRandomDamage(damageEnchant.rawData)
            val percent = data[1] / 100.0
            damager.damage(damage * percent)
        }
    },

    POISON(
        "poison",
        EnchantDataType.IntegerArray(3) {
            "Шанс ${it[0]}% наложить отравление ${it[1]} уровня на ${it[2]} сек."
        },
        "Отравление",
        "${GREEN}☣"
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            val data = rawData.parse<IntArray>()
            val entity = e.entity as? LivingEntity ?: return
            if (RANDOM.nextInt(0, 100) >= data[0]) return
            val potionEffect = PotionEffect(PotionEffectType.POISON, data[2].seconds.inTicksInt, data[1] - 1)
            entity.addPotionEffect(potionEffect)
        }
    },

    BLINDNESS(
        "blindness",
        EnchantDataType.IntegerArray(2) {
            "Шанс ${it[0]}% ослепить врага на ${it[1]} сек."
        },
        "Слепота",
        "${DARK_GRAY}✣"
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            val data = rawData.parse<IntArray>()
            val entity = e.entity as? LivingEntity ?: return
            if (RANDOM.nextInt(0, 100) >= data[0]) return
            val potionEffect = PotionEffect(PotionEffectType.BLINDNESS, data[1].seconds.inTicksInt, 0)
            entity.addPotionEffect(potionEffect)
            if (entity is Creature && entity !is Player) {
                entity.target = null
            }
        }
    },

    IGNITE(
        "ignite",
        EnchantDataType.IntegerArray(3) {
            "Шанс ${it[0]}% поджечь врага на ${it[1]} сек. (урон мобам ${it[2]}/сек.)"
        },
        "Поджигание",
        "${RED}㈫"
    ) {
        override fun applyForDamager(e: EntityDamageByEntityEvent, damager: Player, rawData: ByteArray) {
            val data = rawData.parse<IntArray>()
            if (RANDOM.nextInt(0, 100) >= data[0]) return
            e.entity.fireTicks = data[1].seconds.inTicksInt

            val metadata = FixedMetadataValue(ItemEnchant.PLUGIN, data[2])
            e.entity.setMetadata(key, metadata)
        }
    }
    ;

    override fun canBeAppliedTo(type: Material) = type in arrayOf(
        Material.WOOD_SWORD, Material.STONE_SWORD, Material.GOLD_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD
    )
}
