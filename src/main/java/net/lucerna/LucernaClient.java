package net.lucerna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class LucernaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LucernaController controller = LucernaController.getInstance();
        controller.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> controller.tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> controller.shutdown());
    }
}
