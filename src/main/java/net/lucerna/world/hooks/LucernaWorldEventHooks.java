package net.lucerna.world.hooks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.lucerna.Lucerna;
import net.lucerna.LucernaController;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Objects;

public final class LucernaWorldEventHooks {
    private static boolean registered;

    private LucernaWorldEventHooks() {
    }

    public static synchronized void register(LucernaController controller) {
        Objects.requireNonNull(controller, "controller");
        if (registered) {
            return;
        }

        LucernaWorldFeedAdapter adapter = new LucernaWorldFeedAdapter(controller.worldFeed());
        LucernaClientWorldStateTracker tracker = new LucernaClientWorldStateTracker(adapter);

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> tracker.onPlayReady(client));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> tracker.onPlayDisconnected(client));
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (level != null) {
                tracker.onLevelChanged(level);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(tracker::onClientTick);
        ClientTickEvents.END_LEVEL_TICK.register(tracker::onLevelTick);

        ClientChunkEvents.CHUNK_LOAD.register(adapter::markChunkLoaded);
        ClientChunkEvents.CHUNK_UNLOAD.register(adapter::markChunkUnloaded);
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> adapter.markBlockEntityChanged(level, blockEntity));
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> adapter.markBlockEntityChanged(level, blockEntity));

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Lucerna.id("world_feed_reload"),
                (ResourceManagerReloadListener) resourceManager -> adapter.markResourcePackReloaded()
        );

        registered = true;
    }
}
