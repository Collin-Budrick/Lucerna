package net.lucerna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.lucerna.world.hooks.LucernaWorldEventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LucernaClient implements ClientModInitializer {
    private static final boolean CONTROLLER_SCREENSHOT_REQUESTED =
            Boolean.parseBoolean(System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_REQUEST", "false"));
    private static final int CONTROLLER_SCREENSHOT_DELAY_TICKS =
            parseControllerScreenshotDelayTicks();
    private static final Path CONTROLLER_SCREENSHOT_TRIGGER_FILE =
            parseControllerScreenshotTriggerFile();
    private static final String CONTROLLER_SCREENSHOT_SCENE_SETUP =
            System.getenv().getOrDefault("LUCERNA_CONTROLLER_SCREENSHOT_SCENE_SETUP", "").trim();

    private static int controllerScreenshotReadyTicks;
    private static int controllerScreenshotGameplayTicks;
    private static int controllerScreenshotSceneCommandIndex;
    private static int controllerScreenshotSceneCommandCooldown;
    private static int controllerScreenshotSceneSettleTicks;
    private static boolean controllerScreenshotSceneSetupComplete;
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
        if (CONTROLLER_SCREENSHOT_TRIGGER_FILE != null && !Files.exists(CONTROLLER_SCREENSHOT_TRIGGER_FILE)) {
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
        if (controllerScreenshotGameplayTicks < 10) {
            return;
        }

        controllerScreenshotCaptured = true;
        clearControllerScreenshotChat(client);
        Lucerna.LOGGER.info(
                "Lucerna controller in-client screenshot requested: delayTicks={} playerReady=true screenOpen=false gameplayTicks={} menuScreenshotRejected=true.",
                CONTROLLER_SCREENSHOT_DELAY_TICKS,
                controllerScreenshotGameplayTicks
        );
        Screenshot.grab(client, false);
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
        if (controllerScreenshotSceneSettleTicks < 10) {
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
                    "fill -16 180 -34 16 191 -2 minecraft:air",
                    "fill -16 179 -34 16 179 -2 minecraft:smooth_stone",
                    "fill -16 180 -2 16 188 -2 minecraft:smooth_stone",
                    "fill -14 180 -3 -8 185 -3 minecraft:red_concrete",
                    "fill -7 180 -3 -1 185 -3 minecraft:blue_concrete",
                    "fill 1 180 -3 7 185 -3 minecraft:lime_concrete",
                    "fill 8 180 -3 14 185 -3 minecraft:yellow_concrete",
                    "setblock -11 183 -5 minecraft:glowstone",
                    "setblock 0 183 -5 minecraft:sea_lantern",
                    "setblock 11 183 -5 minecraft:redstone_lamp[lit=true]",
                    "fill -5 180 -14 -5 186 -14 minecraft:oak_log",
                    "fill 5 180 -14 5 186 -14 minecraft:spruce_log",
                    "fill -9 185 -20 -1 188 -12 minecraft:oak_leaves",
                    "fill 1 185 -20 9 188 -12 minecraft:spruce_leaves",
                    "fill -13 180 -25 -7 183 -25 minecraft:air",
                    "fill 7 180 -25 13 181 -25 minecraft:air",
                    "fill -13 179 -25 -7 179 -25 minecraft:smooth_stone",
                    "fill 7 179 -25 13 179 -25 minecraft:smooth_stone",
                    "fill -2 180 -18 2 183 -18 minecraft:copper_block",
                    "setblock -7 180 -11 minecraft:cobblestone_wall",
                    "setblock 7 180 -11 minecraft:cobblestone_wall",
                    "weather clear",
                    "time set 6000",
                    "tp @s 0 181.55 -30 0 6"
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
