package net.lucerna.render.lighting.gi;

import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.render.gbuffer.GBufferTargetContract;
import net.lucerna.render.gbuffer.GBufferWriteIntent;

import java.util.ArrayList;
import java.util.List;

public record DiffuseGiFrameInput(
        LucernaFrameConstants frameConstants,
        GBufferWriteIntent gBufferWriteIntent,
        DiffuseGiLowResolutionGrid lowResolutionGrid,
        DiffuseGiSettings settings
) {
    private static final List<String> REQUIRED_GBUFFER_ATTACHMENTS = List.of(
            GBufferTargetContract.DEPTH,
            GBufferTargetContract.NORMAL_ROUGHNESS,
            GBufferTargetContract.ALBEDO_OPACITY,
            GBufferTargetContract.MATERIAL_ID,
            GBufferTargetContract.EMISSIVE
    );
    private static final List<String> TEMPORAL_GBUFFER_ATTACHMENTS = List.of(
            GBufferTargetContract.MOTION_HISTORY
    );

    public DiffuseGiFrameInput {
        if (frameConstants == null) {
            frameConstants = LucernaFrameConstants.unavailable();
        }
        if (gBufferWriteIntent == null) {
            gBufferWriteIntent = GBufferWriteIntent.empty(frameConstants.frameIndex());
        }
        if (settings == null) {
            settings = DiffuseGiSettings.disabled();
        }
        if (lowResolutionGrid == null) {
            lowResolutionGrid = DiffuseGiLowResolutionGrid.fromViewport(
                    frameConstants.viewport(),
                    settings.internalScaleDivisor()
            );
        }
    }

    public static DiffuseGiFrameInput from(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent gBufferWriteIntent,
            DiffuseGiSettings settings
    ) {
        LucernaFrameConstants resolvedConstants = frameConstants == null
                ? LucernaFrameConstants.unavailable()
                : frameConstants;
        DiffuseGiSettings resolvedSettings = settings == null ? DiffuseGiSettings.disabled() : settings;
        return new DiffuseGiFrameInput(
                resolvedConstants,
                gBufferWriteIntent,
                DiffuseGiLowResolutionGrid.fromViewport(resolvedConstants.viewport(), resolvedSettings.internalScaleDivisor()),
                resolvedSettings
        );
    }

    public List<String> requiredGBufferAttachments() {
        return REQUIRED_GBUFFER_ATTACHMENTS;
    }

    public List<String> temporalGBufferAttachments() {
        return TEMPORAL_GBUFFER_ATTACHMENTS;
    }

    public List<String> missingRequiredGBufferAttachments() {
        List<String> missing = new ArrayList<>();
        for (String requiredAttachment : REQUIRED_GBUFFER_ATTACHMENTS) {
            if (!this.gBufferWriteIntent.writesAttachment(requiredAttachment)) {
                missing.add(requiredAttachment);
            }
        }
        return List.copyOf(missing);
    }

    public List<String> missingTemporalGBufferAttachments() {
        List<String> missing = new ArrayList<>();
        for (String temporalAttachment : TEMPORAL_GBUFFER_ATTACHMENTS) {
            if (!this.gBufferWriteIntent.writesAttachment(temporalAttachment)) {
                missing.add(temporalAttachment);
            }
        }
        return List.copyOf(missing);
    }

    public List<String> missingInputs() {
        List<String> missing = new ArrayList<>(this.frameConstants.missingRequiredConstants());
        if (!this.settings.enabled()) {
            missing.add("diffuseGiSettings");
        }
        if (this.frameConstants.hasRenderFlags() && !this.frameConstants.flags().diffuseGiEnabled()) {
            missing.add("diffuseGiFrameFlag");
        }
        if (!this.lowResolutionGrid.available()) {
            missing.add("lowResolutionGrid");
        }
        if (!this.gBufferWriteIntent.dimensionsAvailable()) {
            missing.add("gBufferDimensions");
        }
        missing.addAll(this.missingRequiredGBufferAttachments());
        return List.copyOf(missing);
    }

    public boolean hasRequiredInputs() {
        return this.missingInputs().isEmpty();
    }

    public boolean dimensionsMatchViewport() {
        return this.frameConstants.hasViewport()
                && this.gBufferWriteIntent.width() == this.frameConstants.viewport().width()
                && this.gBufferWriteIntent.height() == this.frameConstants.viewport().height();
    }
}
