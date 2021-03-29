package ru.cristalix.csc.enchant

import me.stepbystep.api.RANDOM
import me.stepbystep.api.item.COMPOUND_TAG_TYPE
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.accessTag
import me.stepbystep.api.wordForNum
import net.minecraft.server.v1_12_R1.NBTTagCompound
import net.minecraft.server.v1_12_R1.NBTTagList

data class AppliedEnchant(
    val type: ItemEnchant,
    val rawData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppliedEnchant

        if (type != other.type) return false
        if (!rawData.contentEquals(other.rawData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + rawData.contentHashCode()
        return result
    }
}

private const val ENCHANTS_KEY = "customEnchants"

var NMSItemStack.enchants: Map<ItemEnchant, ByteArray>
    get() {
        val tag = tag ?: return emptyMap()
        val enchantsTag = tag.getList(ENCHANTS_KEY, COMPOUND_TAG_TYPE)
            .takeUnless { it.isEmpty } ?: return emptyMap()

        val result = linkedMapOf<ItemEnchant, ByteArray>()

        for (i in 0 until enchantsTag.size()) {
            val enchantTag = enchantsTag[i]
            val enchant = ItemEnchant.byKey(enchantTag.getString("k"))
            result[enchant] = enchantTag.getByteArray("d")
        }

        return result
    }
    set(value) {
        accessTag {
            if (value.isEmpty()) {
                remove(ENCHANTS_KEY)
                return
            }
            val result = NBTTagList()
            val sorted = value.entries.sortedBy { it.key.ordinal }
            for ((type, rawData) in sorted) {
                val enchantTag = NBTTagCompound().apply {
                    setString("k", type.key)
                    setByteArray("d", rawData)
                }
                result.add(enchantTag)
            }
            set(ENCHANTS_KEY, result)
        }
    }

fun NMSItemStack.getEnchant(enchant: ItemEnchant): AppliedEnchant? {
    val tag = tag ?: return null
    val enchantsTag = tag.getList(ENCHANTS_KEY, COMPOUND_TAG_TYPE)
    for (i in 0 until enchantsTag.size()) {
        val enchantTag = enchantsTag[i]
        if (enchantTag.getString("k") == enchant.key) {
            return AppliedEnchant(enchant, enchantTag.getByteArray("d"))
        }
    }
    return null
}

fun ItemEnchant.getRandomDamage(rawData: ByteArray): Double {
    val range = dataType.parse<IntArray>(rawData)
    return RANDOM.nextInt(range[0], range[1] + 1).toDouble()
}

fun Int.secondsWord() = wordForNum("секунду", "секунды", "секунд")
