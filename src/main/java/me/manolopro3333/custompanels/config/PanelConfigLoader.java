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



    public static void saveConfig(String panelName, Level level, Map<String, String> configData) {
        Path configPath = getConfigPath(level).resolve(panelName + ".yml");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(configPath))) {
            Map<String, Object> yamlStructure = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : configData.entrySet()) {
                String[] keys = entry.getKey().split("\\.");
                Map<String, Object> currentMap = yamlStructure;

                for (int i = 0; i < keys.length - 1; i++) {
                    // Verificar si el nodo actual es un mapa válido
                    Object node = currentMap.get(keys[i]);
                    if (!(node instanceof Map)) {
                        // Si no lo es, crear nuevo mapa y reemplazar el valor existente
                        node = new LinkedHashMap<>();
                        currentMap.put(keys[i], node);
                    }
                    currentMap = (Map<String, Object>) node;
                }

                // Guardar el valor final
                currentMap.put(keys[keys.length - 1], entry.getValue());
            }

            writeYaml(writer, yamlStructure, 0);

        } catch (IOException e) {
            Custompanels.LOGGER.error("Error al guardar el panel: {}", e.getMessage());
        }
    }

    private static void writeYaml(PrintWriter writer, Map<String, Object> data, int indentLevel) {
        String indent = "  ".repeat(indentLevel);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map) {
                writer.println(indent + entry.getKey() + ":");
                writeYaml(writer, (Map<String, Object>) entry.getValue(), indentLevel + 1);
            } else if (entry.getValue() instanceof List) {
                // Manejar listas si es necesario
            } else {
                writer.println(indent + entry.getKey() + ": " + entry.getValue().toString());
            }
        }
    }

    public static int getButtonWidth(Map<String, String> config, int screenWidth) {
        String value = config.getOrDefault("Main.button_width", "30%");
        return parseSize(value, screenWidth, 150, 100, 30); // 30% default, max 150px
    }

    public static int getButtonHeight(Map<String, String> config, int screenHeight) {
        String value = config.getOrDefault("Main.button_height", "5%");
        return parseSize(value, screenHeight, 40, 20, 5); // 5% default, max 40px
    }

    private static int parseSize(String value, int base, int maxPx, int minPx, int defaultPercent) {
        try {
            if (value.contains("%")) {
                int percent = Integer.parseInt(value.replace("%", "").trim());
                return Math.min(maxPx, Math.max(minPx, (base * percent) / 100));
            }
            return Math.min(maxPx, Math.max(minPx, Integer.parseInt(value)));
        } catch (Exception e) {
            return Math.min(maxPx, Math.max(minPx, (base * defaultPercent) / 100));
        }
    }

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