package me.manolopro3333.custompanels.network;

import me.manolopro3333.custompanels.Custompanels;
import me.manolopro3333.custompanels.actions.ButtonActions;
import me.manolopro3333.custompanels.config.PanelConfigLoader;
import me.manolopro3333.custompanels.executes.ButtonTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TestButtonMessage {
    private final int buttonID;
    private final int x, y, z;
    private final String panelName;
    private final String botonId;
    private final String acciones;

    public TestButtonMessage(int buttonID, int x, int y, int z, String panelName, String botonId, String acciones) {
        this.buttonID = buttonID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.panelName = panelName;
        this.botonId = botonId;
        this.acciones = acciones;
    }

    public TestButtonMessage(FriendlyByteBuf buffer) {
        this.buttonID = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.panelName = buffer.readUtf(Short.MAX_VALUE);
        this.botonId = buffer.readUtf(Short.MAX_VALUE);
        this.acciones = buffer.readUtf(Short.MAX_VALUE);
    }

    public static void buffer(TestButtonMessage msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.buttonID);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        buf.writeInt(msg.z);
        buf.writeUtf(msg.panelName);
        buf.writeUtf(msg.botonId);
        buf.writeUtf(msg.acciones);
    }

    public static void handler(TestButtonMessage message, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            Player player = ctx.getSender();
            if (player == null) return;
            handleButtonAction(player, message);
        });
        ctx.setPacketHandled(true);
    }

    private static void handleButtonAction(Player player, TestButtonMessage message) {
        Level world = player.level();
        Map<String, String> config = PanelConfigLoader.loadConfig(message.panelName, world);
        List<Map<String, String>> actions = PanelConfigLoader.getActions(config, message.botonId);
        MinecraftServer server = player.getServer();

        processActions(player, actions, 0, player.getServer());


    }

    private static void processActions(Player player, List<Map<String, String>> actions, int index, MinecraftServer server) {
        if (index >= actions.size() || server == null) return;

        Map<String, String> action = actions.get(index);
        String type = action.get("TYPE");

        server.execute(() -> { // Ejecutar en el hilo del servidor
            switch (type.toUpperCase()) {
                case "TO_PLAYER_COMMAND" -> {
                    // Usar ServerPlayer en lugar de Player
                    ServerPlayer target = server.getPlayerList().getPlayerByName(action.get("PLAYER"));
                    if (target != null) {
                        ButtonActions.executePlayerCommand(target, action);
                    }
                    processActions(player, actions, index + 1, server);
                }
                case "WAIT_TICKS" -> {
                    try {
                        int ticks = Integer.parseInt(action.getOrDefault("TICKS", "20"));
                        ButtonActions.scheduleDelayedAction(
                                () -> processActions(player, actions, index + 1, server),
                                ticks,
                                server // Añadir server como parámetro
                        );
                    } catch (NumberFormatException e) {
                        Custompanels.LOGGER.error("Ticks inválidos: {}", action.get("TICKS"));
                    }
                }
                case "TO_ALL_COMMAND" -> {
                    String command = action.get("COMMAND");
                    if (command != null) {
                        ButtonActions.executeAllPlayersCommand(server, command);
                    }
                    processActions(player, actions, index + 1, server);
                }
                case "CONSOLE_COMMAND" -> {
                    ButtonActions.executeConsoleCommand(server, action.get("COMMAND"));
                    processActions(player, actions, index + 1, server);
                }
                default -> {
                    Custompanels.LOGGER.warn("Acción no implementada: {}", type);
                    processActions(player, actions, index + 1, server);
                }
            }
        });
    }

    private static void handlePlayerCommand(Player player, Map<String, String> action) {
        ButtonActions.executePlayerCommand(player, action);
    }

    public static void registerMessage(SimpleChannel channel) {
        channel.registerMessage(
                0,
                TestButtonMessage.class,
                TestButtonMessage::buffer,
                TestButtonMessage::new,
                TestButtonMessage::handler
        );
    }
}
