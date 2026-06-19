package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.telemetry.LucernaStatusSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LucernaSettingsScreen extends Screen {
    private final Screen parent;

    public LucernaSettingsScreen(Screen parent) {
        super(Component.translatable("lucerna.options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LucernaController controller = LucernaController.getInstance();
        var config = controller.getConfig();
        int centerX = this.width / 2;
        int y = 98;

        this.addRenderableWidget(Button.builder(Component.literal("Renderer: " + (config.rendererEnabled() ? "On" : "Off")), button -> {
            var manager = controller.configManager();
            manager.setRendererEnabled(!manager.config().rendererEnabled());
            Minecraft.getInstance().gui.setScreen(new LucernaSettingsScreen(this.parent));
        }).bounds(centerX - 155, y, 310, 20).build());

        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Quality: " + config.qualityPreset().displayName()), button -> {
            var manager = controller.configManager();
            manager.cycleQualityPreset();
            Minecraft.getInstance().gui.setScreen(new LucernaSettingsScreen(this.parent));
        }).bounds(centerX - 155, y, 310, 20).build());

        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Debug: " + config.debugOverlay().displayName()), button -> {
            var manager = controller.configManager();
            manager.cycleDebugOverlay();
            Minecraft.getInstance().gui.setScreen(new LucernaSettingsScreen(this.parent));
        }).bounds(centerX - 155, y, 310, 20).build());

        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Iris notice: " + (config.showIrisNotice() ? "Show" : "Hide")), button -> {
            var manager = controller.configManager();
            manager.setShowIrisNotice(!manager.config().showIrisNotice());
            Minecraft.getInstance().gui.setScreen(new LucernaSettingsScreen(this.parent));
        }).bounds(centerX - 155, y, 310, 20).build());

        y += 34;
        int doneY = this.height >= 292 ? this.height - 32 : y;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(centerX - 75, doneY, 150, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        LucernaController controller = LucernaController.getInstance();
        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(controller);

        graphics.centeredText(this.font, this.title, this.width / 2, 32, 0xFFFFFFFF);
        int y = 52;
        for (Component line : LucernaDebugOverlayLines.settingsSummary(snapshot)) {
            graphics.centeredText(this.font, this.fitLine(line), this.width / 2, y, 0xFFB8C7D9);
            y += 12;
        }

        if (this.height >= 292) {
            y = 214;
            int lastOverlayY = this.height - 44;
            for (Component line : LucernaDebugOverlayLines.selectedOverlay(snapshot)) {
                if (y > lastOverlayY) {
                    break;
                }
                graphics.centeredText(this.font, this.fitLine(line), this.width / 2, y, 0xFF9FC5FF);
                y += 12;
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private Component fitLine(Component line) {
        int maxWidth = Math.max(40, this.width - 24);
        String text = line.getString();
        if (this.font.width(text) <= maxWidth) {
            return line;
        }

        String suffix = "...";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return Component.literal(text.substring(0, end) + suffix);
    }
}
