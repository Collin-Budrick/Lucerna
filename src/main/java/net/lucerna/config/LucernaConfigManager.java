package net.lucerna.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.lucerna.Lucerna;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LucernaConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("lucerna.json");
    private LucernaConfig config = LucernaConfig.defaults();

    public synchronized LucernaConfig config() {
        return this.config;
    }

    public synchronized void load() {
        if (!Files.exists(this.configPath)) {
            this.save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.configPath)) {
            LoadedConfig loaded = this.readConfig(reader);
            this.config = loaded.config();
            if (loaded.needsRewrite()) {
                this.save();
            }
        } catch (IOException | RuntimeException exception) {
            Lucerna.LOGGER.warn("Could not load Lucerna config; using defaults.", exception);
            this.config = LucernaConfig.defaults();
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(this.configPath.getParent());
            Path tempPath = this.configPath.resolveSibling(this.configPath.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempPath)) {
                GSON.toJson(this.writeConfig(this.config.normalized()), writer);
            }
            this.replaceConfig(tempPath);
        } catch (IOException exception) {
            Lucerna.LOGGER.warn("Could not save Lucerna config.", exception);
        }
    }

    public synchronized void setConfig(LucernaConfig config) {
        this.config = (config == null ? LucernaConfig.defaults() : config).normalized();
        this.save();
    }

    public void setRendererEnabled(boolean enabled) {
        this.setConfig(this.config().withRendererEnabled(enabled));
    }

    public void setQualityPreset(QualityPreset preset) {
        this.setConfig(this.config().withQualityPreset(preset));
    }

    public void setDebugOverlay(DebugOverlay overlay) {
        this.setConfig(this.config().withDebugOverlay(overlay));
    }

    public void setShowIrisNotice(boolean show) {
        this.setConfig(this.config().withShowIrisNotice(show));
    }

    public void cycleQualityPreset() {
        this.setQualityPreset(this.config().qualityPreset().next());
    }

    public void cycleDebugOverlay() {
        this.setDebugOverlay(this.config().debugOverlay().next());
    }

    private LoadedConfig readConfig(Reader reader) {
        JsonElement element = JsonParser.parseReader(reader);
        if (element == null || !element.isJsonObject()) {
            return new LoadedConfig(LucernaConfig.defaults(), true);
        }

        JsonObject object = element.getAsJsonObject();
        LucernaConfig defaults = LucernaConfig.defaults();
        boolean needsRewrite = false;

        ReadValue<Integer> schemaVersion = this.readInt(object, "schemaVersion", defaults.schemaVersion());
        needsRewrite = schemaVersion.usedFallback() || schemaVersion.value() != LucernaConfig.CURRENT_SCHEMA_VERSION;

        ReadValue<Boolean> rendererEnabled = this.readBoolean(object, "rendererEnabled", defaults.rendererEnabled());
        ReadValue<QualityPreset> qualityPreset = this.readQualityPreset(object, "qualityPreset", defaults.qualityPreset());
        ReadValue<DebugOverlay> debugOverlay = this.readDebugOverlay(object, "debugOverlay", defaults.debugOverlay());
        ReadValue<Boolean> showIrisNotice = this.readBoolean(object, "showIrisNotice", defaults.showIrisNotice());

        needsRewrite = needsRewrite
                || rendererEnabled.usedFallback()
                || qualityPreset.usedFallback()
                || debugOverlay.usedFallback()
                || showIrisNotice.usedFallback();

        return new LoadedConfig(new LucernaConfig(
                schemaVersion.value(),
                rendererEnabled.value(),
                qualityPreset.value(),
                debugOverlay.value(),
                showIrisNotice.value()
        ).normalized(), needsRewrite);
    }

    private JsonObject writeConfig(LucernaConfig config) {
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", config.schemaVersion());
        object.addProperty("rendererEnabled", config.rendererEnabled());
        object.addProperty("qualityPreset", config.qualityPreset().name());
        object.addProperty("debugOverlay", config.debugOverlay().name());
        object.addProperty("showIrisNotice", config.showIrisNotice());
        return object;
    }

    private void replaceConfig(Path tempPath) throws IOException {
        try {
            Files.move(tempPath, this.configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempPath, this.configPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            try {
                Files.move(tempPath, this.configPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackException) {
                exception.addSuppressed(fallbackException);
                throw exception;
            }
        }
    }

    private ReadValue<Integer> readInt(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return new ReadValue<>(fallback, true);
        }

        try {
            return new ReadValue<>(element.getAsInt(), false);
        } catch (RuntimeException exception) {
            return new ReadValue<>(fallback, true);
        }
    }

    private ReadValue<Boolean> readBoolean(JsonObject object, String name, boolean fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            return new ReadValue<>(fallback, true);
        }

        try {
            return new ReadValue<>(element.getAsBoolean(), false);
        } catch (RuntimeException exception) {
            return new ReadValue<>(fallback, true);
        }
    }

    private ReadValue<QualityPreset> readQualityPreset(JsonObject object, String name, QualityPreset fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return new ReadValue<>(fallback, true);
        }

        String serialized = element.getAsString();
        QualityPreset parsed = QualityPreset.fromSerializedName(serialized, fallback);
        return new ReadValue<>(parsed, parsed == fallback && !fallback.name().equalsIgnoreCase(serialized));
    }

    private ReadValue<DebugOverlay> readDebugOverlay(JsonObject object, String name, DebugOverlay fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return new ReadValue<>(fallback, true);
        }

        String serialized = element.getAsString();
        DebugOverlay parsed = DebugOverlay.fromSerializedName(serialized, fallback);
        return new ReadValue<>(parsed, parsed == fallback && !fallback.name().equalsIgnoreCase(serialized));
    }

    private record LoadedConfig(LucernaConfig config, boolean needsRewrite) {
    }

    private record ReadValue<T>(T value, boolean usedFallback) {
    }
}
