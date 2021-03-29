package ru.cristalix.csc.util

import me.stepbystep.api.*
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.PMessage2
import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.WHITE
import me.stepbystep.api.item.*
import me.stepbystep.api.menu.MenuHolder
import net.minecraft.server.v1_12_R1.*
import org.apache.commons.math3.util.Pair
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionType
import ru.cristalix.csc.UUID_TAG
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.phase.DuelPhase
import ru.cristalix.csc.phase.ScheduleNextWavePhase
import ru.cristalix.csc.phase.SpawnMobsPhase
import ru.cristalix.csc.player.CSCPlayer
import ru.cristalix.csc.player.CSCTeam
import java.util.*
import kotlin.collections.HashMap

typealias ApachePair<K, V> = Pair<K, V>
typealias BalanceMessageFormatter = PMessage2<Int, Int>

fun HumanEntity.asCSCPlayer(): CSCPlayer = CSCPlayer.get(uniqueId)
fun HumanEntity.asCSCPlayerOrNull(): CSCPlayer? = CSCPlayer.getOrNull(uniqueId)
fun UUID.asCSCPlayer(): CSCPlayer = CSCPlayer.get(this)

fun Material.isRefillable(): Boolean {
    return isEdible || this == Material.POTION || this == Material.LINGERING_POTION || this == Material.SPLASH_POTION ||
            this == Material.SHIELD || this == Material.BOW
}

var NMSItemStack.upgradeLevel by IntTag("upgradeLevel", 0)
var NMSItemStack.isTransferable by BooleanTag("isTransferable")
var NMSItemStack.transferTransactions by StringListTag(TRANSFER_TRANSACTIONS_KEY)

val NMSItemStack.isCurrentlyTransferred: Boolean get() = !transferTransactions.isEmpty
inline fun NMSItemStack.updateTransferTransactions(action: (NBTTagList) -> Unit) {
    val newTransferTransactions = transferTransactions.also(action)
    if (newTransferTransactions.isEmpty) {
        tag?.remove(TRANSFER_TRANSACTIONS_KEY)
    } else {
        transferTransactions = newTransferTransactions
    }
}

fun Player.createHeadItem(): NMSItemStack = createBukkitItem(
    material = Material.SKULL_ITEM,
    data = 3,
    displayName = "$GREEN$name"
).also {
    it.itemMeta = (it.itemMeta as SkullMeta).apply {
        owningPlayer = this@createHeadItem
    }
}.asNMS()

val GameScheduler.duelPhase: DuelPhase?
    get() = when (val phase = currentPhase) {
        is ScheduleNextWavePhase -> phase.duelPhase
        is SpawnMobsPhase -> phase.duelPhase
        else -> null
    }

var EntityLiving.speed: Double
    get() = getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).value
    set(value) {
        getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).value = value
    }

var EntityLiving.damage: Double
    get() = getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).value
    set(value) {
        getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).value = value
    }

fun NMSItemStack.isSimilarWithoutDisplayAndUUID(other: NMSItemStack): Boolean {
    if (item != other.item || data != other.data) return false
    val firstTag = tag
    val secondTag = other.tag
    if (firstTag == null && secondTag == null) return true

    val firstMap = firstTag.map
    val secondMap = secondTag.map
    if (firstMap.size != secondMap.size) return false

    for ((key, value) in firstMap) {
        if (key != UUID_TAG && key != DISPLAY_TAG && value != secondMap[key]) {
            return false
        }
    }

    return true
}

fun GameScheduler.getInstantHealthRegeneration(rawAmount: Double): Double = 1.5 * rawAmount + displayWaveIndex / 2.0

fun NMSItemStack.updateLoreIfNeed(player: Player, gameScheduler: GameScheduler) = apply {
    val text = when (val item = item) {
        Items.POTION -> {
            val potionMeta = asCraftMirror().itemMeta as PotionMeta
            if (potionMeta.basePotionData.type != PotionType.INSTANT_HEAL) return@apply
            val amplifier = if (potionMeta.basePotionData.isUpgraded) 1 else 0
            val rawHealAmount = 4 shl amplifier
            val healAmount = gameScheduler.getInstantHealthRegeneration(rawHealAmount.toDouble()) / 2
            "$WHITE${CommonMessages.heals(player)}: $GREEN$healAmount ${RED}❤"
        }
        is ItemSword -> {
            val rawDamage = item.a
            val sharpnessLevel = asCraftMirror().getEnchantmentLevel(Enchantment.DAMAGE_ALL)
            val sharpnessDamage = if (sharpnessLevel != 0) (sharpnessLevel + 1) * 0.5 else 0.0
            val bookDamage = player.asCSCPlayerOrNull()?.additionalDamage ?: 0
            val totalDamage = rawDamage + sharpnessDamage + bookDamage + 1 // 1 = damage from hand
            "$WHITE${CommonMessages.damage(player)}: $GREEN$totalDamage"
        }
        Items.SHIELD -> {
            val durability = shieldDurability
            if (durability == 0) return@apply
            "${WHITE}Прочность: $GREEN$durability"
        }
        else -> return@apply
    }

    updateLore {
        it.setOrAdd(0, text)
    }
}

fun Player.spawnParticlesAround(
    particle: Particle,
    blockFaces: Array<out BlockFace>,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    offsetZ: Double = 0.0,
    count: Int = 1,
) {
    val world = player.world
    val nmsPlayer = player.asNMS()
    val startY = nmsPlayer.locY

    for (blockFace in blockFaces) {
        val x = nmsPlayer.locX + blockFace.modX * 0.5
        val z = nmsPlayer.locZ + blockFace.modZ * 0.5

        for (j in 0.0..2.0 step 0.4) {
            world.spawnParticle(particle, x, startY + j, z, count, offsetX, offsetY, offsetZ)
        }
    }
}

inline fun <reified T : MenuHolder> getOnlinePlayersWithOpenedMenu(
    additionalPredicate: (T) -> Boolean = { true }
): List<Player> {
    return Bukkit.getOnlinePlayers().filter { player ->
        val holder = player.openInventory.topInventory?.holder
        holder is T && additionalPredicate(holder)
    }
}

inline fun <reified T> Player.hasOpenedMenu(): Boolean =
    openInventory.topInventory?.holder is T

fun CSCTeam.getSpawnLocations(initial: Location): Map<CSCPlayer, Location> =
    livingPlayers.getSpawnLocations(initial, 3.0)

fun CSCTeam.getBukkitSpawnLocations(initial: Location): Map<Player, Location> =
    livingBukkitPlayers.getSpawnLocations(initial, 3.0)

fun <T> List<T>.getSpawnLocations(initial: Location, eachWidth: Double): Map<T, Location> {
    if (isEmpty()) return mapOf()
    if (size == 1) return mapOf(single() to initial)

    val totalWidth = size * eachWidth
    val rotatedDirection = EnumDirection.fromAngle(initial.yaw + 90.0)

    var spawnLocation = initial.clone().add(
        rotatedDirection.adjacentX * (totalWidth / 2),
        0.0,
        rotatedDirection.adjacentZ * (totalWidth / 2),
    )

    return associateWithTo(HashMap(size)) {
        spawnLocation.also {
            spawnLocation = spawnLocation.clone().add(
                rotatedDirection.adjacentX * eachWidth,
                0.0,
                rotatedDirection.adjacentZ * eachWidth,
            )
        }
    }
}

fun NMSPlayerInventory.getItemOrCursor(slot: Int): NMSItemStack =
    if (slot == CURSOR_SLOT) carried else getItem(slot)

fun NMSPlayerInventory.setItemOrCursor(slot: Int, stack: NMSItemStack) {
    if (slot == CURSOR_SLOT) {
        carried = stack
    } else {
        setItem(slot, stack)
    }
}

var EntityLiving.maximumHealth: Double
    get() = getAttributeInstance(GenericAttributes.maxHealth).value
    set(value) {
        getAttributeInstance(GenericAttributes.maxHealth).value = value
        if (health > value) {
            health = value.toFloat()
        }
    }

fun Player.hasSameTeam(other: Player): Boolean =
    asCSCPlayerOrNull()?.team == other.asCSCPlayerOrNull()?.team
