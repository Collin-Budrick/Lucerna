package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.config.DebugOverlay;
import net.lucerna.telemetry.LucernaStatusSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class LucernaHudDebugOverlay {
    private static final int LEFT = 6;
    private static final int TOP = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_LINES = 16;
    private static final int TEXT_COLOR = 0xFFE5F0FF;
    private static final int BACKGROUND_COLOR = 0xA0000000;

    private LucernaHudDebugOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null || client.font == null) {
            return;
        }

        LucernaController controller = LucernaController.getInstance();
        if (controller.getConfig().debugOverlay() == DebugOverlay.OFF) {
            return;
        }

        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(controller);
        List<Component> lines = LucernaDebugOverlayLines.selectedOverlay(snapshot);
        if (lines.isEmpty()) {
            return;
        }

        int maxWidth = Math.min(420, Math.max(160, graphics.guiWidth() - 12));
        int lineCount = Math.min(MAX_LINES, lines.size());
        int backgroundHeight = lineCount * LINE_HEIGHT + 6;
        graphics.fill(LEFT - 3, TOP - 3, LEFT + maxWidth + 3, TOP + backgroundHeight, BACKGROUND_COLOR);

        int y = TOP;
        for (int index = 0; index < lineCount; index++) {
            graphics.text(client.font, fitLine(client, lines.get(index), maxWidth), LEFT, y, TEXT_COLOR);
            y += LINE_HEIGHT;
        }
    }

    private static Component fitLine(Minecraft client, Component line, int maxWidth) {
        String text = line.getString();
        if (client.font.width(text) <= maxWidth) {
            return line;
        }

        String suffix = "...";
        int end = text.length();
        while (end > 0 && client.font.width(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return Component.literal(text.substring(0, end) + suffix);
    }
}
