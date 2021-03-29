package ru.cristalix.csc.game

enum class DuelStage {
    BeforeStart,
    AboutToStart,
    Active,
    Finished;

    val isStarted: Boolean
        get() = this == Active || this == Finished
}
