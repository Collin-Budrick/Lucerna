package net.lucerna.render.gbuffer;

import java.util.Objects;
import java.util.Optional;

public enum GBufferWriteSemantic {
    DEPTH(GBufferTargetContract.DEPTH, "depth-compatible visibility"),
    NORMAL_ROUGHNESS(GBufferTargetContract.NORMAL_ROUGHNESS, "surface normal and roughness"),
    ALBEDO_OPACITY(GBufferTargetContract.ALBEDO_OPACITY, "base color and opacity"),
    MATERIAL_ID(GBufferTargetContract.MATERIAL_ID, "material table lookup id"),
    EMISSIVE(GBufferTargetContract.EMISSIVE, "emissive radiance seed"),
    MOTION_HISTORY(GBufferTargetContract.MOTION_HISTORY, "motion vector and history confidence");

    private final String attachmentName;
    private final String description;

    GBufferWriteSemantic(String attachmentName, String description) {
        this.attachmentName = attachmentName;
        this.description = description;
    }

    public String attachmentName() {
        return this.attachmentName;
    }

    public String description() {
        return this.description;
    }

    public static Optional<GBufferWriteSemantic> fromAttachmentName(String attachmentName) {
        Objects.requireNonNull(attachmentName, "attachmentName");
        for (GBufferWriteSemantic semantic : values()) {
            if (semantic.attachmentName.equals(attachmentName)) {
                return Optional.of(semantic);
            }
        }
        return Optional.empty();
    }
}
