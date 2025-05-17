package me.manolopro3333.custompanels.init;

import me.manolopro3333.custompanels.world.inventory.EditPanelMenu;
import me.manolopro3333.custompanels.world.inventory.TestPanelMenu;

import me.manolopro3333.custompanels.Custompanels;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Custompanels.MODID);

    public static final RegistryObject<MenuType<TestPanelMenu>> TEST_PANEL =
            REGISTRY.register("test_panel", () ->
                    IForgeMenuType.create((id, inv, buf) ->
                            new TestPanelMenu(id, inv, buf)
                    )
            );

    public static final RegistryObject<MenuType<EditPanelMenu>> EDIT_PANEL =
            REGISTRY.register("edit_panel", () ->
                    IForgeMenuType.create((id, inv, buf) ->
                            new EditPanelMenu(id, inv, buf)
                    )
            );

}