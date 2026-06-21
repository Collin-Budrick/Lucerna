package net.lucerna.render.gbuffer;

public enum GBufferSceneDataStatusCode {
    READY("Actual Minecraft/Mojang/Sodium sampled frame data is declared, written, bound, sampled, native-readable, and backed by concrete sample evidence."),
    TARGET_ATTACHMENT_MISSING("The target G-buffer contract does not declare this scene-data attachment."),
    WRITE_INTENT_MISSING("The current G-buffer write intent does not include this scene-data attachment."),
    WRITE_DIMENSIONS_MISSING("The current G-buffer write intent has no positive dimensions."),
    FRAME_TARGET_UNAVAILABLE("No frame target is available for scene-data handoff."),
    FRAME_TARGET_UNSAFE("The frame target is not safe for Lucerna lighting attachment."),
    FRAME_TARGET_METADATA_ONLY("The frame target only exposes metadata, not scene-data image objects."),
    FRAME_TARGET_JAVA_OPAQUE("The frame target exposes Java-opaque objects without a native-readable scene-data contract."),
    FRAME_TARGET_NOT_NATIVE_WRITABLE("The frame target is not native-writable for scene-data handoff."),
    FRAME_DATA_ATTACHMENT_MISSING("No explicit scene-derived frame-data attachment is bound for this semantic."),
    FRAME_DATA_NOT_SCENE_DERIVED("The bound attachment is not marked as scene-derived G-buffer data."),
    FRAME_DATA_EXTENT_MISSING("The bound scene-data attachment has no positive extent."),
    FRAME_DATA_EXTENT_MISMATCH("The bound scene-data attachment extent does not match the G-buffer write intent."),
    FRAME_DATA_NOT_NATIVE_READABLE("The bound scene-data attachment has no native image/view handles."),
    FRAME_DATA_NOT_SAMPLED("The bound scene-data attachment is not marked sampled for lighting."),
    FRAME_DATA_METADATA_ONLY("The available scene-data attachment is metadata-only; no sampled image data was reported."),
    FRAME_DATA_PUBLIC_MOJANG_OPAQUE_ONLY("Only public Mojang Java-opaque framebuffer fallback is available; no readable G-buffer attachment was sampled."),
    FRAME_DATA_CONTRACT_ONLY("The bound scene-data attachment is only a contract/metadata placeholder, not sampled Minecraft frame data."),
    FRAME_DATA_SYNTHETIC("The bound scene-data attachment is synthetic Lucerna data, not sampled Minecraft frame data."),
    FRAME_DATA_NOT_ACTUAL_GAME_SAMPLED("The bound scene-data attachment has not been proven to come from Minecraft/Mojang/Sodium sampled frame data."),
    FRAME_DATA_SAMPLE_EVIDENCE_MISSING("The bound scene-data attachment is sampled, but no concrete sample-count evidence was reported."),
    FRAME_DATA_DEPTH_SAMPLE_EVIDENCE_MISSING("The depth attachment lacks nonzero normalized depth sample evidence."),
    FRAME_DATA_NORMAL_SAMPLE_EVIDENCE_MISSING("The normal attachment lacks sampled normal evidence."),
    FRAME_DATA_MATERIAL_ID_SAMPLE_EVIDENCE_MISSING("The material attachment lacks material-id sample evidence."),
    FRAME_DATA_ALBEDO_SAMPLE_EVIDENCE_MISSING("The albedo attachment lacks sampled albedo evidence."),
    FRAME_DATA_EMISSIVE_SAMPLE_EVIDENCE_MISSING("The emissive attachment lacks sampled emissive evidence.");

    private final String description;

    GBufferSceneDataStatusCode(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
