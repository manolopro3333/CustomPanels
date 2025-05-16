package me.manolopro3333.custompanels.client.gui;

import me.manolopro3333.custompanels.config.PanelConfigLoader;
import me.manolopro3333.custompanels.network.TestButtonMessage;
import me.manolopro3333.custompanels.world.inventory.TestPanelMenu;
import me.manolopro3333.custompanels.Custompanels;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TestPanelScreen extends AbstractContainerScreen<TestPanelMenu> {
    private final Level world;
    private final int x, y, z;
    private final String panelName;
    private final Map<String, String> configData;

    public TestPanelScreen(TestPanelMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.panelName = container.panelName;
        this.configData = PanelConfigLoader.loadConfig(panelName, container.world);
        // Tamaño base o resolución personalizada
        String res = configData.getOrDefault("Main.resolution", "176x166");
        try {
            String[] parts = res.split("x");
            this.imageWidth = Integer.parseInt(parts[0]);
            this.imageHeight = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            this.imageWidth = 176;
            this.imageHeight = 166;
        }


    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float pt) {
        this.renderBackground(gg);
        super.render(gg, mouseX, mouseY, pt);
        this.renderTooltip(gg, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();


        int transparencia;

        try {
            String rawValue = configData.getOrDefault("Main.BGTransparency", "35");

            transparencia = Math.min(100, Math.max(0, Integer.parseInt(rawValue)));

        } catch (NumberFormatException e) {
            transparencia = 35;
        }

        // 5. Calcular alpha (0 = transparente, 255 = opaco)
        int alpha = (int) (transparencia / 100.0 * 255);
        int argb = (alpha << 24); // Formato ARGB (Alpha en bits 24-31)

        // 6. Aplicar fondo
        guiGraphics.fillGradient(0, 0, this.width, this.height, argb, argb);
        RenderSystem.disableBlend();
    }

    @Override
    @SuppressWarnings("removal")
    protected void renderBg(GuiGraphics gg, float pt, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.blit(new ResourceLocation("custompanels:textures/screens/gui.png"),
                this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mx, int my) {
        String title = PanelConfigLoader.getValue(configData, "Main.display_name");
        if (title == null || title.isEmpty()) {
            title = "Panel: " + panelName;
            Custompanels.LOGGER.error("Clave 'Main.display_name' no encontrada. Usando nombre por defecto.", panelName);
        }

        // Calcular posiciones porcentuales (valores por defecto: 50% = centro)
        int titleX = PanelConfigLoader.getPercentageValue(configData, "Main.title_x", 50, this.imageWidth);
        int titleY = PanelConfigLoader.getPercentageValue(configData, "Main.title_y", 50, this.imageHeight);


        titleX -= this.font.width(title) / 2;

        gg.drawString(
                this.font,
                title,
                titleX,
                titleY,
                0xFFFFFF,
                false
        );
    }

    @Override
    public void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        System.out.println("[DEBUG] GUI posicionada en: ("
                + leftPos + ", " + topPos + ")");

        createDebugButtons(); // ← ahora carga los de DEBUG
    }

    private void createButtons() {
        int buttonY = 30;
        // Buscar todas las claves que terminen en ".display"
        for (String key : configData.keySet()) {
            if (key.endsWith(".display")) {
                String buttonPath = key.substring(0, key.lastIndexOf(".display"));
                String displayText = configData.get(key);
                String actions = configData.getOrDefault(buttonPath + ".Actions", "");

                // Extraer el ID del botón (última parte de la ruta)
                String buttonId = buttonPath.substring(buttonPath.lastIndexOf(".") + 1);

                this.addRenderableWidget(
                        Button.builder(Component.literal(displayText), btn -> {
                                    Custompanels.PACKET_HANDLER.sendToServer(
                                            new TestButtonMessage(0, x, y, z, panelName, buttonId, actions)
                                    );
                                })
                                .bounds(this.leftPos + 50, this.topPos + buttonY, 100, 20)
                                .build()
                );

                buttonY += 24;
            }
        }
    }

    private void createDebugButtons() {
        int buttonY = 30;
        for (Map.Entry<String, String> entry : configData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("DEBUG.") && key.endsWith(".display")) {
                String id = key.substring("DEBUG.".length(), key.length() - ".display".length());
                String display = entry.getValue();
                String actions = configData.getOrDefault("DEBUG." + id + ".Actions", "BAD_OMEN");

                final int currentY = buttonY;

                this.addRenderableWidget(
                        Button.builder(Component.literal(display), btn -> {
                                    Custompanels.PACKET_HANDLER.sendToServer(
                                            new TestButtonMessage(0, x, y, z, panelName, id, actions)
                                    );
                                })
                                .bounds(this.leftPos + 50, this.topPos + currentY, 100, 20)
                                .build()
                );

                buttonY += 24;
            }
        }
    }
}
