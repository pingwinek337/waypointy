package dev.waypointmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Przechowuje waypointy aktualnego swiata/serwera.
 * Kazdy swiat ma wlasny plik: config/waypointmanager/<swiat>.json
 */
public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final List<Waypoint> WAYPOINTS = new ArrayList<>();
    public static boolean showMarkers = true;
    public static boolean showList = true;

    private static Path file;

    private static class Data {
        boolean showMarkers = true;
        boolean showList = true;
        List<Waypoint> waypoints = new ArrayList<>();
    }

    private WaypointStore() {
    }

    public static void load(String worldId) {
        WAYPOINTS.clear();
        showMarkers = true;
        showList = true;

        Path dir = FabricLoader.getInstance().getConfigDir().resolve("waypointmanager");
        String safe = worldId.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.length() > 100) {
            safe = safe.substring(0, 100);
        }
        file = dir.resolve(safe + ".json");

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data != null) {
                    showMarkers = data.showMarkers;
                    showList = data.showList;
                    if (data.waypoints != null) {
                        WAYPOINTS.addAll(data.waypoints);
                    }
                }
            } catch (Exception e) {
                WaypointManagerClient.LOGGER.error("Nie udalo sie wczytac waypointow z {}", file, e);
            }
        }
    }

    public static void unload() {
        WAYPOINTS.clear();
        file = null;
    }

    public static void save() {
        if (file == null) {
            return;
        }
        Data data = new Data();
        data.showMarkers = showMarkers;
        data.showList = showList;
        data.waypoints = new ArrayList<>(WAYPOINTS);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            WaypointManagerClient.LOGGER.error("Nie udalo sie zapisac waypointow do {}", file, e);
        }
    }

    public static String currentDimension(MinecraftClient mc) {
        if (mc.world == null) {
            return "minecraft:overworld";
        }
        return mc.world.getRegistryKey().getValue().toString();
    }

    public static Text dimensionName(String dim) {
        if (dim == null) {
            return Text.translatable("waypointmanager.dimension.overworld");
        }
        return switch (dim) {
            case "minecraft:overworld" -> Text.translatable("waypointmanager.dimension.overworld");
            case "minecraft:the_nether" -> Text.translatable("waypointmanager.dimension.nether");
            case "minecraft:the_end" -> Text.translatable("waypointmanager.dimension.end");
            default -> Text.literal(dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim);
        };
    }
}
