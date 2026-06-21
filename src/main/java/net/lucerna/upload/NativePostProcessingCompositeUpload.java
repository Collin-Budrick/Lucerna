package net.lucerna.upload;

import net.lucerna.render.lighting.post.FinalCompositeHandoff;
import net.lucerna.render.tracing.TracedLightingConsumptionEvidence;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativePostProcessingCompositeUpload {
    public static final int FLAG_READY_FOR_WORLD_COLOR_HANDOFF = 1;
    public static final int FLAG_BORROWED_WORLD_COLOR_TARGET = 1 << 1;
    public static final int FLAG_BEFORE_HUD_AND_LATE_TRANSLUCENCY = 1 << 2;
    public static final int FLAG_CLEAR_BEFORE_WRITE = 1 << 3;
    public static final int FLAG_USES_DENOISED_DIFFUSE = 1 << 4;
    public static final int FLAG_DEBUG_OVERLAY_AVAILABLE = 1 << 5;
    public static final int FLAG_WRITES_WORLD_COLOR_TARGET = 1 << 6;
    public static final int FLAG_USES_DIRECT_LIGHTING = 1 << 7;
    public static final int FLAG_USES_RAW_DIFFUSE_GI = 1 << 8;
    public static final int FLAG_BLENDS_DIRECT_RAW_AND_DENOISED = 1 << 9;
    public static final int FLAG_TRACE_LIGHTING_CONSUMED_BY_FINAL_COMPOSITE = 1 << 10;
    public static final int FLAG_REAL_GPU_TRACE_LIGHTING_CONSUMED = 1 << 11;

    private final long frameIndex;
    private final long sourceGeneration;
    private final String targetName;
    private final String targetDescription;
    private final int width;
    private final int height;
    private final int readResourceCount;
    private final int writeResourceCount;
    private final String[] readResources;
    private final String[] writeResources;
    private final boolean borrowedWorldColorTarget;
    private final boolean beforeHudAndLateTranslucency;
    private final boolean clearBeforeWrite;
    private final boolean usesDirectLighting;
    private final boolean usesRawDiffuseGi;
    private final boolean usesDenoisedDiffuse;
    private final boolean blendsDirectRawAndDenoised;
    private final boolean debugOverlayAvailable;
    private final boolean writesWorldColorTarget;
    private final boolean readyForWorldColorHandoff;
    private final long tracedLightingGeneration;
    private final long tracedLightingRayCount;
    private final long tracedLightingHitCount;
    private final long tracedLightingMaterialCoupledHitCount;
    private final long tracedLightingDepthCoupledHitCount;
    private final boolean tracedLightingConsumedByFinalComposite;
    private final boolean realGpuTracedLightingConsumed;
    private final String tracedLightingSource;
    private final String tracedLightingBlocker;
    private final int flags;

    private NativePostProcessingCompositeUpload(
            long frameIndex,
            long sourceGeneration,
            String targetName,
            String targetDescription,
            int width,
            int height,
            String[] readResources,
            String[] writeResources,
            boolean borrowedWorldColorTarget,
            boolean beforeHudAndLateTranslucency,
            boolean clearBeforeWrite,
            boolean usesDirectLighting,
            boolean usesRawDiffuseGi,
            boolean usesDenoisedDiffuse,
            boolean blendsDirectRawAndDenoised,
            boolean debugOverlayAvailable,
            boolean writesWorldColorTarget,
            boolean readyForWorldColorHandoff,
            long tracedLightingGeneration,
            long tracedLightingRayCount,
            long tracedLightingHitCount,
            long tracedLightingMaterialCoupledHitCount,
            long tracedLightingDepthCoupledHitCount,
            boolean tracedLightingConsumedByFinalComposite,
            boolean realGpuTracedLightingConsumed,
            String tracedLightingSource,
            String tracedLightingBlocker,
            int flags
    ) {
        this.frameIndex = frameIndex;
        this.sourceGeneration = sourceGeneration;
        this.targetName = requireText(targetName, "targetName");
        this.targetDescription = requireText(targetDescription, "targetDescription");
        this.width = width;
        this.height = height;
        this.readResources = copy(readResources, "readResources");
        this.writeResources = copy(writeResources, "writeResources");
        this.readResourceCount = this.readResources.length;
        this.writeResourceCount = this.writeResources.length;
        this.borrowedWorldColorTarget = borrowedWorldColorTarget;
        this.beforeHudAndLateTranslucency = beforeHudAndLateTranslucency;
        this.clearBeforeWrite = clearBeforeWrite;
        this.usesDirectLighting = usesDirectLighting;
        this.usesRawDiffuseGi = usesRawDiffuseGi;
        this.usesDenoisedDiffuse = usesDenoisedDiffuse;
        this.blendsDirectRawAndDenoised = blendsDirectRawAndDenoised;
        this.debugOverlayAvailable = debugOverlayAvailable;
        this.writesWorldColorTarget = writesWorldColorTarget;
        this.readyForWorldColorHandoff = readyForWorldColorHandoff;
        this.tracedLightingGeneration = tracedLightingGeneration;
        this.tracedLightingRayCount = tracedLightingRayCount;
        this.tracedLightingHitCount = tracedLightingHitCount;
        this.tracedLightingMaterialCoupledHitCount = tracedLightingMaterialCoupledHitCount;
        this.tracedLightingDepthCoupledHitCount = tracedLightingDepthCoupledHitCount;
        this.tracedLightingConsumedByFinalComposite = tracedLightingConsumedByFinalComposite;
        this.realGpuTracedLightingConsumed = realGpuTracedLightingConsumed;
        this.tracedLightingSource = requireText(tracedLightingSource, "tracedLightingSource");
        this.tracedLightingBlocker = requireText(tracedLightingBlocker, "tracedLightingBlocker");
        this.flags = flags;

        this.validate();
    }

    public static NativePostProcessingCompositeUpload from(FinalCompositeHandoff handoff) {
        return from(handoff, null);
    }

    public static NativePostProcessingCompositeUpload from(
            FinalCompositeHandoff handoff,
            TracedLightingConsumptionEvidence traceEvidence
    ) {
        Objects.requireNonNull(handoff, "handoff");
        TracedLightingConsumptionEvidence resolvedTraceEvidence = traceEvidence == null
                ? TracedLightingConsumptionEvidence.notConsumed(
                        handoff.sourceGeneration(),
                        "final_composite_trace_consumption_not_supplied"
                )
                : traceEvidence;
        boolean compositeUsesDiffuseGi = handoff.readyForWorldColorHandoff()
                && handoff.writesWorldColorTarget()
                && (handoff.usesRawDiffuseGi() || handoff.usesDenoisedDiffuse());
        boolean tracedLightingConsumed = compositeUsesDiffuseGi
                && resolvedTraceEvidence.finalGiSourceConsumed();
        boolean realGpuTraceConsumed = tracedLightingConsumed
                && resolvedTraceEvidence.realGpuTraversalConsumed();
        return new NativePostProcessingCompositeUpload(
                handoff.frameIndex(),
                handoff.sourceGeneration(),
                handoff.targetName(),
                handoff.targetDescription(),
                handoff.width(),
                handoff.height(),
                toArray(handoff.readResources()),
                toArray(handoff.writeResources()),
                handoff.borrowedWorldColorTarget(),
                handoff.beforeHudAndLateTranslucency(),
                handoff.clearBeforeWrite(),
                handoff.usesDirectLighting(),
                handoff.usesRawDiffuseGi(),
                handoff.usesDenoisedDiffuse(),
                handoff.blendsDirectRawAndDenoisedSources(),
                handoff.debugOverlayAvailable(),
                handoff.writesWorldColorTarget(),
                handoff.readyForWorldColorHandoff(),
                resolvedTraceEvidence.generation(),
                resolvedTraceEvidence.rayCount(),
                resolvedTraceEvidence.hitCount(),
                resolvedTraceEvidence.materialCoupledHitCount(),
                resolvedTraceEvidence.depthCoupledHitCount(),
                tracedLightingConsumed,
                realGpuTraceConsumed,
                resolvedTraceEvidence.finalGiSource() + " via " + resolvedTraceEvidence.evidenceSource(),
                resolvedTraceEvidence.blocker(),
                flags(handoff, tracedLightingConsumed, realGpuTraceConsumed)
        );
    }

    public long frameIndex() {
        return this.frameIndex;
    }

    public long sourceGeneration() {
        return this.sourceGeneration;
    }

    public String targetName() {
        return this.targetName;
    }

    public String targetDescription() {
        return this.targetDescription;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int readResourceCount() {
        return this.readResourceCount;
    }

    public int writeResourceCount() {
        return this.writeResourceCount;
    }

    public String[] readResources() {
        return copy(this.readResources, "readResources");
    }

    public String[] writeResources() {
        return copy(this.writeResources, "writeResources");
    }

    public boolean borrowedWorldColorTarget() {
        return this.borrowedWorldColorTarget;
    }

    public boolean beforeHudAndLateTranslucency() {
        return this.beforeHudAndLateTranslucency;
    }

    public boolean clearBeforeWrite() {
        return this.clearBeforeWrite;
    }

    public boolean usesDirectLighting() {
        return this.usesDirectLighting;
    }

    public boolean usesRawDiffuseGi() {
        return this.usesRawDiffuseGi;
    }

    public boolean usesDenoisedDiffuse() {
        return this.usesDenoisedDiffuse;
    }

    public boolean blendsDirectRawAndDenoised() {
        return this.blendsDirectRawAndDenoised;
    }

    public boolean debugOverlayAvailable() {
        return this.debugOverlayAvailable;
    }

    public boolean writesWorldColorTarget() {
        return this.writesWorldColorTarget;
    }

    public boolean readyForWorldColorHandoff() {
        return this.readyForWorldColorHandoff;
    }

    public int flags() {
        return this.flags;
    }

    public long tracedLightingGeneration() {
        return this.tracedLightingGeneration;
    }

    public long tracedLightingRayCount() {
        return this.tracedLightingRayCount;
    }

    public long tracedLightingHitCount() {
        return this.tracedLightingHitCount;
    }

    public long tracedLightingMaterialCoupledHitCount() {
        return this.tracedLightingMaterialCoupledHitCount;
    }

    public long tracedLightingDepthCoupledHitCount() {
        return this.tracedLightingDepthCoupledHitCount;
    }

    public boolean tracedLightingConsumedByFinalComposite() {
        return this.tracedLightingConsumedByFinalComposite;
    }

    public boolean realGpuTracedLightingConsumed() {
        return this.realGpuTracedLightingConsumed;
    }

    public String tracedLightingSource() {
        return this.tracedLightingSource;
    }

    public String tracedLightingBlocker() {
        return this.tracedLightingBlocker;
    }

    public long[] traceConsumptionCounts() {
        return new long[]{
                this.tracedLightingGeneration,
                this.tracedLightingRayCount,
                this.tracedLightingHitCount,
                this.tracedLightingMaterialCoupledHitCount,
                this.tracedLightingDepthCoupledHitCount
        };
    }

    public int[] traceConsumptionFlags() {
        return new int[]{
                this.tracedLightingConsumedByFinalComposite ? 1 : 0,
                this.realGpuTracedLightingConsumed ? 1 : 0
        };
    }

    public String[] traceConsumptionLabels() {
        return new String[]{
                this.tracedLightingSource,
                this.tracedLightingBlocker
        };
    }

    private void validate() {
        requireNonNegative(this.frameIndex, "frameIndex");
        requireNonNegative(this.sourceGeneration, "sourceGeneration");
        requireNonNegative(this.width, "width");
        requireNonNegative(this.height, "height");
        requireTextEntries(this.readResources, "readResources");
        requireTextEntries(this.writeResources, "writeResources");
        if (this.readyForWorldColorHandoff
                && (this.frameIndex == 0L || this.width == 0 || this.height == 0 || !this.writesWorldColorTarget)) {
            throw new IllegalArgumentException("ready composite handoff requires frame, dimensions, and world color write");
        }
        if (this.blendsDirectRawAndDenoised
                != (this.usesDirectLighting && this.usesRawDiffuseGi && this.usesDenoisedDiffuse)) {
            throw new IllegalArgumentException("blendsDirectRawAndDenoised must match direct/raw/denoised source state");
        }
        requireNonNegative(this.tracedLightingGeneration, "tracedLightingGeneration");
        requireNonNegative(this.tracedLightingRayCount, "tracedLightingRayCount");
        requireNonNegative(this.tracedLightingHitCount, "tracedLightingHitCount");
        requireNonNegative(this.tracedLightingMaterialCoupledHitCount, "tracedLightingMaterialCoupledHitCount");
        requireNonNegative(this.tracedLightingDepthCoupledHitCount, "tracedLightingDepthCoupledHitCount");
        if (this.tracedLightingMaterialCoupledHitCount > this.tracedLightingHitCount) {
            throw new IllegalArgumentException("tracedLightingMaterialCoupledHitCount cannot exceed tracedLightingHitCount");
        }
        if (this.tracedLightingDepthCoupledHitCount > this.tracedLightingHitCount) {
            throw new IllegalArgumentException("tracedLightingDepthCoupledHitCount cannot exceed tracedLightingHitCount");
        }
        if (this.tracedLightingConsumedByFinalComposite
                && (!this.readyForWorldColorHandoff
                || !this.writesWorldColorTarget
                || (!this.usesRawDiffuseGi && !this.usesDenoisedDiffuse)
                || this.tracedLightingRayCount == 0L
                || this.tracedLightingHitCount == 0L
                || this.tracedLightingMaterialCoupledHitCount == 0L
                || this.tracedLightingDepthCoupledHitCount == 0L)) {
            throw new IllegalArgumentException(
                    "tracedLightingConsumedByFinalComposite requires ready diffuse-GI composite and trace coupling counts"
            );
        }
        if (this.realGpuTracedLightingConsumed && !this.tracedLightingConsumedByFinalComposite) {
            throw new IllegalArgumentException(
                    "realGpuTracedLightingConsumed requires tracedLightingConsumedByFinalComposite"
            );
        }
        int expectedFlags = flags(
                this.readyForWorldColorHandoff,
                this.borrowedWorldColorTarget,
                this.beforeHudAndLateTranslucency,
                this.clearBeforeWrite,
                this.usesDirectLighting,
                this.usesRawDiffuseGi,
                this.usesDenoisedDiffuse,
                this.blendsDirectRawAndDenoised,
                this.debugOverlayAvailable,
                this.writesWorldColorTarget,
                this.tracedLightingConsumedByFinalComposite,
                this.realGpuTracedLightingConsumed
        );
        if (this.flags != expectedFlags) {
            throw new IllegalArgumentException("flags must match composite upload state");
        }
    }

    private static int flags(
            FinalCompositeHandoff handoff,
            boolean tracedLightingConsumedByFinalComposite,
            boolean realGpuTracedLightingConsumed
    ) {
        return flags(
                handoff.readyForWorldColorHandoff(),
                handoff.borrowedWorldColorTarget(),
                handoff.beforeHudAndLateTranslucency(),
                handoff.clearBeforeWrite(),
                handoff.usesDirectLighting(),
                handoff.usesRawDiffuseGi(),
                handoff.usesDenoisedDiffuse(),
                handoff.blendsDirectRawAndDenoisedSources(),
                handoff.debugOverlayAvailable(),
                handoff.writesWorldColorTarget(),
                tracedLightingConsumedByFinalComposite,
                realGpuTracedLightingConsumed
        );
    }

    private static int flags(
            boolean readyForWorldColorHandoff,
            boolean borrowedWorldColorTarget,
            boolean beforeHudAndLateTranslucency,
            boolean clearBeforeWrite,
            boolean usesDirectLighting,
            boolean usesRawDiffuseGi,
            boolean usesDenoisedDiffuse,
            boolean blendsDirectRawAndDenoised,
            boolean debugOverlayAvailable,
            boolean writesWorldColorTarget,
            boolean tracedLightingConsumedByFinalComposite,
            boolean realGpuTracedLightingConsumed
    ) {
        int flags = 0;
        if (readyForWorldColorHandoff) {
            flags |= FLAG_READY_FOR_WORLD_COLOR_HANDOFF;
        }
        if (borrowedWorldColorTarget) {
            flags |= FLAG_BORROWED_WORLD_COLOR_TARGET;
        }
        if (beforeHudAndLateTranslucency) {
            flags |= FLAG_BEFORE_HUD_AND_LATE_TRANSLUCENCY;
        }
        if (clearBeforeWrite) {
            flags |= FLAG_CLEAR_BEFORE_WRITE;
        }
        if (usesDirectLighting) {
            flags |= FLAG_USES_DIRECT_LIGHTING;
        }
        if (usesRawDiffuseGi) {
            flags |= FLAG_USES_RAW_DIFFUSE_GI;
        }
        if (usesDenoisedDiffuse) {
            flags |= FLAG_USES_DENOISED_DIFFUSE;
        }
        if (blendsDirectRawAndDenoised) {
            flags |= FLAG_BLENDS_DIRECT_RAW_AND_DENOISED;
        }
        if (debugOverlayAvailable) {
            flags |= FLAG_DEBUG_OVERLAY_AVAILABLE;
        }
        if (writesWorldColorTarget) {
            flags |= FLAG_WRITES_WORLD_COLOR_TARGET;
        }
        if (tracedLightingConsumedByFinalComposite) {
            flags |= FLAG_TRACE_LIGHTING_CONSUMED_BY_FINAL_COMPOSITE;
        }
        if (realGpuTracedLightingConsumed) {
            flags |= FLAG_REAL_GPU_TRACE_LIGHTING_CONSUMED;
        }
        return flags;
    }

    private static String[] toArray(List<String> values) {
        Objects.requireNonNull(values, "values");
        return values.toArray(String[]::new);
    }

    private static void requireTextEntries(String[] values, String name) {
        for (String value : values) {
            requireText(value, name + " entries");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
