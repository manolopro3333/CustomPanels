package me.manolopro3333.custompanels.executes;

import me.manolopro3333.custompanels.world.inventory.TestPanelMenu;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import io.netty.buffer.Unpooled;

public class AbrirGuiTest {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, String panelName) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        BlockPos bpos = BlockPos.containing(x, y, z);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(bpos); // Usar writeBlockPos en lugar de writeInt
        buf.writeUtf(panelName);

        NetworkHooks.openScreen(
                serverPlayer,
                new MenuProvider() {
                    @Override public Component getDisplayName() {
                        return Component.literal("Panel: " + panelName);
                    }
                    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new TestPanelMenu(id, inv, buf);
                    }
                },
                (buffer) -> {
                    buffer.writeBlockPos(bpos);
                    buffer.writeUtf(panelName);
                }
        );
    }
}