package dev.waypointmanager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rysuje waypointy na ekranie (HUD):
 *  - znaczniki w swiecie (romb + nazwa + odleglosc), a gdy waypoint jest poza ekranem - przy krawedzi,
 *  - liste najblizszych waypointow z odlegloscia w lewym gornym rogu.
 * Rzutowanie 3D -> 2D liczone recznie z pozycji i obrotu kamery, wiec nie wymaga mixinow.
 */
public final class WaypointHud {
    private static final int EDGE_MARGIN = 24;

    private record Entry(Waypoint wp, double dist) {
    }

    private WaypointHud() {
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        if (WaypointStore.WAYPOINTS.isEmpty()) {
            return;
        }

        String dim = mc.world.getRegistryKey().getValue().toString();
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        List<Entry> entries = new ArrayList<>();
        for (Waypoint wp : WaypointStore.WAYPOINTS) {
            if (!wp.visible) {
                continue;
            }
            if (wp.dimension != null && !wp.dimension.equals(dim)) {
                continue;
            }
            entries.add(new Entry(wp, wp.distanceTo(px, py, pz)));
        }
        if (entries.isEmpty()) {
            return;
        }

        if (WaypointStore.showMarkers) {
            drawMarkers(ctx, mc, entries);
        }
        if (WaypointStore.showList) {
            drawList(ctx, mc, entries);
        }
    }

    private static void drawMarkers(DrawContext ctx, MinecraftClient mc, List<Entry> entries) {
        TextRenderer tr = mc.textRenderer;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);

        // Baza kamery: przod, prawo, gora (Minecraft: yaw 0 = poludnie +Z, pitch > 0 = patrzenie w dol)
        double fx = -sinY * cosP, fy = -sinP, fz = cosY * cosP;
        double rx = -cosY, ry = 0, rz = -sinY;
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double tanV = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double tanH = tanV * ((double) w / (double) h);

        // Najdalsze rysujemy pierwsze, zeby najblizsze byly na wierzchu.
        List<Entry> ordered = new ArrayList<>(entries);
        ordered.sort(Comparator.comparingDouble(Entry::dist).reversed());

        for (Entry e : ordered) {
            Waypoint wp = e.wp();
            double dx = wp.x + 0.5 - cp.x;
            double dy = wp.y + 1.0 - cp.y;
            double dz = wp.z + 0.5 - cp.z;

            double xc = dx * rx + dy * ry + dz * rz;
            double yc = dx * ux + dy * uy + dz * uz;
            double zc = dx * fx + dy * fy + dz * fz;

            double sx = 0;
            double sy = 0;
            boolean onScreen = false;
            if (zc > 0.05) {
                sx = w / 2.0 + (xc / zc) / tanH * (w / 2.0);
                sy = h / 2.0 - (yc / zc) / tanV * (h / 2.0);
                onScreen = sx >= EDGE_MARGIN && sx <= w - EDGE_MARGIN
                        && sy >= EDGE_MARGIN && sy <= h - EDGE_MARGIN;
            }

            if (!onScreen) {
                // Waypoint poza ekranem lub za plecami: przypnij znacznik do krawedzi w jego kierunku.
                double ddx = xc;
                double ddy = -yc;
                double len = Math.sqrt(ddx * ddx + ddy * ddy);
                if (len < 1e-6) {
                    ddx = 0;
                    ddy = 1;
                    len = 1;
                }
                ddx /= len;
                ddy /= len;
                double tx = (w / 2.0 - EDGE_MARGIN) / Math.max(Math.abs(ddx), 1e-6);
                double ty = (h / 2.0 - EDGE_MARGIN) / Math.max(Math.abs(ddy), 1e-6);
                double t = Math.min(tx, ty);
                sx = w / 2.0 + ddx * t;
                sy = h / 2.0 + ddy * t;
            }

            int ix = (int) Math.round(sx);
            int iy = (int) Math.round(sy);
            int rgb = 0xFF000000 | wp.color;
            String distText = Waypoint.formatDistance(e.dist());

            if (onScreen) {
                diamond(ctx, ix, iy, 5, 0xFF000000);
                diamond(ctx, ix, iy, 4, rgb);
                ctx.drawText(tr, wp.name, ix - tr.getWidth(wp.name) / 2, iy - 18, 0xFFFFFFFF, true);
                ctx.drawCenteredTextWithShadow(tr, distText, ix, iy + 8, 0xFFDDDDDD);
            } else {
                diamond(ctx, ix, iy, 4, 0xFF000000);
                diamond(ctx, ix, iy, 3, rgb);
                ctx.drawCenteredTextWithShadow(tr, distText, ix, iy + 6, 0xFFDDDDDD);
            }
        }
    }

    private static void drawList(DrawContext ctx, MinecraftClient mc, List<Entry> entries) {
        TextRenderer tr = mc.textRenderer;
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingDouble(Entry::dist));

        int y = 4;
        int shown = 0;
        for (Entry e : sorted) {
            if (shown++ >= 8) {
                break;
            }
            ctx.fill(4, y, 12, y + 8, 0xFF000000);
            ctx.fill(5, y + 1, 11, y + 7, 0xFF000000 | e.wp().color);
            ctx.drawText(tr, e.wp().name + "  " + Waypoint.formatDistance(e.dist()), 16, y, 0xFFFFFFFF, true);
            y += 11;
        }
    }

    private static void diamond(DrawContext ctx, int cx, int cy, int r, int argb) {
        for (int i = -r; i <= r; i++) {
            int half = r - Math.abs(i);
            ctx.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, argb);
        }
    }
}
