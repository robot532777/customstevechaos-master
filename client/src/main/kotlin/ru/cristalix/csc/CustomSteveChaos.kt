package ru.cristalix.csc

import com.comphenix.protocol.ProtocolLibrary
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI
import me.stepbystep.api.EntityInteractCanceller
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.getNow
import me.stepbystep.api.menu.MenuListener
import me.stepbystep.api.register
import me.stepbystep.api.registerEntity
import me.stepbystep.mgapi.client.ClientActor
import me.stepbystep.mgapi.client.countdown.BossBarCountdown
import me.stepbystep.mgapi.client.countdown.ChatMessageCountdown
import me.stepbystep.mgapi.client.countdown.EventFiringCountdown
import me.stepbystep.mgapi.client.countdown.plus
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.WorldCreator
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import ru.cristalix.core.CoreApi
import ru.cristalix.core.party.IPartyService
import ru.cristalix.core.party.PartyService
import ru.cristalix.core.realm.IRealmService
import ru.cristalix.csc.command.StartNewGameCommand
import ru.cristalix.csc.command.duel.DuelCommand
import ru.cristalix.csc.entity.*
import ru.cristalix.csc.entity.animal.*
import ru.cristalix.csc.game.GameScheduler
import ru.cristalix.csc.game.GameTypeOptions
import ru.cristalix.csc.game.`class`.SelectedClass
import ru.cristalix.csc.game.customitem.CustomItem
import ru.cristalix.csc.game.customitem.CustomItemListener
import ru.cristalix.csc.game.listener.BeforeGameListener
import ru.cristalix.csc.game.listener.CustomEntitiesListener
import ru.cristalix.csc.game.listener.LivingPlayersInfoListener
import ru.cristalix.csc.game.listener.MinigameListener
import ru.cristalix.csc.game.listener.gameplay.CSCListener
import ru.cristalix.csc.game.listener.gameplay.ClassListener
import ru.cristalix.csc.game.listener.gameplay.TransferItemListener
import ru.cristalix.csc.game.listener.gameplay.TrashCanListener
import ru.cristalix.csc.packet.DuelDataPacket
import ru.cristalix.csc.packet.MapDataPacket
import ru.cristalix.csc.phase.LobbyPhase
import ru.cristalix.csc.player.CSCTeam
import ru.cristalix.csc.util.*
import java.util.*
import java.util.concurrent.CompletableFuture

class CustomSteveChaos : JavaPlugin() {

    private val actor by lazy {
        onSingleEnable()

        val gameTypeHandler = CSCGameTypeHandler()
        val gameType = when (IRealmService.get().currentRealmInfo.realmId.id) {
            in 200..299 -> CSCGameType.Duo
            else -> CSCGameType.Solo
        }
        ClientActor(this, gameTypeHandler, gameType) {
            handleDefaultHandshake()
            registerDefaultListener()
            enableScoreboard(SCOREBOARD_NAME)
            enableQuitItem(Material.MAGMA_CREAM)
            enableCustomNames()
            setupCristalixNetwork("Custom Steve Chaos", "CSC")

            countdownSeconds = when (gameType.minPlayers) {
                1, 2 -> 35 // dev environment
                else -> 70
            }
            secondsWhenFull = 25
            maxCountdownOnJoin = 25
            spawnLocation = Location(Bukkit.getWorld("world"), 269.5, 103.0, -25.5, 0f, 0f)
            countdown = ChatMessageCountdown(this) + BossBarCountdown(this) + EventFiringCountdown()
            isPlayerAlive = {
                it.asCSCPlayerOrNull()?.isAlive == true
            }

            CSCDonateGsonAdapter.init(this)
        }
    }

    val titleHandler = ItemTitleHandler(this)

    override fun onLoad() {
        registerEntities()
    }

    override fun onEnable() {
        actor.setupBukkit()
        Bukkit.createWorld(WorldCreator(DUEL_MAP_WORLD)) // allow it to be deserialized
        Bukkit.clearRecipes()

        val mapFuture = actor.messageTransport.sendPacket(MapDataPacket())
        val duelFuture = actor.messageTransport.sendPacket(DuelDataPacket())

        CompletableFuture.allOf(duelFuture, mapFuture).thenAccept {
            val map = mapFuture.getNow().serverData.map
            val duelMaps = duelFuture.getNow().serverData.duelMaps
            val gameScheduler = GameScheduler(actor, this, duelMaps, map)
            CSCTeam.resetAll(map, gameScheduler)

            UUID.randomUUID().toString()

            MinigameListener(gameScheduler).register(this)
            CSCListener(gameScheduler).register(this)
            CustomEntitiesListener(this).register(this)
            ClassListener(gameScheduler).register(this)
            TrashCanListener(this).register(this)
            BeforeGameListener(gameScheduler).register(this)

            if (gameScheduler.gameTypeOptions != GameTypeOptions.Solo) {
                LivingPlayersInfoListener(gameScheduler).register(this)
            }

            if (gameScheduler.gameTypeOptions != GameTypeOptions.Solo) {
                TransferItemListener(gameScheduler).register(this)
            }
            registerCommands(gameScheduler)
        }.handle { _, t ->
            if (t != null) {
                t.printStackTrace()
                Bukkit.shutdown()
            }
        }

        // TODO: uncomment enchant-related classes
//        ItemEnchant.initPlugin(this)
//        ItemEnchant.ensureUniqueKeys()
        LobbyPhase.initItems(actor)

        registerListeners()
        killAllEntities()

        CitizensAPI.getNPCRegistry().forEach {
            if (!it.isSpawned) {
                it.spawn(it.storedLocation)
            }
        }
    }

    override fun onDisable() {
        killAllEntities()

        ProtocolLibrary.getProtocolManager().removePacketListeners(this)
    }

    private fun registerListeners() {
        MenuListener.startListening(this)
//        CustomEnchantsListener(this).register(this)
        CustomItemListener(this).register(this)
        EntityInteractCanceller(this, CitizensAPI.getNPCRegistry()::isNPC).register(this)
        titleHandler.register(this)

        Quest.VALUES.forEach { it.register(this) }
        CustomItem.VALUES.forEach { it.setup(this) }
        SelectedClass.VALUES.forEach { it.setup(this) }
    }

    private fun registerEntities() {
        registerEntity<CSCEntitySpider>(EntityType.SPIDER)
        registerEntity<CSCEntityCaveSpider>(EntityType.CAVE_SPIDER)
        registerEntity<CSCEntitySkeletonWither>(EntityType.WITHER_SKELETON)
        registerEntity<CSCEntityBlaze>(EntityType.BLAZE)
        registerEntity<CSCEntityPolarBear>(EntityType.POLAR_BEAR)
        registerEntity<CSCEntityWolf>(EntityType.WOLF)
        registerEntity<CSCEntityEndermite>(EntityType.ENDERMITE)
        registerEntity<CSCEntityPigZombie>(EntityType.PIG_ZOMBIE)
        registerEntity<CSCEntityChicken>(EntityType.CHICKEN)
        registerEntity<CSCEntityCow>(EntityType.COW)
        registerEntity<CSCEntityHorse>(EntityType.HORSE)
        registerEntity<CSCEntityPig>(EntityType.PIG)
        registerEntity<CSCEntitySheep>(EntityType.SHEEP)
        registerEntity<CSCEntityIronGolem>(EntityType.IRON_GOLEM)
    }

    private fun registerCommands(gameScheduler: GameScheduler) {
        DuelCommand(gameScheduler)
        StartNewGameCommand(gameScheduler.actor)
    }

    private fun onSingleEnable() {
        val location = Location(Bukkit.getWorld("world"), 299.5, 103.0, -3.5)
        HologramsAPI.createHologram(this, location).let { hologram ->
            listOf(" ".repeat(20), "${GREEN}Топ прошлого сезона", " ".repeat(20)).forEach(hologram::appendTextLine)
        }

        CoreApi.get().registerService(IPartyService::class.java, PartyService(CoreApi.get().socketClient))
    }

    private fun killAllEntities() {
        Bukkit.getWorlds()
            .flatMap { it.entities }
            .filterNot { it is ArmorStand || it is EnderCrystal }
            .filterNot { CitizensAPI.getNPCRegistry().isNPC(it) }
            .forEach { it.remove() }
    }
}
