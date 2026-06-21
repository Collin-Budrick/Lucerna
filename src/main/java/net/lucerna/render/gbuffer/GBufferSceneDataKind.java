package net.lucerna.render.gbuffer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public enum GBufferSceneDataKind {
    DEPTH("depth", GBufferWriteSemantic.DEPTH, true),
    NORMAL("normal", GBufferWriteSemantic.NORMAL_ROUGHNESS, true),
    MATERIAL("material", GBufferWriteSemantic.MATERIAL_ID, true),
    ALBEDO("albedo", GBufferWriteSemantic.ALBEDO_OPACITY, true),
    EMISSIVE("emissive", GBufferWriteSemantic.EMISSIVE, true),
    MOTION_HISTORY("motionHistory", GBufferWriteSemantic.MOTION_HISTORY, false);

    private static final List<GBufferSceneDataKind> LIGHTING_REQUIRED = List.of(
            DEPTH,
            NORMAL,
            MATERIAL,
            ALBEDO,
            EMISSIVE
    );
    private static final List<GBufferSceneDataKind> FRAME_READINESS_TRACKED = List.of(
            DEPTH,
            NORMAL,
            MATERIAL,
            MOTION_HISTORY,
            ALBEDO,
            EMISSIVE
    );
    private static final List<GBufferSceneDataKind> PHYSICAL_GI_SHADOW_SAMPLING_REQUIRED = List.of(
            DEPTH,
            NORMAL,
            MATERIAL,
            MOTION_HISTORY
    );

    private final String label;
    private final GBufferWriteSemantic semantic;
    private final boolean requiredForLighting;

    GBufferSceneDataKind(String label, GBufferWriteSemantic semantic, boolean requiredForLighting) {
        this.label = label;
        this.semantic = semantic;
        this.requiredForLighting = requiredForLighting;
    }

    public String label() {
        return this.label;
    }

    public GBufferWriteSemantic semantic() {
        return this.semantic;
    }

    public String attachmentName() {
        return this.semantic.attachmentName();
    }

    public String description() {
        return this.semantic.description();
    }

    public boolean requiredForLighting() {
        return this.requiredForLighting;
    }

    public static List<GBufferSceneDataKind> lightingRequired() {
        return LIGHTING_REQUIRED;
    }

    public static List<GBufferSceneDataKind> frameReadinessTracked() {
        return FRAME_READINESS_TRACKED;
    }

    public static List<GBufferSceneDataKind> physicalGiShadowSamplingRequired() {
        return PHYSICAL_GI_SHADOW_SAMPLING_REQUIRED;
    }

    public static Optional<GBufferSceneDataKind> fromSemantic(GBufferWriteSemantic semantic) {
        Objects.requireNonNull(semantic, "semantic");
        for (GBufferSceneDataKind kind : values()) {
            if (kind.semantic == semantic) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    public static Optional<GBufferSceneDataKind> fromAttachmentName(String attachmentName) {
        Objects.requireNonNull(attachmentName, "attachmentName");
        for (GBufferSceneDataKind kind : values()) {
            if (kind.attachmentName().equals(attachmentName)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
