package dev.waypointmanager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaypointManagerClient implements ClientModInitializer {
    public static final String MOD_ID = "waypointmanager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String CATEGORY = "key.categories.waypointmanager";

    @Override
    public void onInitializeClient() {
        KeyBinding openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.waypointmanager.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        KeyBinding addKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.waypointmanager.add", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY));
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.waypointmanager.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.world != null && client.currentScreen == null) {
                    client.setScreen(new WaypointListScreen(null, 0));
                }
            }
            while (addKey.wasPressed()) {
                if (client.player != null && client.world != null && client.currentScreen == null) {
                    client.setScreen(new WaypointEditScreen(null, null));
                }
            }
            while (toggleKey.wasPressed()) {
                if (client.player != null) {
                    WaypointStore.showMarkers = !WaypointStore.showMarkers;
                    WaypointStore.save();
                    client.player.sendMessage(Text.translatable("waypointmanager.message.markers",
                            ScreenTexts.onOrOff(WaypointStore.showMarkers)), true);
                }
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                WaypointStore.load(worldId(handler, client)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WaypointStore.unload());

        HudRenderCallback.EVENT.register(WaypointHud::render);
    }

    /** Identyfikator swiata: nazwa swiata (singleplayer) albo adres serwera (multiplayer). */
    private static String worldId(ClientPlayNetworkHandler handler, MinecraftClient client) {
        IntegratedServer server = client.getServer();
        if (server != null) {
            return "sp_" + server.getSaveProperties().getLevelName();
        }
        ServerInfo info = handler.getServerInfo();
        if (info != null) {
            return "mp_" + info.address;
        }
        return "unknown";
    }
}
