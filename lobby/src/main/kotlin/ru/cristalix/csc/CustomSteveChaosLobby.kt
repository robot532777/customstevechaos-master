package ru.cristalix.csc

import me.stepbystep.api.chat.GOLD
import me.stepbystep.api.chat.GREEN
import me.stepbystep.api.chat.RED
import me.stepbystep.api.chat.message0
import me.stepbystep.api.getOrCreateChatView
import me.stepbystep.api.item.ItemTag
import me.stepbystep.api.item.createNMSItem
import me.stepbystep.api.menu.MenuListener
import me.stepbystep.api.register
import me.stepbystep.mgapi.common.packet.PacketHandler
import me.stepbystep.mgapi.common.util.handlePacket
import me.stepbystep.mgapi.lobby.LobbyActor
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.server.v1_12_R1.NBTTagString
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import ru.cristalix.core.chat.ChatTextComponent
import ru.cristalix.core.chat.IChatService
import ru.cristalix.core.text.TextFormat
import ru.cristalix.csc.command.BroadcastMessageCommand
import ru.cristalix.csc.command.SaveWorldsCommand
import ru.cristalix.csc.db.DataBase
import ru.cristalix.csc.listener.*
import ru.cristalix.csc.lootbox.LootBoxListener
import ru.cristalix.csc.packet.GetPlayerPlacePacket
import ru.cristalix.csc.packet.SetSalePacket
import ru.cristalix.csc.runnable.UpdateGameHologramsRunnable
import ru.cristalix.csc.util.CSCDonateGsonAdapter

class CustomSteveChaosLobby : JavaPlugin() {
    var salePercent = 0

    override fun onEnable() {
        val actor = LobbyActor(this, CSCGameTypeHandler()) {
            handleDefaultHandshake()
            registerNpcListener()
            registerDefaultListener()
            enableGameHolograms()
            enableCustomNames()
            enableJoinQueueItem(
                getDisplayName = message0(
                    russian = "${GREEN}Войти в очередь",
                    english = "${GREEN}Join the queue",
                )
            ) {
                createNMSItem(
                    material = Material.CLAY_BALL,
                    displayName = when (it as CSCGameType) {
                        CSCGameType.Solo -> "${GREEN}Одиночный режим"
                        CSCGameType.Duo -> "${GREEN}Двойной режим"
                    },
                    customTag = ItemTag(
                        key = "other",
                        value = NBTTagString(
                            when (it) {
                                CSCGameType.Solo -> "guild_world"
                                CSCGameType.Duo -> "guild_members"
                            }
                        )
                    )
                )
            }
            enableHubItem(
                getDisplayName = message0(
                    russian = "${RED}Выход в хаб",
                    english = "${RED}Go to hub",
                ),
            )
            setupCristalixNetwork("Custom Steve Chaos", "CSC")
            spawnLocation = spawnLocation.clone().apply {
                yaw = 0f
                pitch = 0f
            }

            CSCDonateGsonAdapter.init(this)
        }

        actor.packetHandler.handlePackets()

        val dataBase = DataBase(this)

        registerChatExtensions(actor)
        registerListeners(actor, dataBase)

        BroadcastMessageCommand(actor)
        SaveWorldsCommand()

        UpdateGameHologramsRunnable().start(this)
    }

    private fun PacketHandler.handlePackets() {
        handlePacket<SetSalePacket> { packet, _ ->
            salePercent = packet.serverData.salePercent
        }
    }

    private fun registerListeners(actor: LobbyActor, dataBase: DataBase) {
        CSCTopsHandler(this, dataBase)
        MenuListener.startListening(this)
//        CustomEnchantsListener(this).register(this)
        LobbyListener(this, dataBase).register(this)
        DonateListener(this)
        ClassesInfoListener(this)
        PerksListener(this)
        PlayerStatisticsListener(actor)
        LootBoxListener(this, actor).register(this)
    }

    private fun registerChatExtensions(actor: LobbyActor) {
        val suffix = ChatTextComponent(50, TextFormat.NONE, { _, _ -> true }, { playerUUID ->
            val packet = GetPlayerPlacePacket(GetPlayerPlacePacket.ClientData(playerUUID, CSCGameType.Solo))
            actor.messageTransport.sendPacket(packet).thenApply { response ->
                val place = response.serverData.place
                TextComponent.fromLegacyText("$GOLD(#$place)")
            }
        })

        IChatService.get().getOrCreateChatView().addSuffix(suffix)
    }
}
