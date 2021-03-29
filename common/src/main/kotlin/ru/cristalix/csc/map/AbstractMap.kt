package ru.cristalix.csc.map

import me.stepbystep.mgapi.common.game.GameType

abstract class AbstractMap {
    abstract val id: Int
    abstract val name: String // TODO: localize
    abstract val gameType: GameType?
}
