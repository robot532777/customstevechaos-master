package ru.cristalix.csc

import me.stepbystep.api.chat.message0
import me.stepbystep.mgapi.common.game.AbstractGameType

sealed class CSCGameType(id: String) : AbstractGameType(id) {
    final override val maxPlayers = 16

    object Solo : CSCGameType("cscsolo") {
        override val minPlayers = 10

        override val displayName = message0(
            russian = "Одиночный",
            english = "Solo",
        )
    }

    object Duo : CSCGameType("cscduo") {
        override val minPlayers = 12

        override val displayName = message0(
            russian = "Двойной",
            english = "Duo",
        )
    }

    companion object {
        fun fromString(name: String): CSCGameType = when (name.toLowerCase()) {
            "solo" -> Solo
            "duo" -> Duo
            else -> error("Unknown CSCGameType: $name")
        }
    }
}
