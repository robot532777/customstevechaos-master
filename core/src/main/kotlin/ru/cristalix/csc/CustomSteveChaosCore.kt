package ru.cristalix.csc

import me.stepbystep.api.menu.MenuListener
import me.stepbystep.api.register
import me.stepbystep.api.runDelayed
import me.stepbystep.mgapi.common.ServerInfo
import me.stepbystep.mgapi.common.packet.Packet
import me.stepbystep.mgapi.common.packet.PacketHandler
import me.stepbystep.mgapi.common.packet.type.HandshakePacket
import me.stepbystep.mgapi.common.util.handlePacket
import me.stepbystep.mgapi.common.util.handlePacketWithResponse
import me.stepbystep.mgapi.core.CoreActor
import me.stepbystep.mgapi.lobby.getServerInfo
import me.stepbystep.mgapi.lobby.maxPlayers
import me.stepbystep.mgapi.minecraft.DefaultListener
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.*
import ru.cristalix.csc.command.*
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.db.table.ClassSelectionTable
import ru.cristalix.csc.db.table.DonateTable
import ru.cristalix.csc.db.table.PlayerTable
import ru.cristalix.csc.enchant.ItemEnchant
import ru.cristalix.csc.map.DuelMap
import ru.cristalix.csc.map.GameMap
import ru.cristalix.csc.packet.*
import ru.cristalix.csc.shop.item.CSCCage
import ru.cristalix.csc.shop.item.CSCMessagePack
import ru.cristalix.csc.shop.item.CSCWeaponSkin
import ru.cristalix.csc.util.CSCDonateGsonAdapter
import ru.cristalix.csc.util.DUEL_MAP_WORLD
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.minutes
import kotlin.time.seconds

class CustomSteveChaosCore : JavaPlugin() {
    override fun onEnable() {
        val actor = CoreActor(this, CSCGameTypeHandler(), 8.seconds) {
            handleDefaultHandshake()
            scheduleMidnightRestart()
            enableBanController(3.minutes)
            enableCustomNames()

            CSCDonateGsonAdapter.init(this)
        }

        Bukkit.createWorld(WorldCreator(DUEL_MAP_WORLD)) // allow it to be deserialized

        val dataBase = DataBase(this)
        val ratingUpdater = PlayerRatingUpdater(dataBase).start(this)
        val items = dataBase.selectItems().toMutableList()
        val maps = dataBase.selectMaps().toMutableList()
        val duelMaps = dataBase.selectDuelMaps().toMutableList()

        ItemEnchant.initPlugin(this)
        ItemEnchant.ensureUniqueKeys()

        handlePackets(actor, items, maps, duelMaps, dataBase, ratingUpdater)

        MenuListener.startListening(this)
        DefaultListener().register(this)
        CoreListener(dataBase).register(this)

        registerCommands(dataBase, actor, maps, items, duelMaps)
    }

    private fun registerCommands(
        dataBase: DataBase,
        actor: CoreActor,
        maps: MutableList<GameMap>,
        items: MutableList<CSCItemStack>,
        duelMaps: MutableList<DuelMap>,
    ) {
        MapCommand(dataBase, maps, actor.gameTypeHandler.maxPlayers)
        ItemCommand(dataBase, items)
        DuelMapCommand(dataBase, duelMaps)
        SaveWorldsCommand()
        ItemEnchantCommand()
        BigEnchantCommand()
        CheckTextCommand(this)
        TpWorldCommand()
        BroadcastMessageCommand(actor)
        SetSaleCommand(actor, this)
        ClassStatisticsCommand(dataBase)
        MakeFrozenItemCommand()
    }

    private fun handlePackets(
        actor: CoreActor,
        items: List<CSCItemStack>,
        maps: List<GameMap>,
        duelMaps: List<DuelMap>,
        database: DataBase,
        ratingUpdater: PlayerRatingUpdater,
    ) {
        actor.packetHandler.handlePacketWithResponse<MapDataPacket> { _, serverID ->
            val serverInfo = actor.getServerInfo<ServerInfo.Game>(serverID)
            val map = maps.filter { it.gameType == serverInfo.gameType }.random()
            val packetData = MapDataPacket.ServerData(map)
            CompletableFuture.completedFuture(MapDataPacket(packetData))
        }

        actor.packetHandler.handlePacketWithResponse<ItemDataPacket> { _, _ ->
            val packetData = ItemDataPacket.ServerData(items)
            CompletableFuture.completedFuture(ItemDataPacket(packetData))
        }

        actor.packetHandler.handlePacketWithResponse<DuelDataPacket> { _, serverID ->
            val serverInfo = actor.getServerInfo<ServerInfo.Game>(serverID)
            val applicableMaps = duelMaps.filter { it.gameType == null || it.gameType == serverInfo.gameType }
            val packetData = DuelDataPacket.ServerData(applicableMaps)
            CompletableFuture.completedFuture(DuelDataPacket(packetData))
        }

        actor.packetHandler.handlePacket<IncrementGameCountPacket> { packet, _ ->
            val playerUUID = packet.clientData.playerUUID
            database.asyncTransaction {
                PlayerTable.update(where = { PlayerTable.uuid eq playerUUID }) {
                    with(SqlExpressionBuilder) {
                        it.update(totalGames, totalGames + 1)
                    }
                }
            }
        }

        actor.packetHandler.handlePacketWithResponse<GetDonateStatusPacket> { packet, _ ->
            val future = CompletableFuture<GetDonateStatusPacket>()
            database.asyncTransaction {
                val data = packet.clientData
                val hasDonate = with(DonateTable) { hasDonate(data.playerUUID, data.donate) }
                val responsePacket = GetDonateStatusPacket(GetDonateStatusPacket.ServerData(hasDonate))
                future.complete(responsePacket)
            }
            future
        }

        actor.packetHandler.handlePacketWithResponse<GetMaxDonateStatusPacket> { packet, _ ->
            val future = CompletableFuture<GetMaxDonateStatusPacket>()
            database.asyncTransaction {
                val data = packet.clientData
                val maxDonate = data.donate.donates.lastOrNull {
                    with(DonateTable) { hasDonate(data.playerUUID, it) }
                }
                val responsePacket = GetMaxDonateStatusPacket(GetMaxDonateStatusPacket.ServerData(maxDonate))
                future.complete(responsePacket)
            }
            future
        }

        actor.packetHandler.handlePacket<ChangeMoneyPacket> { packet, _ ->
            database.asyncTransaction {
                val playerUUID = packet.clientData.playerUUID
                val difference = packet.clientData.difference
                if (difference == 0) return@asyncTransaction

                PlayerTable.update(where = { PlayerTable.uuid eq playerUUID }) {
                    with(SqlExpressionBuilder) {
                        it.update(balance, balance + difference)
                    }
                }
            }
        }

        actor.packetHandler.handlePacket<ChangeRatingPacket> { packet, serverID ->
            val serverInfo = actor.getServerInfo<ServerInfo.Game>(serverID)
            val column = when (val gameType = serverInfo.gameType) {
                CSCGameType.Solo -> PlayerTable.soloRating
                CSCGameType.Duo -> PlayerTable.duoRating
                else -> error("Unknown gameType: $gameType")
            }

            database.asyncTransaction {
                val clientData = packet.clientData
                modifyColumnForPlayer(clientData.playerUUID, column) {
                    (it + clientData.amount).coerceAtLeast(0)
                }
            }
        }

        actor.packetHandler.handleGetSelectedItemPacket(
            database = database,
            getPlayerUUID = { clientData.playerUUID },
            getItem = { it[PlayerTable.selectedCage] },
            createResponse = { GetSelectedCagePacket(GetSelectedCagePacket.ServerData(it)) },
            defaultItem = CSCCage.Default,
        )

        actor.packetHandler.handleGetSelectedItemPacket(
            database = database,
            getPlayerUUID = { clientData.playerUUID },
            getItem = { it[PlayerTable.selectedMessagePack] },
            createResponse = { GetSelectedMessagePackPacket(GetSelectedMessagePackPacket.ServerData(it)) },
            defaultItem = CSCMessagePack.Default,
        )

        actor.packetHandler.handleGetSelectedItemPacket(
            database = database,
            getPlayerUUID = { clientData.playerUUID },
            getItem = { it[PlayerTable.selectedWeaponSkin] },
            createResponse = { GetSelectedWeaponSkinPacket(GetSelectedWeaponSkinPacket.ServerData(it)) },
            defaultItem = CSCWeaponSkin.Default,
        )

        actor.packetHandler.handlePacket<HandshakePacket> { _, serverID ->
            runDelayed(5) { // make sure everything is initialized
                if (actor.messageTransport.getServerInfo(serverID) is ServerInfo.Lobby) {
                    actor.messageTransport.sendPacket(serverID, createSalePacket())
                }
            }
        }

        actor.packetHandler.handlePacketWithResponse<GetPlayerPlacePacket> { packet, _ ->
            val (playerUUID, gameType) = packet.clientData
            val place = ratingUpdater.getPlayerPlace(gameType, playerUUID)
            val response = GetPlayerPlacePacket(GetPlayerPlacePacket.ServerData(place))
            CompletableFuture.completedFuture(response)
        }

        actor.packetHandler.handlePacket<RecordPlayerClassPacket> { packet, _ ->
            database.asyncTransaction {
                ClassSelectionTable.insert {
                    it[selectedClass] = packet.clientData.selectedClass
                    it[allClasses] = packet.clientData.allClasses.joinToString(separator = ";")
                }
            }
        }

        actor.packetHandler.handlePacket<RecordPlayerWavePacket> { packet, _ ->
            val playerUUID = packet.clientData.playerUUID
            val wave = packet.clientData.wave

            database.asyncTransaction {
                PlayerTable.update(where = { (PlayerTable.uuid eq playerUUID) and (PlayerTable.maxWave less wave) }) {
                    it[maxWave] = wave
                }
            }
        }
    }

    fun createSalePacket(): SetSalePacket {
        val sale = config.getInt(SetSaleCommand.CONFIG_KEY)
        return SetSalePacket(SetSalePacket.ServerData(sale))
    }

    private inline fun <reified P : Packet<*, *>, T> PacketHandler.handleGetSelectedItemPacket(
        database: DataBase,
        crossinline getPlayerUUID: P.() -> UUID,
        crossinline getItem: (ResultRow) -> T,
        crossinline createResponse: (T) -> P,
        defaultItem: T,
    ) {
        handlePacketWithResponse<P> { packet, _ ->
            val future = CompletableFuture<P>()

            database.asyncTransaction {
                val query = PlayerTable.select { PlayerTable.uuid eq packet.getPlayerUUID() }
                val item = when {
                    query.empty() -> defaultItem // In case player never joined lobby -> doesn't have account
                    else -> getItem(query.first())
                }
                val responsePacket = createResponse(item)

                future.complete(responsePacket)
            }

            future
        }
    }
}
