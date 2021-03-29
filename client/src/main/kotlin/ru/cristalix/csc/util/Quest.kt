package ru.cristalix.csc.util

import me.stepbystep.api.asBukkit
import me.stepbystep.api.asNMS
import me.stepbystep.api.registerServiceIfAbsent
import me.stepbystep.api.wordForNum
import net.minecraft.server.v1_12_R1.EnchantmentManager
import net.minecraft.server.v1_12_R1.Enchantments
import net.minecraft.server.v1_12_R1.Items
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.cristalix.core.CoreApi
import ru.cristalix.core.quest.IQuestService
import ru.cristalix.core.quest.QuestService
import ru.cristalix.csc.event.DuelSuccessfullyCompleteEvent
import ru.cristalix.csc.event.PlayerEnchantItemEvent
import ru.cristalix.csc.event.PlayerSelectRandomItemEvent
import ru.cristalix.csc.event.WaveStatusEvent
import ru.cristalix.csc.game.Wave
import ru.cristalix.csc.player.CSCPlayer
import java.util.*

sealed class Quest(
    id: String,
    name: String,
    expReward: Int,
) : Listener {
    companion object {
        private const val EASY_QUEST_REWARD = 2500
        private const val HARD_QUEST_REWARD = 15000

        // init block should be before VALUES initialization
        init {
            registerServiceIfAbsent {
                QuestService(CoreApi.get().platform, CoreApi.get().socketClient, "CSC")
            }
        }

        val VALUES = Quest::class.nestedClasses.mapNotNull { it.objectInstance as? Quest }
    }

    private val quest = IQuestService.get().makeQuest("CSC-$id", IQuestService.experienceCallback(name, expReward))

    protected fun CSCPlayer.addProgress(amount: Int = 1) {
        println("Adding progress at ${quest.questId} for $name: +$amount")
        quest.progress(uuid, amount.toLong())
    }

    protected fun Player.addProgress(amount: Int = 1) {
        println("Adding progress at ${quest.questId} for $name: +$amount")
        quest.progress(uniqueId, amount.toLong())
    }

    protected fun addProgressToLivingPlayers() {
        CSCPlayer.getLivingPlayers().forEach {
            it.addProgress()
        }
    }

    open fun clearData() {
        // nothing
    }

    open class StartWaveQuest(
        id: String,
        private val requiredDisplayWaveIndex: Int,
        expReward: Int,
    ) : Quest(id, "Дойти до $requiredDisplayWaveIndex волны", expReward) {

        @EventHandler
        fun WaveStatusEvent.Start.handle() {
            // +1 because SpawnMobsPhase is not actually started
            if (displayWaveIndex + 1 != requiredDisplayWaveIndex) return

            addProgressToLivingPlayers()
        }
    }

    open class CollectItemsQuest(
        id: String,
        name: String,
        expReward: Int,
        private vararg val materials: Material,
    ) : Quest(id, name, expReward) {

        @EventHandler
        fun PlayerSelectRandomItemEvent.handle() {
            if (player.containsAllMaterials()) {
                player.addProgress()
            }
        }

        private fun Player.containsAllMaterials(): Boolean {
            val leftMaterials = materials.toHashSet()
            val nmsInventory = inventory.asNMS()

            for (stack in nmsInventory.items + nmsInventory.armor) {
                val item = stack.item ?: continue
                leftMaterials -= item.asBukkit()

                if (leftMaterials.isEmpty()) {
                    return true
                }
            }

            return false
        }
    }

    open class MakeWinnerBetsQuest(
        id: String,
        private val amount: Int,
        expReward: Int,
    ) : Quest(
        id = id,
        name = "Сделать $amount победных ${amount.wordForNum("ставка", "ставки", "ставок")} за игру",
        expReward = expReward,
    ) {
        private val betsCount = hashMapOf<UUID, Int>()

        @EventHandler
        fun DuelSuccessfullyCompleteEvent.handle() {
            winner.bets.forEach { (uuid, _) ->
                val oldBets = betsCount[uuid] ?: 0
                val newBets = oldBets + 1
                betsCount[uuid] = newBets

                if (newBets == amount) {
                    CSCPlayer.getOrNull(uuid)?.addProgress()
                }
            }
        }

        override fun clearData() {
            betsCount.clear()
        }
    }

    object Start10WaveQuest : StartWaveQuest("D-0", 10, EASY_QUEST_REWARD)
    object Start20WaveQuest : StartWaveQuest("W-2", 20, HARD_QUEST_REWARD)

    object WinDuelQuest : Quest("D-1", "Выиграть дуэль", EASY_QUEST_REWARD) {
        @EventHandler
        fun DuelSuccessfullyCompleteEvent.handle() {
            winner.team.livingPlayers.forEach { it.addProgress() }
        }
    }

    object Make3WinnerBetsQuest : MakeWinnerBetsQuest("D-2", 3, EASY_QUEST_REWARD)
    object Make6WinnerBetsQuest : MakeWinnerBetsQuest("W-3", 6, HARD_QUEST_REWARD)

    object GetMixWaveQuest : Quest("D-3", "Попасть на волну \"Микс\"", EASY_QUEST_REWARD) {
        @EventHandler
        fun WaveStatusEvent.Start.handle() {
            if (wave != Wave.Mixed) return

            addProgressToLivingPlayers()
        }
    }

    object CollectLeatherSetQuest : CollectItemsQuest(
        id ="D-4",
        name = "Собрать кожаный сет",
        expReward = EASY_QUEST_REWARD,
        Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
    )

    object CollectDiamondSetQuest : CollectItemsQuest(
        id = "W-0",
        name = "Собрать алмазный сет",
        expReward = HARD_QUEST_REWARD,
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
    )

    object EnchantDiamondSwordQuest : Quest("W-1", "Улучшить алмазный меч до остроты 5", HARD_QUEST_REWARD) {
        @EventHandler
        fun PlayerEnchantItemEvent.handle() {
            if (newItem.item != Items.DIAMOND_SWORD) return
            if (EnchantmentManager.getEnchantmentLevel(Enchantments.DAMAGE_ALL, newItem) != 5) return

            player.addProgress()
        }
    }
}
