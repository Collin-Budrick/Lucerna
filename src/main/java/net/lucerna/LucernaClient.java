package net.lucerna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.lucerna.world.hooks.LucernaWorldEventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

public final class LucernaClient implements ClientModInitializer {
    private static final boolean CONTROLLER_SCREENSHOT_REQUESTED =
            Boolean.parseBoolean(System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_REQUEST", "false"));
    private static final int CONTROLLER_SCREENSHOT_DELAY_TICKS =
            parseControllerScreenshotDelayTicks();

    private static int controllerScreenshotReadyTicks;
    private static boolean controllerScreenshotCaptured;

    @Override
    public void onInitializeClient() {
        LucernaController controller = LucernaController.getInstance();
        controller.initialize();
        LucernaWorldEventHooks.register(controller);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            controller.onViewportChanged(client.getWindow().getWidth(), client.getWindow().getHeight());
            controller.tick();
            captureControllerScreenshotIfRequested(client);
        });
        LevelExtractionEvents.END_EXTRACTION.register(context -> controller.captureFrameConstants(
                context,
                context.deltaTracker().getGameTimeDeltaPartialTick(false)
        ));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> controller.shutdown());
    }

    private static void captureControllerScreenshotIfRequested(Minecraft client) {
        if (!CONTROLLER_SCREENSHOT_REQUESTED || controllerScreenshotCaptured) {
            return;
        }
        if (client.level == null || client.player == null) {
            controllerScreenshotReadyTicks = 0;
            return;
        }

        controllerScreenshotReadyTicks++;
        if (controllerScreenshotReadyTicks < CONTROLLER_SCREENSHOT_DELAY_TICKS) {
            return;
        }

        controllerScreenshotCaptured = true;
        Lucerna.LOGGER.info(
                "Lucerna controller in-client screenshot requested: delayTicks={} playerReady=true.",
                CONTROLLER_SCREENSHOT_DELAY_TICKS
        );
        Screenshot.grab(client, false);
    }

    private static int parseControllerScreenshotDelayTicks() {
        String rawDelay = System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_DELAY_TICKS", "160");
        try {
            return Math.max(1, Integer.parseInt(rawDelay));
        } catch (NumberFormatException ignored) {
            return 160;
        }
    }
}
