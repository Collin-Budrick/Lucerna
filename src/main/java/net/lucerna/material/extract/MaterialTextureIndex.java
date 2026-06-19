package net.lucerna.material.extract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MaterialTextureIndex {
    public static final int UNKNOWN_TEXTURE_INDEX = 0;
    public static final String UNKNOWN_TEXTURE_ID = "";

    private int nextIndex = 1;
    private final Map<String, Integer> textureIndices = new LinkedHashMap<>();

    public synchronized int getOrCreate(String textureId) {
        String normalizedTextureId = normalize(textureId);
        if (normalizedTextureId.isEmpty()) {
            return UNKNOWN_TEXTURE_INDEX;
        }

        Integer existing = this.textureIndices.get(normalizedTextureId);
        if (existing != null) {
            return existing;
        }

        int created = this.nextIndex++;
        this.textureIndices.put(normalizedTextureId, created);
        return created;
    }

    public synchronized Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.textureIndices));
    }

    public synchronized int textureCount() {
        return this.textureIndices.size();
    }

    static String normalize(String textureId) {
        return Objects.requireNonNullElse(textureId, UNKNOWN_TEXTURE_ID).trim();
    }
}
