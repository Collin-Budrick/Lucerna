package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.config.DebugOverlay;
import net.lucerna.nativebridge.DirectLightingCpuOutputPayload;
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
    private static final int PROOF_WIDTH = 128;
    private static final int PROOF_HEIGHT = 28;
    private static final int PROOF_MARGIN = 6;
    private static final int PROOF_BACKGROUND_COLOR = 0xD0203038;
    private static final int PROOF_ACCENT_COLOR = 0xFF39E6FF;
    private static final int PROOF_READY_COLOR = 0xFFFFD34D;

    private LucernaHudDebugOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null || client.font == null) {
            return;
        }

        LucernaController controller = LucernaController.getInstance();
        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(controller);

        boolean debugOverlayVisible = controller.getConfig().debugOverlay() != DebugOverlay.OFF;
        boolean proofOverlayVisible = shouldRenderDirectLightProofOverlay(snapshot);
        if (!debugOverlayVisible && !proofOverlayVisible) {
            return;
        }

        if (debugOverlayVisible) {
            renderDebugLines(graphics, client, snapshot);
        }
        if (proofOverlayVisible) {
            renderDirectLightProofOverlay(graphics, client, controller.directLightingCpuOutputPayload());
        }
    }

    private static void renderDebugLines(GuiGraphicsExtractor graphics, Minecraft client, LucernaStatusSnapshot snapshot) {
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

    private static void renderDirectLightProofOverlay(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            DirectLightingCpuOutputPayload payload
    ) {
        int left = Math.max(PROOF_MARGIN, graphics.guiWidth() - PROOF_WIDTH - PROOF_MARGIN);
        int top = PROOF_MARGIN;
        int right = left + PROOF_WIDTH;
        int bottom = top + PROOF_HEIGHT;

        graphics.fill(left, top, right, bottom, PROOF_BACKGROUND_COLOR);
        graphics.fill(left, top, right, top + 2, PROOF_ACCENT_COLOR);
        graphics.fill(left, bottom - 2, right, bottom, PROOF_ACCENT_COLOR);
        graphics.fill(left, top, left + 2, bottom, PROOF_ACCENT_COLOR);
        graphics.fill(right - 2, top, right, bottom, PROOF_ACCENT_COLOR);
        graphics.fill(left + 5, top + 6, left + 17, bottom - 6, PROOF_READY_COLOR);
        graphics.text(client.font, Component.literal("CPU output proof"), left + 22, top + 5, 0xFFFFFFFF);
        graphics.text(
                client.font,
                Component.literal("CPU " + proofEvidenceLabel(payload)),
                left + 22,
                top + 16,
                0xFFE5F0FF
        );
    }

    private static boolean shouldRenderDirectLightProofOverlay(LucernaStatusSnapshot snapshot) {
        if (!snapshot.rendererEnabled() || !snapshot.rendererActive()) {
            return false;
        }
        return LucernaController.getInstance().directLightingCpuOutputPayload().readyForPreviewDraw();
    }

    private static String proofEvidenceLabel(DirectLightingCpuOutputPayload payload) {
        if (payload == null || !payload.readyForPreviewDraw()) {
            return "";
        }
        String checksum = payload.snapshot().outputChecksum();
        if (checksum.length() > 8) {
            checksum = checksum.substring(checksum.length() - 8);
        }
        return "#" + checksum;
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
