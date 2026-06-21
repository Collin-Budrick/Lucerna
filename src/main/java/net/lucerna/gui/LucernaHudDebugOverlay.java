package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.config.DebugOverlay;
import net.lucerna.nativebridge.DirectLightingCpuOutputPayload;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
import net.lucerna.render.preview.ProofVisualMode;
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
    private static final int ROUND6_MAX_LINES = 10;
    private static final int PANEL_GUTTER = 8;
    private static final int MIN_SECONDARY_PANEL_WIDTH = 220;
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
    private static final int ROUND8_LEGEND_WIDTH = 172;
    private static final int ROUND8_LEGEND_HEIGHT = 44;
    private static final int ROUND8_LEGEND_SWATCH_WIDTH = 28;
    private static final int ROUND8_LEGEND_BACKGROUND_COLOR = 0xD0182028;

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
            renderRoundEightLegendIfNeeded(graphics, client, snapshot.debugOverlay());
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
        int lineCount = renderLinePanel(graphics, client, lines, LEFT, TOP, maxWidth, MAX_LINES);
        if (snapshot.debugOverlay() == DebugOverlay.DIRECT_LIGHTING) {
            renderRoundSixEvidencePanel(graphics, client, snapshot, maxWidth, lineCount);
        }
    }

    private static void renderRoundSixEvidencePanel(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            LucernaStatusSnapshot snapshot,
            int primaryWidth,
            int primaryLineCount
    ) {
        List<Component> lines = LucernaDebugOverlayLines.roundSixEvidenceOverlay(snapshot);
        if (lines.isEmpty()) {
            return;
        }

        int rightColumnLeft = LEFT + primaryWidth + PANEL_GUTTER;
        int rightColumnWidth = graphics.guiWidth() - rightColumnLeft - PANEL_GUTTER;
        if (rightColumnWidth >= MIN_SECONDARY_PANEL_WIDTH) {
            int lineCount = Math.min(ROUND6_MAX_LINES, lines.size());
            int panelHeight = lineCount * LINE_HEIGHT + 6;
            int top = Math.max(
                    TOP + PROOF_HEIGHT + (PROOF_MARGIN * 2),
                    graphics.guiHeight() - panelHeight - PANEL_GUTTER
            );
            renderLinePanel(
                    graphics,
                    client,
                    lines,
                    rightColumnLeft,
                    top,
                    Math.min(360, rightColumnWidth),
                    ROUND6_MAX_LINES
            );
            return;
        }

        int belowTop = TOP + (primaryLineCount * LINE_HEIGHT) + PANEL_GUTTER + 6;
        int availableHeight = graphics.guiHeight() - belowTop - PANEL_GUTTER;
        if (availableHeight < LINE_HEIGHT + 6) {
            return;
        }
        int maxLines = Math.max(1, Math.min(ROUND6_MAX_LINES, (availableHeight - 6) / LINE_HEIGHT));
        renderLinePanel(graphics, client, lines, LEFT, belowTop, primaryWidth, maxLines);
    }

    private static int renderLinePanel(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            List<Component> lines,
            int left,
            int top,
            int maxWidth,
            int maxLines
    ) {
        int lineCount = Math.min(maxLines, lines.size());
        if (lineCount <= 0) {
            return 0;
        }
        int backgroundHeight = lineCount * LINE_HEIGHT + 6;
        graphics.fill(left - 3, top - 3, left + maxWidth + 3, top + backgroundHeight, BACKGROUND_COLOR);
        int y = top;
        for (int index = 0; index < lineCount; index++) {
            graphics.text(client.font, fitLine(client, lines.get(index), maxWidth), left, y, TEXT_COLOR);
            y += LINE_HEIGHT;
        }
        return lineCount;
    }

    private static void renderRoundEightLegendIfNeeded(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            DebugOverlay overlay
    ) {
        if (!isRoundEightHeatmapOverlay(overlay)) {
            return;
        }

        int left = Math.max(PROOF_MARGIN, graphics.guiWidth() - ROUND8_LEGEND_WIDTH - PROOF_MARGIN);
        int top = PROOF_MARGIN + PROOF_HEIGHT + PROOF_MARGIN;
        int right = left + ROUND8_LEGEND_WIDTH;
        int bottom = top + ROUND8_LEGEND_HEIGHT;
        graphics.fill(left, top, right, bottom, ROUND8_LEGEND_BACKGROUND_COLOR);
        graphics.text(client.font, Component.literal(roundEightLegendTitle(overlay)), left + 5, top + 5, 0xFFFFFFFF);

        int swatchTop = top + 19;
        int swatchLeft = left + 5;
        int[] colors = roundEightLegendColors(overlay);
        for (int index = 0; index < colors.length; index++) {
            int x0 = swatchLeft + (index * ROUND8_LEGEND_SWATCH_WIDTH);
            graphics.fill(x0, swatchTop, x0 + ROUND8_LEGEND_SWATCH_WIDTH, swatchTop + 8, colors[index]);
        }
        graphics.text(client.font, Component.literal(roundEightLegendLabels(overlay)), left + 5, top + 31, 0xFFE5F0FF);
    }

    private static boolean isRoundEightHeatmapOverlay(DebugOverlay overlay) {
        return overlay == DebugOverlay.RAY_BUDGET_HEATMAP
                || overlay == DebugOverlay.VARIANCE_MAP
                || overlay == DebugOverlay.HISTORY_CONFIDENCE
                || overlay == DebugOverlay.DISOCCLUSION_MASK;
    }

    private static String roundEightLegendTitle(DebugOverlay overlay) {
        return switch (overlay) {
            case RAY_BUDGET_HEATMAP -> "R8 ray budget";
            case VARIANCE_MAP -> "R8 variance";
            case HISTORY_CONFIDENCE -> "R8 history confidence";
            case DISOCCLUSION_MASK -> "R8 disocclusion";
            default -> "R8 adaptive";
        };
    }

    private static int[] roundEightLegendColors(DebugOverlay overlay) {
        return switch (overlay) {
            case RAY_BUDGET_HEATMAP -> new int[]{0xFF4D8CFF, 0xFF42D66B, 0xFFFFD34D, 0xFFFF5A4D, 0xFF707780};
            case VARIANCE_MAP -> new int[]{0xFF4D8CFF, 0xFF42D66B, 0xFFFFD34D, 0xFFFF5A4D, 0xFF707780};
            case HISTORY_CONFIDENCE -> new int[]{0xFFFF5A4D, 0xFFFFD34D, 0xFF42D66B, 0xFF4D8CFF, 0xFF707780};
            case DISOCCLUSION_MASK -> new int[]{0x40203038, 0xFF42D66B, 0xFFFFD34D, 0xFFFF5A4D, 0xFF707780};
            default -> new int[]{0xFF707780, 0xFF42D66B, 0xFFFFD34D, 0xFFFF5A4D, 0xFF4D8CFF};
        };
    }

    private static String roundEightLegendLabels(DebugOverlay overlay) {
        return switch (overlay) {
            case RAY_BUDGET_HEATMAP -> "reuse low med high ?";
            case VARIANCE_MAP -> "low ok med high ?";
            case HISTORY_CONFIDENCE -> "reset low ok high ?";
            case DISOCCLUSION_MASK -> "still ok move reset ?";
            default -> "missing ok med high ready";
        };
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
        if (proofOverlaysHiddenForValidation()) {
            return false;
        }
        if (!ProofVisualMode.directLightProofOverlayAllowed(snapshot.debugOverlay())) {
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
        if (proofOverlaysHiddenForValidation()) {
            return false;
        }
        if (!ProofVisualMode.round6GiProofOverlayAllowed(snapshot.debugOverlay())) {
            return false;
        }
        return LucernaController.getInstance().round6DiffuseGiCpuOutputPayload().readyForPreviewDraw();
    }

    private static boolean proofOverlaysHiddenForValidation() {
        return ProofVisualMode.proofOverlaysHidden();
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
