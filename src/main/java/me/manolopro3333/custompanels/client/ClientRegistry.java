package me.manolopro3333.custompanels.client;


import me.manolopro3333.custompanels.client.gui.TestPanelScreen;
import me.manolopro3333.custompanels.init.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientRegistry {

    @SuppressWarnings("removal")
    public static void init(FMLClientSetupEvent event) { // ¡Parámetro añadido!
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.TEST_PANEL.get(), TestPanelScreen::new);
        });
    }


    private static void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.TEST_PANEL.get(), TestPanelScreen::new);
        });
    }



}
