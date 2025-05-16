package me.manolopro3333.custompanels.config;

import me.manolopro3333.custompanels.Custompanels;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PanelConfigLoader {
    private static final String CONFIG_SUBDIR = "custompanels/paneles";


    public static int getPercentageValue(Map<String, String> config, String key, int defaultValue, int max) {
        String value = config.get(key);
        if (value == null) return (max * defaultValue) / 100;
        try {
            int percent = Integer.parseInt(value);
            percent = Math.max(0, Math.min(100, percent)); // Limitar entre 0-100
            return (max * percent) / 100;
        } catch (NumberFormatException e) {
            return (max * defaultValue) / 100;
        }
    }

    public static int getInt(Map<String, String> config, String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Map<String, String> loadConfig(String panelName, Level level) {
        Path configPath = getConfigPath(level).resolve(panelName + ".yml");
        Map<String, String> configData = new LinkedHashMap<>();

        try {
            if (Files.exists(configPath)) {
                parseYaml(configPath, configData);
                Custompanels.LOGGER.debug("[DEBUG] Config cargada desde: {}", configPath.toAbsolutePath());
            } else {
                Custompanels.LOGGER.error("[ERROR] Archivo no encontrado: {}", configPath.toAbsolutePath());
            }
        } catch (IOException e) {
            Custompanels.LOGGER.error("[ERROR] Fallo al cargar configuración: {}", e.getMessage());
        }
        return configData;
    }

    private static void parseYaml(Path configPath, Map<String, String> configData) throws IOException {
        List<String> context = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            String line;
            int lastIndent = 0;

            while ((line = reader.readLine()) != null) {
                line = line.replace("\t", "  "); // Convertir tabs a espacios
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                int indent = line.indexOf(line.trim()) / 2;
                line = line.trim();

                // Manejar contexto
                while (context.size() > indent) {
                    context.remove(context.size() - 1);
                }

                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (!context.isEmpty()) {
                        key = String.join(".", context) + "." + key;
                    }

                    configData.put(key, value);
                    context.add(parts[0].trim());
                    lastIndent = indent;
                }
            }
        }
    }

    private static Path getConfigPath(Level level) {
        // 1. Verificar si estamos en un mundo de singleplayer (aunque sea desde el cliente)
        if (isSingleplayerWorld(level)) {
            System.out.println("[DEBUG] w4 - Singleplayer detectado (desde cliente)");
            return getSingleplayerConfigPath(level);
        }

        // 2. Para servidor dedicado
        if (level instanceof ServerLevel serverLevel && !serverLevel.getServer().isSingleplayer()) {
            System.out.println("[DEBUG] w5 - Servidor dedicado");
            return Paths.get("config").resolve(CONFIG_SUBDIR);
        }

        // 3. Para cliente puro (multijugador) o fallback
        System.out.println("[DEBUG] w2 - Entorno cliente estándar");
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_SUBDIR);
    }

    // Método auxiliar: Determinar si es singleplayer
    private static boolean isSingleplayerWorld(Level level) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Verificar si hay un servidor local activo (singleplayer)
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            return server != null && !server.isStopped();
        }
        return level instanceof ServerLevel serverLevel && serverLevel.getServer().isSingleplayer();
    }

    // Método auxiliar: Obtener ruta de singleplayer
    private static Path getSingleplayerConfigPath(Level level) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("config")
                .resolve(CONFIG_SUBDIR);
    }

    // Métodos auxiliares
    public static String getValue(Map<String, String> config, String key) {
        return config.getOrDefault(key, "");
    }

    public static List<String> getList(Map<String, String> config, String baseKey) {
        List<String> list = new ArrayList<>();
        int index = 0;
        while (config.containsKey(baseKey + "[" + index + "]")) {
            list.add(config.get(baseKey + "[" + index + "]"));
            index++;
        }
        return list;
    }
}