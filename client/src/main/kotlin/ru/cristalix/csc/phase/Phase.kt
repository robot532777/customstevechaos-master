package ru.cristalix.csc.phase

import ru.cristalix.csc.game.GameScheduler

abstract class Phase(protected val gameScheduler: GameScheduler) {
    open fun finish() {
        // nothing
    }
}