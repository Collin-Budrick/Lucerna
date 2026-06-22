package net.lucerna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.lucerna.world.hooks.LucernaWorldEventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LucernaClient implements ClientModInitializer {
    private static final boolean CONTROLLER_SCREENSHOT_REQUESTED =
            Boolean.parseBoolean(System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_REQUEST", "false"));
    private static final boolean CONTROLLER_SCREENSHOT_AUTO_AFTER_SCENE =
            Boolean.parseBoolean(System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_AUTO_AFTER_SCENE", "false"));
    private static final int CONTROLLER_SCREENSHOT_DELAY_TICKS =
            parseControllerScreenshotDelayTicks();
    private static final Path CONTROLLER_SCREENSHOT_TRIGGER_FILE =
            parseControllerScreenshotTriggerFile();
    private static final String CONTROLLER_SCREENSHOT_SCENE_SETUP =
            System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_SCENE_SETUP", "").trim();
    private static final boolean CONTROLLER_SCENE_COMMAND_SETUP_ALLOWED =
            Boolean.parseBoolean(System.getenv().getOrDefault("LUCERNA_ALLOW_SCENE_COMMAND_SETUP", "false"));

    private static int controllerScreenshotReadyTicks;
    private static int controllerScreenshotGameplayTicks;
    private static int controllerScreenshotSceneCommandIndex;
    private static int controllerScreenshotSceneCommandCooldown;
    private static int controllerScreenshotSceneSettleTicks;
    private static boolean controllerScreenshotSceneSetupComplete;
    private static boolean controllerScreenshotPendingCapture;
    private static Boolean controllerScreenshotPreviousHideGui;
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
        if (!CONTROLLER_SCREENSHOT_REQUESTED || controllerScreenshotCaptured || controllerScreenshotPendingCapture) {
            return;
        }
        if (CONTROLLER_SCREENSHOT_TRIGGER_FILE != null
                && !CONTROLLER_SCREENSHOT_AUTO_AFTER_SCENE
                && !Files.exists(CONTROLLER_SCREENSHOT_TRIGGER_FILE)) {
            return;
        }
        clearControllerScreenshotChat(client);
        if (client.level == null || client.player == null) {
            controllerScreenshotReadyTicks = 0;
            controllerScreenshotGameplayTicks = 0;
            return;
        }

        controllerScreenshotReadyTicks++;
        if (controllerScreenshotReadyTicks < CONTROLLER_SCREENSHOT_DELAY_TICKS) {
            return;
        }
        if (client.gui.screen() != null) {
            String screenName = client.gui.screen().getClass().getName();
            client.gui.setScreen(null);
            controllerScreenshotGameplayTicks = 0;
            Lucerna.LOGGER.info(
                    "Lucerna controller in-client screenshot deferred: closing active screen {} before gameplay capture.",
                    screenName
            );
            return;
        }
        if (!controllerScreenshotSceneSetupComplete(client)) {
            controllerScreenshotGameplayTicks = 0;
            return;
        }

        controllerScreenshotGameplayTicks++;
        if (controllerScreenshotGameplayTicks < 40) {
            return;
        }

        clearControllerScreenshotChat(client);
        controllerScreenshotPendingCapture = true;
        controllerScreenshotPreviousHideGui = setHideGuiForControllerScreenshot(client, true);
        Lucerna.LOGGER.info(
                "Lucerna controller in-client screenshot queued: delayTicks={} playerReady=true screenOpen=false gameplayTicks={} menuScreenshotRejected=true capturePhase=endMain hideGuiPreArmed={}.",
                CONTROLLER_SCREENSHOT_DELAY_TICKS,
                controllerScreenshotGameplayTicks,
                controllerScreenshotPreviousHideGui != null
        );
    }

    static void capturePendingControllerScreenshotAfterLucernaComposite(boolean lucernaCompositeSubmitted) {
        capturePendingControllerScreenshotFromMainTarget(Minecraft.getInstance(), lucernaCompositeSubmitted);
    }

    private static void capturePendingControllerScreenshotFromMainTarget(Minecraft client, boolean lucernaCompositeSubmitted) {
        if (!controllerScreenshotPendingCapture || controllerScreenshotCaptured) {
            return;
        }
        if (client.level == null || client.player == null || client.gui.screen() != null) {
            restoreControllerScreenshotHideGui(client);
            controllerScreenshotPendingCapture = false;
            controllerScreenshotGameplayTicks = 0;
            return;
        }

        controllerScreenshotCaptured = true;
        controllerScreenshotPendingCapture = false;
        clearControllerScreenshotChat(client);
        Lucerna.LOGGER.info(
                "Lucerna controller in-client screenshot requested: capturePhase=afterLucernaComposite playerReady=true screenOpen=false gameplayTicks={} mainRenderTargetBeforeHud=true lucernaCompositeSubmitted={} menuScreenshotRejected=true.",
                controllerScreenshotGameplayTicks,
                lucernaCompositeSubmitted
        );
        try {
            Screenshot.grab(client, false);
        } finally {
            restoreControllerScreenshotHideGui(client);
        }
    }

    private static void restoreControllerScreenshotHideGui(Minecraft client) {
        if (controllerScreenshotPreviousHideGui != null) {
            setHideGuiForControllerScreenshot(client, controllerScreenshotPreviousHideGui);
            controllerScreenshotPreviousHideGui = null;
        }
    }

    private static Boolean setHideGuiForControllerScreenshot(Minecraft client, boolean value) {
        for (String fieldName : List.of("hideGui", "field_1842", "Y")) {
            try {
                Field field = client.options.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                boolean previousValue = field.getBoolean(client.options);
                field.setBoolean(client.options, value);
                return previousValue;
            } catch (ReflectiveOperationException ignored) {
                // Try the next namespace candidate; the active dev/runtime mappings vary here.
            }
        }
        Lucerna.LOGGER.warn("Lucerna controller screenshot could not force hideGui; no known Options field matched.");
        return null;
    }

    private static void clearControllerScreenshotChat(Minecraft client) {
        client.gui.hud.getChat().clearMessages(true);
        client.gui.hud.clearCache();
    }

    private static boolean controllerScreenshotSceneSetupComplete(Minecraft client) {
        if (CONTROLLER_SCREENSHOT_SCENE_SETUP.isBlank()) {
            return true;
        }
        if (controllerScreenshotSceneSetupComplete) {
            return true;
        }

        List<String> commands = controllerScreenshotSceneCommands();
        if (commands.isEmpty()) {
            controllerScreenshotSceneSetupComplete = true;
            return true;
        }

        if (controllerScreenshotSceneCommandIndex < commands.size()) {
            if (controllerScreenshotSceneCommandCooldown > 0) {
                controllerScreenshotSceneCommandCooldown--;
                return false;
            }
            String command = commands.get(controllerScreenshotSceneCommandIndex);
            client.player.connection.sendCommand(command);
            controllerScreenshotSceneCommandIndex++;
            controllerScreenshotSceneCommandCooldown = 0;
            Lucerna.LOGGER.info(
                    "Lucerna controller screenshot scene command submitted: index={}/{} command={}.",
                    controllerScreenshotSceneCommandIndex,
                    commands.size(),
                    command
            );
            return false;
        }

        controllerScreenshotSceneSettleTicks++;
        if (controllerScreenshotSceneSettleTicks < 80) {
            return false;
        }

        controllerScreenshotSceneSetupComplete = true;
        Lucerna.LOGGER.info(
                "Lucerna controller screenshot scene setup complete: scene={} commands={} settleTicks={}.",
                CONTROLLER_SCREENSHOT_SCENE_SETUP,
                commands.size(),
                controllerScreenshotSceneSettleTicks
        );
        return true;
    }

    private static List<String> controllerScreenshotSceneCommands() {
        if (!CONTROLLER_SCENE_COMMAND_SETUP_ALLOWED) {
            return List.of();
        }
        if ("real-renderer-milestone1".equals(CONTROLLER_SCREENSHOT_SCENE_SETUP)) {
            return List.of(
                    "gamerule sendCommandFeedback false",
                    "gamerule doDaylightCycle false",
                    "gamerule doWeatherCycle false",
                    "gamemode creative",
                    "difficulty peaceful",
                    "gamerule doMobSpawning false",
                    "weather clear",
                    "time set 6000",
                    "effect clear @s",
                    "kill @e[type=!player,distance=..96]",
                    "fill -18 180 -40 18 193 -4 minecraft:air",
                    "fill -16 179 -38 16 179 -6 minecraft:smooth_stone",
                    "fill -16 180 -7 16 187 -7 minecraft:light_gray_concrete",
                    "fill -17 180 -36 -17 184 -8 minecraft:smooth_stone",
                    "fill 17 180 -36 17 184 -8 minecraft:smooth_stone",
                    "fill -15 179 -30 -3 179 -18 minecraft:white_concrete",
                    "fill 3 179 -30 15 179 -18 minecraft:white_concrete",
                    "fill -15 180 -18 -3 183 -18 minecraft:white_concrete",
                    "fill 3 180 -18 15 183 -18 minecraft:white_concrete",
                    "fill -15 180 -10 -11 184 -10 minecraft:red_concrete",
                    "fill -10 180 -10 -6 184 -10 minecraft:orange_concrete",
                    "fill -5 180 -10 -2 184 -10 minecraft:magenta_concrete",
                    "fill 2 180 -10 5 184 -10 minecraft:cyan_concrete",
                    "fill 6 180 -10 10 184 -10 minecraft:lime_concrete",
                    "fill 11 180 -10 15 184 -10 minecraft:yellow_concrete",
                    "fill -16 180 -29 -16 184 -21 minecraft:blue_concrete",
                    "fill 16 180 -29 16 184 -21 minecraft:red_concrete",
                    "fill -2 180 -27 -2 183 -20 minecraft:cyan_concrete",
                    "fill 2 180 -27 2 183 -20 minecraft:orange_concrete",
                    "setblock -13 181 -20 minecraft:redstone_lamp[lit=true]",
                    "setblock -8 181 -20 minecraft:glowstone",
                    "setblock -4 181 -20 minecraft:sea_lantern",
                    "setblock 4 181 -20 minecraft:sea_lantern",
                    "setblock 8 181 -20 minecraft:glowstone",
                    "setblock 13 181 -20 minecraft:redstone_lamp[lit=true]",
                    "setblock -12 180 -24 minecraft:redstone_lamp[lit=true]",
                    "setblock -6 180 -24 minecraft:sea_lantern",
                    "setblock 6 180 -24 minecraft:sea_lantern",
                    "setblock 12 180 -24 minecraft:redstone_lamp[lit=true]",
                    "fill -14 180 -23 -12 181 -21 minecraft:orange_concrete",
                    "fill 12 180 -23 14 181 -21 minecraft:cyan_concrete",
                    "fill -8 180 -28 -5 181 -26 minecraft:magenta_concrete",
                    "fill 5 180 -28 8 181 -26 minecraft:lime_concrete",
                    "fill -7 180 -23 -5 180 -21 minecraft:yellow_concrete",
                    "fill 5 180 -23 7 180 -21 minecraft:blue_concrete",
                    "fill -7 180 -25 -5 181 -23 minecraft:magenta_concrete",
                    "fill 5 180 -25 7 181 -23 minecraft:lime_concrete",
                    "fill -5 180 -22 -3 181 -20 minecraft:cyan_concrete",
                    "fill 3 180 -22 5 181 -20 minecraft:orange_concrete",
                    "fill -12 180 -27 -12 183 -27 minecraft:polished_deepslate",
                    "fill 12 180 -27 12 183 -27 minecraft:polished_deepslate",
                    "fill -5 180 -26 -4 182 -26 minecraft:dark_oak_fence",
                    "fill 4 180 -26 5 182 -26 minecraft:dark_oak_fence",
                    "setblock -10 180 -22 minecraft:cobblestone_wall",
                    "setblock 10 180 -22 minecraft:cobblestone_wall",
                    "fill -15 180 -33 -13 182 -33 minecraft:copper_block",
                    "fill 13 180 -33 15 182 -33 minecraft:copper_block",
                    "fill -1 180 -34 1 184 -28 minecraft:air",
                    "weather clear",
                    "time set 6000",
                    "tp @s 0 182.05 -36 0 7"
            );
        }
        return List.of();
    }

    private static int parseControllerScreenshotDelayTicks() {
        String rawDelay = System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_DELAY_TICKS", "160");
        try {
            return Math.max(1, Integer.parseInt(rawDelay));
        } catch (NumberFormatException ignored) {
            return 160;
        }
    }

    private static Path parseControllerScreenshotTriggerFile() {
        String rawPath = System.getenv("LUCERNA_CONTROLLER_SCREENSHOT_TRIGGER_FILE");
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        return Path.of(rawPath.trim());
    }
}
