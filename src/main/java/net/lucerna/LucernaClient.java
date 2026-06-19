package net.lucerna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.lucerna.world.hooks.LucernaWorldEventHooks;

public final class LucernaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LucernaController controller = LucernaController.getInstance();
        controller.initialize();
        LucernaWorldEventHooks.register(controller);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            controller.onViewportChanged(client.getWindow().getWidth(), client.getWindow().getHeight());
            controller.tick();
        });
        LevelExtractionEvents.END_EXTRACTION.register(context -> controller.captureFrameConstants(
                context,
                context.deltaTracker().getGameTimeDeltaPartialTick(false)
        ));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> controller.shutdown());
    }
}
