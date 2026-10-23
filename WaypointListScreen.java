package dev.waypointmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Glowny ekran managera: lista waypointow, przycisk "+ Dodaj", edycja, usuwanie, wl./wyl. */
public class WaypointListScreen extends Screen {
    private static final int ROW_H = 24;

    private final Screen parent;
    private final int page;

    private int curPage;
    private int rows;
    private int top;
    private int left;
    private int pageY;

    public WaypointListScreen(Screen parent, int page) {
        super(Text.translatable("waypointmanager.screen.list"));
        this.parent = parent;
        this.page = Math.max(0, page);
    }

    /** Swiezo zbudowana kopia tego ekranu (po zmianie danych). */
    WaypointListScreen refreshed() {
        return new WaypointListScreen(parent, curPage);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        left = cx - 141;
        top = 32;
        int doneY = height - 26;
        int toggleY = height - 50;
        pageY = height - 74;

        rows = Math.max(1, (pageY - 4 - top) / ROW_H);
        List<Waypoint> all = WaypointStore.WAYPOINTS;
        int total = all.size();
        int pages = Math.max(1, (total + rows - 1) / rows);
        curPage = Math.min(page, pages - 1);

        for (int i = 0; i < rows; i++) {
            int idx = curPage * rows + i;
            if (idx >= total) {
                break;
            }
            Waypoint wp = all.get(idx);
            int y = top + i * ROW_H;

            addDrawableChild(ButtonWidget.builder(Text.translatable("waypointmanager.button.edit"),
                    b -> client.setScreen(new WaypointEditScreen(this, wp)))
                    .dimensions(left + 172, y, 44, 20).build());

            addDrawableChild(ButtonWidget.builder(ScreenTexts.onOrOff(wp.visible), b -> {
                wp.visible = !wp.visible;
                WaypointStore.save();
                b.setMessage(ScreenTexts.onOrOff(wp.visible));
            }).dimensions(left + 220, y, 38, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("X").formatted(Formatting.RED), b -> {
                WaypointStore.WAYPOINTS.remove(wp);
                WaypointStore.save();
                client.setScreen(new WaypointListScreen(parent, curPage));
            }).dimensions(left + 262, y, 20, 20).build());
        }

        // Paginacja
        ButtonWidget prev = ButtonWidget.builder(Text.literal("<"),
                b -> client.setScreen(new WaypointListScreen(parent, curPage - 1)))
                .dimensions(cx - 80, pageY, 20, 20).build();
        prev.active = curPage > 0;
        addDrawableChild(prev);

        ButtonWidget next = ButtonWidget.builder(Text.literal(">"),
                b -> client.setScreen(new WaypointListScreen(parent, curPage + 1)))
                .dimensions(cx + 60, pageY, 20, 20).build();
        next.active = curPage < pages - 1;
        addDrawableChild(next);

        // Dodaj / znaczniki / lista HUD
        addDrawableChild(ButtonWidget.builder(Text.translatable("waypointmanager.button.add"),
                b -> client.setScreen(new WaypointEditScreen(this, null)))
                .dimensions(left, toggleY, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("waypointmanager.button.markers", ScreenTexts.onOrOff(WaypointStore.showMarkers)), b -> {
                    WaypointStore.showMarkers = !WaypointStore.showMarkers;
                    WaypointStore.save();
                    b.setMessage(Text.translatable("waypointmanager.button.markers",
                            ScreenTexts.onOrOff(WaypointStore.showMarkers)));
                }).dimensions(left + 94, toggleY, 94, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("waypointmanager.button.hud", ScreenTexts.onOrOff(WaypointStore.showList)), b -> {
                    WaypointStore.showList = !WaypointStore.showList;
                    WaypointStore.save();
                    b.setMessage(Text.translatable("waypointmanager.button.hud",
                            ScreenTexts.onOrOff(WaypointStore.showList)));
                }).dimensions(left + 192, toggleY, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
                .dimensions(cx - 100, doneY, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), cx, 12, 0xFFFFFFFF);

        List<Waypoint> all = WaypointStore.WAYPOINTS;
        int total = all.size();
        int pages = Math.max(1, (total + rows - 1) / rows);

        if (total == 0) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("waypointmanager.list.empty"),
                    cx, top + 20, 0xFFAAAAAA);
        }

        String currentDim = WaypointStore.currentDimension(client);
        double px = client.player != null ? client.player.getX() : 0;
        double py = client.player != null ? client.player.getY() : 0;
        double pz = client.player != null ? client.player.getZ() : 0;

        for (int i = 0; i < rows; i++) {
            int idx = curPage * rows + i;
            if (idx >= total) {
                break;
            }
            Waypoint wp = all.get(idx);
            int y = top + i * ROW_H;

            ctx.fill(left, y + 2, left + 10, y + 12, 0xFF000000);
            ctx.fill(left + 1, y + 3, left + 9, y + 11, 0xFF000000 | wp.color);

            String name = textRenderer.trimToWidth(wp.name, 150);
            ctx.drawText(textRenderer, name, left + 14, y + 2, wp.visible ? 0xFFFFFFFF : 0xFF777777, true);

            boolean sameDim = wp.dimension == null || wp.dimension.equals(currentDim);
            String info = wp.x + ", " + wp.y + ", " + wp.z + "  |  "
                    + (sameDim ? Waypoint.formatDistance(wp.distanceTo(px, py, pz))
                    : WaypointStore.dimensionName(wp.dimension).getString());
            ctx.drawText(textRenderer, textRenderer.trimToWidth(info, 154), left + 14, y + 12, 0xFFAAAAAA, false);
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("waypointmanager.list.page", String.valueOf(curPage + 1), String.valueOf(pages)),
                cx, pageY + 6, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
