package me.manolopro3333.custompanels.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import me.manolopro3333.custompanels.Custompanels;
import me.manolopro3333.custompanels.config.PanelConfigLoader;
import me.manolopro3333.custompanels.network.TestButtonMessage;
import me.manolopro3333.custompanels.world.inventory.EditPanelMenu;
import me.manolopro3333.custompanels.world.inventory.TestPanelMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestPanelScreen extends AbstractContainerScreen<TestPanelMenu> {
    private final Level world;
    private final int x, y, z;
    private final String panelName;
    private Map<String, String> configData;

    private int currentPage = 0;
    private int totalPages = 0;

    private static final float BUTTON_AREA_TOP_RATIO = 0.15f;
    private static final float BUTTON_AREA_SIDE_RATIO = 0.05f;
    private static final float PAGINATOR_Y_RATIO = 0.95f;
    private static final float PAGINATOR_X_MARGIN_RATIO = 0.05f;

    public TestPanelScreen(TestPanelMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.panelName = container.panelName;
        reloadConfig();
        setSizeFromConfig();
    }

    private void reloadConfig() {
        this.configData = PanelConfigLoader.loadConfig(panelName, world);
    }

    private void setSizeFromConfig() {
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
    public void init() {
        super.init();
        reloadConfig();
        clearWidgets();

        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;

        if (menu.isEditMode) {
            addEditControls();
        }
        createButtons();
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);
    }

    @Override
    @SuppressWarnings("removal")
    protected void renderBg(GuiGraphics gg, float pt, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.blit(new ResourceLocation(
                        configData.getOrDefault("Main.Texture", "custompanels:textures/screens/gui.png")
                ),
                leftPos, topPos, 0, 0,
                imageWidth, imageHeight,
                imageWidth, imageHeight
        );
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mx, int my) {
        String title = PanelConfigLoader.getValue(configData, "Main.display_name");
        if (title == null || title.isEmpty()) {
            title = "Panel: " + panelName;
            Custompanels.LOGGER.error("Clave 'Main.display_name' no encontrada. Usando nombre por defecto.", panelName);
        }


        int titleX = PanelConfigLoader.getPercentageValue(configData, "Main.title_x", 50, this.imageWidth);
        int titleY = PanelConfigLoader.getPercentageValue(configData, "Main.title_y", 50, this.imageHeight);
        titleX -= this.font.width(title) / 2;
        gg.drawString(this.font, title, titleX, titleY, 0xFFFFFF, false);
    }

    private void createButtons() {
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        for (String key : configData.keySet()) {
            if (key.endsWith(".display")) {
                entries.add(new AbstractMap.SimpleEntry<>(key, configData.get(key)));
            }
        }

        // 1. Obtener dimensiones desde config
        int buttonWidth = PanelConfigLoader.getButtonWidth(configData, imageWidth);
        int buttonHeight = PanelConfigLoader.getButtonHeight(configData, imageHeight);

        // 2. Calcular márgenes
        int horizontalMargin = (int)(imageWidth * 0.05);
        int verticalMargin = (int)(imageHeight * 0.15);

        // 3. Calcular espacio disponible
        int espacioUtilX = imageWidth - 2 * horizontalMargin;
        int espacioUtilY = imageHeight - verticalMargin - 50; // Espacio para controles

        // 4. Determinar grid
        int maxColumnas = Math.max(1, espacioUtilX / (buttonWidth + 10));
        int maxFilas = Math.max(1, espacioUtilY / (buttonHeight + 5));
        int botonesPorPagina = maxColumnas * maxFilas;

        // 5. Paginación
        totalPages = (int) Math.ceil((double) entries.size() / botonesPorPagina);
        int start = currentPage * botonesPorPagina;
        int end = Math.min(start + botonesPorPagina, entries.size());

        // 6. Posicionamiento
        int xStart = leftPos + horizontalMargin;
        int yStart = topPos + verticalMargin;
        int currentCol = 0;
        int currentRow = 0;

        for (int i = start; i < end; i++) {
            Map.Entry<String, String> e = entries.get(i);
            String path = e.getKey().substring(0, e.getKey().lastIndexOf(".display"));
            String text = e.getValue();
            String actions = configData.getOrDefault(path + ".Actions", "");
            String id = path.substring(path.lastIndexOf('.') + 1);

            int x = xStart + currentCol * (buttonWidth + 10);
            int y = yStart + currentRow * (buttonHeight + 5);
            addButton(x, y, buttonWidth, buttonHeight, text, id, actions);

            // Actualizar posición
            currentCol++;
            if (currentCol >= maxColumnas) {
                currentCol = 0;
                currentRow++;
                if (currentRow >= maxFilas) break; // Evitar desborde vertical
            }
        }
        addPaginationControls();
    }

    private void addButton(int x, int y, int width, int height, String text, String id, String actions) {
        Button.Builder builder = Button.builder(Component.literal(text), btn -> {
            if (menu.isEditMode) {
                // Cerrar pantalla actual
                this.minecraft.setScreen(null);

                // Crear buffer con TODOS los datos necesarios
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeBoolean(true); // isEditMode
                buf.writeBlockPos(new BlockPos(menu.x, menu.y, menu.z));
                buf.writeUtf(menu.panelName);
                buf.writeUtf(id); // ID del botón (¡CRUCIAL!)

                // Crear menú de edición con referencia al original
                EditPanelMenu editMenu = new EditPanelMenu(0, minecraft.player.getInventory(), buf);
                TestPanelMenu originalMenu = this.menu;

                // Crear pantalla de edición
                this.minecraft.setScreen(new EditButtonScreen(
                        editMenu,
                        minecraft.player.getInventory(),
                        Component.literal("Editando: " + id),
                        originalMenu // Pasar referencia del menú padre
                ));
            } else {
                Custompanels.PACKET_HANDLER.sendToServer(
                        new TestButtonMessage(0, this.x, this.y, this.z, panelName, id, actions)
                );
            }
        });

        this.addRenderableWidget(builder.bounds(x, y, width, height).build());
    }

    private void addPaginationControls() {
        if (totalPages > 1) {
            int pageY = topPos + (int) (imageHeight * PAGINATOR_Y_RATIO);
            int leftX = leftPos + (int) (imageWidth * PAGINATOR_X_MARGIN_RATIO);
            int rightX = leftPos + imageWidth - (int) (imageWidth * PAGINATOR_X_MARGIN_RATIO) - 20;

            int yPos = topPos + imageHeight - 30;

            addPaginator(leftPos + 20, yPos, "<", () -> {
                currentPage = Math.max(0, currentPage - 1);
                init();
            });
            // Indicador centrado
            this.addRenderableWidget(
                    new StaticTextWidget(
                            leftPos + (imageWidth / 2) - font.width("X/Y") / 2,
                            yPos,
                            Component.literal((currentPage + 1) + "/" + totalPages),
                            font
                    )
            );

            // Botón siguiente
            addPaginator(leftPos + imageWidth - 40, yPos, ">", () -> {
                currentPage = Math.min(totalPages - 1, currentPage + 1);
                init();
            });
        }
    }

    private void addPaginator(int x, int y, String txt, Runnable action) {
        this.addRenderableWidget(
                Button.builder(Component.literal(txt), btn -> action.run())
                        .bounds(x, y, 20, 20)
                        .build()
        );
    }

    private void addEditControls() {
        int x = leftPos + (int) (imageWidth * BUTTON_AREA_SIDE_RATIO);
        int y = topPos + (int) (imageHeight * BUTTON_AREA_TOP_RATIO / 2);
        this.addRenderableWidget(
                Button.builder(Component.literal("+"), btn -> {
                            addElement();
                            currentPage = 0;
                            this.init();
                        })
                        .bounds(x, y, 20, 20)
                        .build()
        );
    }

    private void addElement() {
        String newId = "boton_" + System.currentTimeMillis();
        configData.put("Buttons." + newId + ".display", "Nuevo botón");
        configData.put("Buttons." + newId + ".Actions", "EJEMPLO_EFECTO");
        PanelConfigLoader.saveConfig(panelName, world, configData);
    }

    private static class StaticTextWidget extends AbstractWidget {
        private final Component text;
        private final Font font;

        public StaticTextWidget(int x, int y, Component text, Font font) {
            super(x, y, font.width(text), font.lineHeight, text);
            this.text = text;
            this.font = font;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float pt) {
            guiGraphics.drawString(font, text, getX(), getY(), 0xFFFFFF, false);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}