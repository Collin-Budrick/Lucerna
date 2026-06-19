package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.config.DebugOverlay;
import net.lucerna.nativebridge.DirectLightingCpuOutputPayload;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
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
    private static final int GI_PROOF_FILL_COLOR = 0x78FFB040;
    private static final int GI_PROOF_BORDER_COLOR = 0xFFFFF0A8;
    private static final int GI_PROOF_LABEL_BACKGROUND_COLOR = 0xD0182418;

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
        boolean round6GiProofVisible = shouldRenderRound6GiProofOverlay(snapshot);
        if (!debugOverlayVisible && !proofOverlayVisible && !round6GiProofVisible) {
            return;
        }

        if (debugOverlayVisible) {
            renderDebugLines(graphics, client, snapshot);
        }
        if (proofOverlayVisible) {
            renderDirectLightProofOverlay(graphics, client, controller.directLightingCpuOutputPayload());
        }
        if (round6GiProofVisible) {
            renderRound6GiProofOverlay(graphics, client, controller.round6DiffuseGiCpuOutputPayload());
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

    private static void renderRound6GiProofOverlay(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            Round6DiffuseGiCpuOutputPayload payload
    ) {
        int left = Math.max(0, Math.round(graphics.guiWidth() * 0.30F));
        int top = Math.max(0, Math.round(graphics.guiHeight() * 0.20F));
        int right = Math.min(graphics.guiWidth(), Math.round(graphics.guiWidth() * 0.70F));
        int bottom = Math.min(graphics.guiHeight(), Math.round(graphics.guiHeight() * 0.75F));
        if (right <= left || bottom <= top) {
            return;
        }

        graphics.fill(left, top, right, bottom, GI_PROOF_FILL_COLOR);
        graphics.fill(left, top, right, top + 2, GI_PROOF_BORDER_COLOR);
        graphics.fill(left, bottom - 2, right, bottom, GI_PROOF_BORDER_COLOR);
        graphics.fill(left, top, left + 2, bottom, GI_PROOF_BORDER_COLOR);
        graphics.fill(right - 2, top, right, bottom, GI_PROOF_BORDER_COLOR);

        int labelWidth = Math.min(150, Math.max(92, right - left - 8));
        int labelLeft = left + 4;
        int labelTop = top + 4;
        graphics.fill(labelLeft, labelTop, labelLeft + labelWidth, labelTop + 22, GI_PROOF_LABEL_BACKGROUND_COLOR);
        graphics.text(client.font, Component.literal("R6 GI proof"), labelLeft + 4, labelTop + 3, 0xFFFFFFFF);
        graphics.text(
                client.font,
                Component.literal("GI " + round6GiEvidenceLabel(payload)),
                labelLeft + 4,
                labelTop + 13,
                0xFFE5F0FF
        );
    }

    private static boolean shouldRenderRound6GiProofOverlay(LucernaStatusSnapshot snapshot) {
        if (!snapshot.rendererEnabled() || !snapshot.rendererActive()) {
            return false;
        }
        return LucernaController.getInstance().round6DiffuseGiCpuOutputPayload().readyForPreviewDraw();
    }

    private static String round6GiEvidenceLabel(Round6DiffuseGiCpuOutputPayload payload) {
        if (payload == null || !payload.readyForPreviewDraw()) {
            return "";
        }
        String checksum = payload.snapshot().outputChecksum();
        if (checksum.length() > 8) {
            checksum = checksum.substring(checksum.length() - 8);
        }
        return "#" + checksum;
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
