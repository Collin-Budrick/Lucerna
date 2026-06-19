package net.lucerna.upload;

import net.lucerna.render.lighting.post.FinalCompositeHandoff;

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
        this.flags = flags;

        this.validate();
    }

    public static NativePostProcessingCompositeUpload from(FinalCompositeHandoff handoff) {
        Objects.requireNonNull(handoff, "handoff");
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
                flags(handoff)
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
                this.writesWorldColorTarget
        );
        if (this.flags != expectedFlags) {
            throw new IllegalArgumentException("flags must match composite upload state");
        }
    }

    private static int flags(FinalCompositeHandoff handoff) {
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
                handoff.writesWorldColorTarget()
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
            boolean writesWorldColorTarget
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
