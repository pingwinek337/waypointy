package dev.waypointmanager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Okno dodawania / edycji waypointa: nazwa, X Y Z, kolor (hex + paleta), wymiar. */
public class WaypointEditScreen extends Screen {
    private final Screen parent;
    private final Waypoint editing;

    private String nameVal;
    private String xVal;
    private String yVal;
    private String zVal;
    private int color;
    private String dimension;
    private final List<String> dimensions = new ArrayList<>();

    private TextFieldWidget nameField;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private TextFieldWidget hexField;
    private ButtonWidget dimButton;

    private Text error;
    private int top;
    private int fx;

    /** @param editing istniejacy waypoint do edycji albo null, gdy tworzymy nowy */
    public WaypointEditScreen(Screen parent, Waypoint editing) {
        super(Text.translatable(editing == null ? "waypointmanager.screen.add" : "waypointmanager.screen.edit"));
        this.parent = parent;
        this.editing = editing;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (editing != null) {
            nameVal = editing.name;
            xVal = String.valueOf(editing.x);
            yVal = String.valueOf(editing.y);
            zVal = String.valueOf(editing.z);
            color = editing.color;
            dimension = editing.dimension != null ? editing.dimension : WaypointStore.currentDimension(mc);
        } else {
            BlockPos pos = mc.player != null ? mc.player.getBlockPos() : BlockPos.ORIGIN;
            int n = WaypointStore.WAYPOINTS.size();
            nameVal = Text.translatable("waypointmanager.default_name", String.valueOf(n + 1)).getString();
            xVal = String.valueOf(pos.getX());
            yVal = String.valueOf(pos.getY());
            zVal = String.valueOf(pos.getZ());
            color = Waypoint.PALETTE[4 + n % 12];
            dimension = WaypointStore.currentDimension(mc);
        }

        dimensions.add("minecraft:overworld");
        dimensions.add("minecraft:the_nether");
        dimensions.add("minecraft:the_end");
        if (!dimensions.contains(dimension)) {
            dimensions.add(0, dimension);
        }
    }

    @Override
    protected void init() {
        int cx = width / 2;
        top = Math.max(20, (height - 208) / 2 + 8);
        fx = cx - 100;

        // Nazwa
        nameField = new TextFieldWidget(textRenderer, fx, top + 10, 200, 20, Text.translatable("waypointmanager.field.name"));
        nameField.setMaxLength(32);
        nameField.setChangedListener(s -> nameVal = s);
        nameField.setText(nameVal);
        addDrawableChild(nameField);

        // X Y Z
        xField = coordField(fx, top + 46, 62, xVal, s -> xVal = s, "X");
        yField = coordField(fx + 69, top + 46, 62, yVal, s -> yVal = s, "Y");
        zField = coordField(fx + 138, top + 46, 62, zVal, s -> zVal = s, "Z");

        // Moja pozycja + wymiar
        addDrawableChild(ButtonWidget.builder(Text.translatable("waypointmanager.button.here"), b -> fillHere())
                .dimensions(fx, top + 72, 98, 20).build());
        dimButton = ButtonWidget.builder(dimensionLabel(), b -> {
            int i = (dimensions.indexOf(dimension) + 1) % dimensions.size();
            dimension = dimensions.get(i);
            b.setMessage(dimensionLabel());
        }).dimensions(fx + 102, top + 72, 98, 20).build();
        addDrawableChild(dimButton);

        // Kolor: pole hex + podglad + paleta
        hexField = new TextFieldWidget(textRenderer, fx, top + 110, 80, 20, Text.translatable("waypointmanager.field.color"));
        hexField.setMaxLength(7);
        hexField.setChangedListener(s -> {
            Integer c = parseHex(s);
            if (c != null) {
                color = c;
            }
        });
        hexField.setText(toHex(color));
        addDrawableChild(hexField);

        for (int i = 0; i < Waypoint.PALETTE.length; i++) {
            int c = Waypoint.PALETTE[i];
            int sx = cx - 86 + (i % 8) * 22;
            int sy = top + 136 + (i / 8) * 22;
            addDrawableChild(new ColorSwatch(sx, sy, 18, c, () -> color == c, () -> {
                color = c;
                hexField.setText(toHex(c));
            }));
        }

        // Zapisz / Anuluj
        addDrawableChild(ButtonWidget.builder(Text.translatable("waypointmanager.button.save"), b -> save())
                .dimensions(fx, top + 188, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, b -> finish())
                .dimensions(fx + 102, top + 188, 98, 20).build());

        setInitialFocus(nameField);
    }

    private TextFieldWidget coordField(int x, int y, int w, String value, Consumer<String> setter, String label) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(label));
        f.setMaxLength(9);
        f.setTextPredicate(s -> s.matches("-?\\d*"));
        f.setChangedListener(setter);
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private Text dimensionLabel() {
        return Text.translatable("waypointmanager.button.dimension", WaypointStore.dimensionName(dimension));
    }

    private void fillHere() {
        if (client == null || client.player == null) {
            return;
        }
        BlockPos p = client.player.getBlockPos();
        xField.setText(String.valueOf(p.getX()));
        yField.setText(String.valueOf(p.getY()));
        zField.setText(String.valueOf(p.getZ()));
        dimension = WaypointStore.currentDimension(client);
        if (!dimensions.contains(dimension)) {
            dimensions.add(0, dimension);
        }
        dimButton.setMessage(dimensionLabel());
    }

    private void save() {
        String n = nameVal.trim();
        if (n.isEmpty()) {
            error = Text.translatable("waypointmanager.error.name");
            return;
        }
        Integer x = parseInt(xVal);
        Integer y = parseInt(yVal);
        Integer z = parseInt(zVal);
        if (x == null || y == null || z == null) {
            error = Text.translatable("waypointmanager.error.coords");
            return;
        }

        if (editing != null) {
            editing.name = n;
            editing.x = x;
            editing.y = y;
            editing.z = z;
            editing.color = color;
            editing.dimension = dimension;
        } else {
            WaypointStore.WAYPOINTS.add(new Waypoint(n, x, y, z, color, dimension));
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.translatable("waypointmanager.message.added", n), true);
            }
        }
        WaypointStore.save();
        finish();
    }

    /** Wraca do poprzedniego ekranu; liste odbudowujemy, zeby pokazala zmiany. */
    private void finish() {
        if (parent instanceof WaypointListScreen list) {
            client.setScreen(list.refreshed());
        } else {
            client.setScreen(parent);
        }
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && getFocused() instanceof TextFieldWidget) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), cx, 6, 0xFFFFFFFF);

        int gray = 0xFFAAAAAA;
        ctx.drawText(textRenderer, Text.translatable("waypointmanager.field.name"), fx, top, gray, false);
        ctx.drawText(textRenderer, "X", fx, top + 36, gray, false);
        ctx.drawText(textRenderer, "Y", fx + 69, top + 36, gray, false);
        ctx.drawText(textRenderer, "Z", fx + 138, top + 36, gray, false);
        ctx.drawText(textRenderer, Text.translatable("waypointmanager.field.color"), fx, top + 100, gray, false);

        // Podglad koloru obok pola hex
        int px = fx + 86;
        int py = top + 110;
        ctx.fill(px, py, px + 20, py + 20, 0xFFFFFFFF);
        ctx.fill(px + 1, py + 1, px + 19, py + 19, 0xFF000000 | color);

        if (error != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, error, cx, top + 176, 0xFFFF5555);
        }
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseHex(String s) {
        String t = s.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        if (t.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
