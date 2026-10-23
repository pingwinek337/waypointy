package dev.waypointmanager;

import java.util.Locale;

/** Pojedynczy waypoint. Pola publiczne, bo serializujemy je Gsonem. */
public class Waypoint {
    /** 16 kolorow do szybkiego wyboru w GUI (0xRRGGBB). */
    public static final int[] PALETTE = {
            0xFFFFFF, 0xAAAAAA, 0x555555, 0x8B4513,
            0xFF5555, 0xAA0000, 0xFFAA00, 0xFFFF55,
            0x55FF55, 0x00AA00, 0x55FFFF, 0x00AAAA,
            0x5555FF, 0x0000AA, 0xFF55FF, 0xAA00AA
    };

    public String name = "";
    public int x;
    public int y;
    public int z;
    /** Kolor 0xRRGGBB (bez kanalu alfa). */
    public int color = 0xFF5555;
    /** Id wymiaru, np. minecraft:overworld. */
    public String dimension;
    public boolean visible = true;

    public Waypoint() {
    }

    public Waypoint(String name, int x, int y, int z, int color, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.dimension = dimension;
    }

    /** Odleglosc 3D od podanej pozycji (srodek bloku w X/Z, poziom stop w Y). */
    public double distanceTo(double px, double py, double pz) {
        double dx = x + 0.5 - px;
        double dy = y - py;
        double dz = z + 0.5 - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static String formatDistance(double d) {
        if (d >= 1000) {
            return String.format(Locale.ROOT, "%.1f km", d / 1000.0);
        }
        return Math.round(d) + " m";
    }
}
