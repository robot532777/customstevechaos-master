package ru.cristalix.csc.util

import me.stepbystep.api.chat.AQUA
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.message1
import me.stepbystep.api.item.BooleanTag
import me.stepbystep.api.item.IntTag
import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.wordForNum

const val SCOREBOARD_NAME = "${AQUA}Custom Steve Chaos"
const val DUEL_MAP_WORLD = "duelworld"

fun menuSlotsIterator(): Iterator<Int> = generateSequence(10) {
    when {
        it % 9 == 7 -> it + 12
        else -> it + 2
    }
}.iterator()

fun compactMenuSlotsIterator(): Iterator<Int> = generateSequence(19) {
    when {
        it % 9 == 7 -> it + 3
        else -> it + 1
    }
}.iterator()

var NMSItemStack.isFrozen by BooleanTag("isFrozen")
var NMSItemStack.shieldDurability by IntTag("shieldDurability", 0)
var NMSItemStack.maxShieldDurability by IntTag("shieldMaxDurability", 3)

val coinsMessage = message1<Int>(
    russian = { coins -> coins.wordForNum("монета", "монеты", "монет") },
    english = { coins -> if (coins == 1) "coin" else "coins" }
)

fun String.mixNewYearColors(): String = mapIndexed { index, char ->
    "${if (index % 2 == 0) RED else GREEN}$char"
}.joinToString(separator = "")
