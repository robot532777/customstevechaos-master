package ru.cristalix.csc.enchant

import me.stepbystep.api.chat.GRAY
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.YELLOW
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin
import ru.cristalix.csc.enchant.type.ArmorEnchant
import ru.cristalix.csc.enchant.type.BowEnchant
import ru.cristalix.csc.enchant.type.SwordEnchant

interface ItemEnchant {
    val key: String
    val ordinal: Int
    val dataType: EnchantDataType
    val icon: String
    val description: String

    fun canBeAppliedTo(type: Material): Boolean

    fun getLoreInfo(rawData: ByteArray): String {
        val data = dataType.parse<Any>(rawData)
        return "$GRAY[$icon$GRAY] $YELLOW$description: $GREEN${dataType.toString(data)}"
    }

    fun <T> ByteArray.parse(): T = dataType.parse(this)

    fun applyForDamager(
        e: EntityDamageByEntityEvent,
        damager: Player,
        rawData: ByteArray,
    ) {
        // nothing
    }

    fun applyForDamaged(
        e: EntityDamageByEntityEvent,
        damager: LivingEntity,
        rawData: ByteArray,
    ) {
        // nothing
    }

    companion object {
        internal lateinit var PLUGIN: Plugin

        fun initPlugin(plugin: Plugin) {
            if (::PLUGIN.isInitialized) {
                error("Plugin $PLUGIN already initialized")
            }
            PLUGIN = plugin
        }

        fun byKeyOrNull(key: String): ItemEnchant? = all().find { it.key.equals(key, ignoreCase = true) }
        fun byKey(key: String): ItemEnchant = byKeyOrNull(key) ?: error("ItemEnchant with key = $key doesn't exist")

        fun ensureUniqueKeys() {
            var foundDuplicate = false
            all()
                .groupBy { it.key }
                .filter { it.value.size != 1 }
                .forEach { (key, count) ->
                    Bukkit.getLogger().severe("Found duplicate: $key, which is used ${count.size} times")
                    foundDuplicate = true
                }

            check(!foundDuplicate) { "Found duplicate" }
        }

        fun all(): Array<ItemEnchant> = arrayOf(
            *ArmorEnchant.values(),
            *BowEnchant.values(),
            *SwordEnchant.values()
        )
    }
}

