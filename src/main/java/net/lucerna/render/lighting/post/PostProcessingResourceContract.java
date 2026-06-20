package net.lucerna.render.lighting.post;

import net.lucerna.render.gbuffer.GBufferTargetContract;
import net.lucerna.render.resources.ShaderPassId;

import java.util.List;

public final class PostProcessingResourceContract {
    public static final String DENOISE_PASS_ID = "lucerna.denoise.diffuse";
    public static final int DENOISE_NUMERIC_PASS_ID = 300;
    public static final String COMPOSITE_PASS_ID = "lucerna.composite.final";
    public static final int COMPOSITE_NUMERIC_PASS_ID = 400;

    public static final String DIRECT_LIGHTING = "lucerna.lighting.direct";
    public static final String DIFFUSE_GI = "lucerna.lighting.diffuseGi";
    public static final String CACHE_CONFIDENCE = "lucerna.lighting.cacheConfidence";
    public static final String VARIANCE = "lucerna.lighting.variance";
    public static final String RAY_BUDGET = "lucerna.lighting.rayBudget";
    public static final String DENOISED_DIFFUSE = "lucerna.denoise.diffuse";
    public static final String REJECTION_MASK = "lucerna.denoise.rejectionMask";
    public static final String WORLD_COLOR = "lucerna.composite.worldColor";
    public static final String DEBUG_OVERLAY = "lucerna.debug.overlay";
    public static final String DENOISE_SHADER_RESOURCE = "lucerna:denoise/diffuse_edge_aware_contract";
    public static final String DENOISE_HISTORY_VARIANCE_SHADER_RESOURCE =
            "lucerna:denoise/history_variance_quality_contract";

    public static final String PREVIOUS_DEPTH = "PreviousDepth";
    public static final String PREVIOUS_NORMAL_ROUGHNESS = "PreviousNormalRoughness";
    public static final String PREVIOUS_LIGHTING = "PreviousLighting";
    public static final String MOTION_HISTORY = "MotionHistory";
    public static final String VARIANCE_CONFIDENCE = "VarianceConfidence";
    public static final String HISTORY_REJECTION_MASK = "lucerna.denoise.historyRejectionMask";
    public static final String RAW_VS_DENOISED_QUALITY = "lucerna.denoise.rawVsDenoisedQuality";

    public static final List<String> DENOISE_READS = List.of(
            GBufferTargetContract.DEPTH,
            GBufferTargetContract.NORMAL_ROUGHNESS,
            GBufferTargetContract.MOTION_HISTORY,
            DIRECT_LIGHTING,
            DIFFUSE_GI,
            CACHE_CONFIDENCE,
            VARIANCE,
            RAY_BUDGET,
            PREVIOUS_DEPTH,
            PREVIOUS_NORMAL_ROUGHNESS,
            PREVIOUS_LIGHTING,
            MOTION_HISTORY,
            VARIANCE_CONFIDENCE
    );

    public static final List<String> DENOISE_WRITES = List.of(
            DENOISED_DIFFUSE,
            REJECTION_MASK
    );

    public static final List<String> DENOISE_CONTRACT_OUTPUTS = List.of(
            DENOISED_DIFFUSE,
            REJECTION_MASK,
            HISTORY_REJECTION_MASK,
            RAW_VS_DENOISED_QUALITY
    );

    public static final List<String> DENOISE_CONTRACT_RESOURCES = List.of(
            DENOISE_SHADER_RESOURCE,
            DENOISE_HISTORY_VARIANCE_SHADER_RESOURCE,
            DENOISED_DIFFUSE,
            REJECTION_MASK,
            HISTORY_REJECTION_MASK,
            VARIANCE_CONFIDENCE,
            RAW_VS_DENOISED_QUALITY
    );

    public static final List<String> COMPOSITE_READS = List.of(
            GBufferTargetContract.ALBEDO_OPACITY,
            DIRECT_LIGHTING,
            DIFFUSE_GI,
            DENOISED_DIFFUSE,
            DEBUG_OVERLAY
    );

    public static final List<String> COMPOSITE_READS_WITHOUT_DEBUG = List.of(
            GBufferTargetContract.ALBEDO_OPACITY,
            DIRECT_LIGHTING,
            DIFFUSE_GI,
            DENOISED_DIFFUSE
    );

    public static final List<String> COMPOSITE_WRITES = List.of(WORLD_COLOR);

    private PostProcessingResourceContract() {
    }

    public static ShaderPassId denoisePassId() {
        return ShaderPassId.of(DENOISE_PASS_ID);
    }

    public static ShaderPassId compositePassId() {
        return ShaderPassId.of(COMPOSITE_PASS_ID);
    }
}
