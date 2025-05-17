package me.manolopro3333.custompanels.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Mod.EventBusSubscriber
public class CrearPanelCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("panel_crear")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("nombre", StringArgumentType.word())
                                .executes(ctx -> crearPanel(
                                        StringArgumentType.getString(ctx, "nombre"),
                                        ctx.getSource()
                                ))
                        )
        );
    }


    private static int crearPanel(String nombre, CommandSourceStack src) {
        // Obtener la ruta correcta
        File PANEL_DIR = getPanelDirectory(src.getLevel());

        if (!PANEL_DIR.exists()) PANEL_DIR.mkdirs();
        File file = new File(PANEL_DIR, nombre + ".yml");

        if (file.exists()) {
            src.sendFailure(Component.literal("§cEl panel '" + nombre + "' ya existe."));
            return 0;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# CustomPanels MOD");
            pw.println();
            pw.println("Main:");
            pw.println("  display_name: Panel " + nombre);
            pw.println("  resolution: 176x166");
            pw.println("  BGTransparency: 35");
            pw.println("  button_width: 30%");
            pw.println("  button_height: 5%");
            pw.println("  Texture: custompanels:textures/screens/gui.png");
            pw.println("  title_x: 50");
            pw.println("  title_y: 5");
            pw.println();
            pw.println("Buttons: {}");

            src.sendSuccess(() -> Component.literal("§aPanel '" + nombre + "' creado en config/custompanels/paneles/" + nombre + ".yml"), false);
            return 1;

        } catch (IOException e) {
            src.sendFailure(Component.literal("§cError al crear el panel: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    public static File getPanelDirectory(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            File worldDir = serverLevel.getServer().getServerDirectory();

            if (serverLevel.getServer().isSingleplayer()) {
                return serverLevel.getServer().getWorldPath(LevelResource.ROOT)
                        .resolve("config/custompanels/paneles")
                        .toFile();

            } else {
                // Servidor dedicado: guardar en carpeta principal del servidor
                return new File("config/custompanels/paneles");            }
        }
        // Fallback por seguridad
        return new File("config/custompanels/paneles");    }

    public static List<String> getPanelesExistentes(CommandSourceStack src) {
        File PANEL_DIR = getPanelDirectory(src.getLevel());
        if (!PANEL_DIR.exists()) return List.of();
        try {
            return Files.list(PANEL_DIR.toPath())
                    .map(p -> p.getFileName().toString().replaceFirst("\\.yml$", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}