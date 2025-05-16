package me.manolopro3333.custompanels.world.inventory;

import me.manolopro3333.custompanels.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import java.util.HashMap;

public class TestPanelMenu extends AbstractContainerMenu {
    public final HashMap<String, Object> guistate = new HashMap<>();
    public final Level world;
    public final int x, y, z;
    public final Player entity;
    public final String panelName;
    public boolean isEditMode = false;

    public TestPanelMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenus.TEST_PANEL.get(), id);
        this.entity = inv.player;
        this.world = entity.level();

        // Leer en el orden correcto
        this.isEditMode = buf.readBoolean();
        BlockPos pos = buf.readBlockPos();
        this.panelName = buf.readUtf(Short.MAX_VALUE);

        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }



    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}