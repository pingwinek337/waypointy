package dev.waypointmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

/** Maly klikalny kwadracik z kolorem (paleta w oknie edycji waypointa). */
public class ColorSwatch extends ClickableWidget {
    private final int rgb;
    private final BooleanSupplier selected;
    private final Runnable onPress;

    public ColorSwatch(int x, int y, int size, int rgb, BooleanSupplier selected, Runnable onPress) {
        super(x, y, size, size, Text.empty());
        this.rgb = rgb;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x1 = getX();
        int y1 = getY();
        int x2 = x1 + getWidth();
        int y2 = y1 + getHeight();
        int border = selected.getAsBoolean() ? 0xFFFFFFFF : (isHovered() ? 0xFFAAAAAA : 0xFF000000);
        ctx.fill(x1, y1, x2, y2, border);
        ctx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF000000 | rgb);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
