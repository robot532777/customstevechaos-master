package ru.cristalix.csc

import me.stepbystep.api.item.NMSItemStack
import me.stepbystep.api.item.NullableUUIDTag
import java.util.*

const val UUID_TAG = "uuid"

data class CSCItemStack(
    val previous: UUID?,
    val stack: NMSItemStack,
    val price: Int,
    val upgrades: MutableList<CSCItemStack>,
)

val CSCItemStack.uuid get() = stack.uuid

var NMSItemStack.uuid by NullableUUIDTag(UUID_TAG)
