package net.lucerna.world.hooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Objects;

public final class LucernaClientWorldStateTracker {
    private static final long TIME_OF_DAY_EVENT_INTERVAL_TICKS = 20L;

    private final LucernaWorldFeedAdapter adapter;
    private boolean worldJoined;
    private String lastDimension;
    private boolean weatherKnown;
    private boolean lastRaining;
    private boolean lastThundering;
    private long lastTimeBucket = Long.MIN_VALUE;

    public LucernaClientWorldStateTracker(LucernaWorldFeedAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public void onPlayReady(Minecraft client) {
        Objects.requireNonNull(client, "client");
        if (client.level != null) {
            this.onLevelAvailable(client.level);
        }
    }

    public void onLevelChanged(ClientLevel level) {
        Objects.requireNonNull(level, "level");
        this.onLevelAvailable(level);
    }

    public void onPlayDisconnected(Minecraft client) {
        Objects.requireNonNull(client, "client");
        this.markWorldLeft(client.level);
    }

    public void onClientTick(Minecraft client) {
        Objects.requireNonNull(client, "client");
        if (client.level == null) {
            this.markWorldLeft(null);
        } else if (!this.worldJoined) {
            this.onLevelAvailable(client.level);
        }
    }

    public void onLevelTick(ClientLevel level) {
        Objects.requireNonNull(level, "level");
        this.onLevelAvailable(level);
        this.trackWeather(level);
        this.trackTimeOfDay(level);
    }

    private void markWorldLeft(ClientLevel level) {
        if (this.worldJoined) {
            if (level != null) {
                this.adapter.markWorldLeft(level);
            } else if (this.lastDimension != null) {
                this.adapter.markWorldLeft(this.lastDimension);
            }
        }
        this.reset();
    }

    private void onLevelAvailable(ClientLevel level) {
        String dimension = this.adapter.dimensionId(level);
        if (!this.worldJoined) {
            this.worldJoined = true;
            this.lastDimension = dimension;
            this.adapter.markWorldJoined(level);
            this.captureWeather(level);
            this.captureTimeOfDay(level);
            return;
        }

        if (!dimension.equals(this.lastDimension)) {
            this.lastDimension = dimension;
            this.adapter.markDimensionChanged(level);
            this.captureWeather(level);
            this.captureTimeOfDay(level);
        }
    }

    private void trackWeather(ClientLevel level) {
        boolean raining = level.isRaining();
        boolean thundering = level.isThundering();
        if (!this.weatherKnown) {
            this.lastRaining = raining;
            this.lastThundering = thundering;
            this.weatherKnown = true;
            return;
        }

        if (raining != this.lastRaining || thundering != this.lastThundering) {
            this.lastRaining = raining;
            this.lastThundering = thundering;
            this.adapter.markWeatherChanged(level);
        }
    }

    private void trackTimeOfDay(ClientLevel level) {
        long timeBucket = this.timeBucket(level);
        if (this.lastTimeBucket == Long.MIN_VALUE) {
            this.lastTimeBucket = timeBucket;
            return;
        }

        if (timeBucket != this.lastTimeBucket) {
            this.lastTimeBucket = timeBucket;
            this.adapter.markTimeOfDayChanged(level);
        }
    }

    private void captureWeather(ClientLevel level) {
        this.lastRaining = level.isRaining();
        this.lastThundering = level.isThundering();
        this.weatherKnown = true;
    }

    private void captureTimeOfDay(ClientLevel level) {
        this.lastTimeBucket = this.timeBucket(level);
    }

    private long timeBucket(ClientLevel level) {
        return Math.floorDiv(level.getLevelData().getGameTime(), TIME_OF_DAY_EVENT_INTERVAL_TICKS);
    }

    private void reset() {
        this.worldJoined = false;
        this.lastDimension = null;
        this.weatherKnown = false;
        this.lastRaining = false;
        this.lastThundering = false;
        this.lastTimeBucket = Long.MIN_VALUE;
    }
}
