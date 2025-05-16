package me.manolopro3333.custompanels.network;

import me.manolopro3333.custompanels.executes.ButtonTest;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.simple.SimpleChannel;

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
        if (message.buttonID == 0) {
            String[] efectos = message.acciones.split(",\\s*");
            for (String efecto : efectos) {
                if (!efecto.isEmpty()) {
                    ButtonTest.execute(world, efecto.trim());
                }
            }
        }
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
