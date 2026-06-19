package net.lucerna.upload;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativeGBufferStagingUploadPacket {
    public static final int GBUFFER_DIMENSION_STRIDE = 2;
    public static final int GBUFFER_DIMENSION_WIDTH_OFFSET = 0;
    public static final int GBUFFER_DIMENSION_HEIGHT_OFFSET = 1;

    private final long generation;
    private final int gBufferStagingCount;
    private final long firstGBufferStagingGeneration;
    private final long lastGBufferStagingGeneration;
    private final long gBufferStagingGeneration;
    private final long[] stagingGenerations;
    private final String[] passIds;
    private final int[] numericPassIds;
    private final int[] widths;
    private final int[] heights;
    private final int[] dimensions;
    private final int[] attachmentPayloadOffsets;
    private final int[] attachmentPayloadCounts;
    private final String[] attachmentNames;
    private final String[] attachmentFormats;
    private final String[] attachmentResolutions;
    private final int[] attachmentSamples;
    private final int[] attachmentEnabled;

    private NativeGBufferStagingUploadPacket(
            long generation,
            int gBufferStagingCount,
            long firstGBufferStagingGeneration,
            long lastGBufferStagingGeneration,
            long gBufferStagingGeneration,
            long[] stagingGenerations,
            String[] passIds,
            int[] numericPassIds,
            int[] widths,
            int[] heights,
            int[] dimensions,
            int[] attachmentPayloadOffsets,
            int[] attachmentPayloadCounts,
            String[] attachmentNames,
            String[] attachmentFormats,
            String[] attachmentResolutions,
            int[] attachmentSamples,
            int[] attachmentEnabled
    ) {
        this.generation = generation;
        this.gBufferStagingCount = gBufferStagingCount;
        this.firstGBufferStagingGeneration = firstGBufferStagingGeneration;
        this.lastGBufferStagingGeneration = lastGBufferStagingGeneration;
        this.gBufferStagingGeneration = gBufferStagingGeneration;
        this.stagingGenerations = copy(stagingGenerations, "stagingGenerations");
        this.passIds = copy(passIds, "passIds");
        this.numericPassIds = copy(numericPassIds, "numericPassIds");
        this.widths = copy(widths, "widths");
        this.heights = copy(heights, "heights");
        this.dimensions = copy(dimensions, "dimensions");
        this.attachmentPayloadOffsets = copy(attachmentPayloadOffsets, "attachmentPayloadOffsets");
        this.attachmentPayloadCounts = copy(attachmentPayloadCounts, "attachmentPayloadCounts");
        this.attachmentNames = copy(attachmentNames, "attachmentNames");
        this.attachmentFormats = copy(attachmentFormats, "attachmentFormats");
        this.attachmentResolutions = copy(attachmentResolutions, "attachmentResolutions");
        this.attachmentSamples = copy(attachmentSamples, "attachmentSamples");
        this.attachmentEnabled = copy(attachmentEnabled, "attachmentEnabled");

        this.validate();
    }

    public static NativeGBufferStagingUploadPacket from(NativeStagedUploadBatch batch) {
        Objects.requireNonNull(batch, "batch");

        NativeUploadStagingMetadata metadata = batch.metadata();
        List<NativeGBufferStagingUpload> uploads = batch.gBufferStaging();
        int uploadCount = uploads.size();
        int attachmentPayloadCount = 0;

        for (NativeGBufferStagingUpload upload : uploads) {
            attachmentPayloadCount = checkedPayloadCount(
                    attachmentPayloadCount,
                    upload.attachmentCount(),
                    "G-buffer attachment payload"
            );
        }

        long[] stagingGenerations = new long[uploadCount];
        String[] passIds = new String[uploadCount];
        int[] numericPassIds = new int[uploadCount];
        int[] widths = new int[uploadCount];
        int[] heights = new int[uploadCount];
        int[] dimensions = new int[uploadCount * GBUFFER_DIMENSION_STRIDE];
        int[] attachmentPayloadOffsets = new int[uploadCount];
        int[] attachmentPayloadCounts = new int[uploadCount];
        String[] attachmentNames = new String[attachmentPayloadCount];
        String[] attachmentFormats = new String[attachmentPayloadCount];
        String[] attachmentResolutions = new String[attachmentPayloadCount];
        int[] attachmentSamples = new int[attachmentPayloadCount];
        int[] attachmentEnabled = new int[attachmentPayloadCount];

        int attachmentPayloadOffset = 0;
        for (int uploadIndex = 0; uploadIndex < uploads.size(); uploadIndex++) {
            NativeGBufferStagingUpload upload = uploads.get(uploadIndex);
            String[] uploadAttachmentNames = upload.attachmentNames();
            String[] uploadAttachmentFormats = upload.attachmentFormats();
            String[] uploadAttachmentResolutions = upload.attachmentResolutions();
            int[] uploadAttachmentSamples = upload.attachmentSamples();
            int[] uploadAttachmentEnabled = upload.attachmentEnabled();
            int attachmentCount = uploadAttachmentNames.length;

            requireMatchingLength(attachmentCount, "upload attachmentFormats", uploadAttachmentFormats.length);
            requireMatchingLength(attachmentCount, "upload attachmentResolutions", uploadAttachmentResolutions.length);
            requireMatchingLength(attachmentCount, "upload attachmentSamples", uploadAttachmentSamples.length);
            requireMatchingLength(attachmentCount, "upload attachmentEnabled", uploadAttachmentEnabled.length);

            stagingGenerations[uploadIndex] = upload.generation();
            passIds[uploadIndex] = upload.passId();
            numericPassIds[uploadIndex] = upload.numericPassId();
            widths[uploadIndex] = upload.width();
            heights[uploadIndex] = upload.height();
            int dimensionOffset = uploadIndex * GBUFFER_DIMENSION_STRIDE;
            dimensions[dimensionOffset + GBUFFER_DIMENSION_WIDTH_OFFSET] = upload.width();
            dimensions[dimensionOffset + GBUFFER_DIMENSION_HEIGHT_OFFSET] = upload.height();
            attachmentPayloadOffsets[uploadIndex] = attachmentPayloadOffset;
            attachmentPayloadCounts[uploadIndex] = attachmentCount;

            System.arraycopy(uploadAttachmentNames, 0, attachmentNames, attachmentPayloadOffset, attachmentCount);
            System.arraycopy(uploadAttachmentFormats, 0, attachmentFormats, attachmentPayloadOffset, attachmentCount);
            System.arraycopy(uploadAttachmentResolutions, 0, attachmentResolutions, attachmentPayloadOffset, attachmentCount);
            System.arraycopy(uploadAttachmentSamples, 0, attachmentSamples, attachmentPayloadOffset, attachmentCount);
            System.arraycopy(uploadAttachmentEnabled, 0, attachmentEnabled, attachmentPayloadOffset, attachmentCount);
            attachmentPayloadOffset += attachmentCount;
        }

        return new NativeGBufferStagingUploadPacket(
                metadata.generation(),
                metadata.gBufferStagingCount(),
                firstGBufferStagingGeneration(uploads),
                lastGBufferStagingGeneration(uploads),
                metadata.gBufferStagingGeneration(),
                stagingGenerations,
                passIds,
                numericPassIds,
                widths,
                heights,
                dimensions,
                attachmentPayloadOffsets,
                attachmentPayloadCounts,
                attachmentNames,
                attachmentFormats,
                attachmentResolutions,
                attachmentSamples,
                attachmentEnabled
        );
    }

    public long generation() {
        return this.generation;
    }

    public int gBufferStagingCount() {
        return this.gBufferStagingCount;
    }

    public long firstGBufferStagingGeneration() {
        return this.firstGBufferStagingGeneration;
    }

    public long lastGBufferStagingGeneration() {
        return this.lastGBufferStagingGeneration;
    }

    public long gBufferStagingGeneration() {
        return this.gBufferStagingGeneration;
    }

    public int gBufferStagingPayloadCount() {
        return this.stagingGenerations.length;
    }

    public int attachmentPayloadCount() {
        return this.attachmentNames.length;
    }

    public boolean isEmpty() {
        return this.gBufferStagingCount == 0;
    }

    public boolean hasPayloads() {
        return this.stagingGenerations.length > 0;
    }

    public long[] stagingGenerations() {
        return copy(this.stagingGenerations, "stagingGenerations");
    }

    public String[] passIds() {
        return copy(this.passIds, "passIds");
    }

    public int[] numericPassIds() {
        return copy(this.numericPassIds, "numericPassIds");
    }

    public int[] widths() {
        return copy(this.widths, "widths");
    }

    public int[] heights() {
        return copy(this.heights, "heights");
    }

    public int[] dimensions() {
        return copy(this.dimensions, "dimensions");
    }

    public int[] attachmentPayloadOffsets() {
        return copy(this.attachmentPayloadOffsets, "attachmentPayloadOffsets");
    }

    public int[] attachmentPayloadCounts() {
        return copy(this.attachmentPayloadCounts, "attachmentPayloadCounts");
    }

    public String[] attachmentNames() {
        return copy(this.attachmentNames, "attachmentNames");
    }

    public String[] attachmentFormats() {
        return copy(this.attachmentFormats, "attachmentFormats");
    }

    public String[] attachmentResolutions() {
        return copy(this.attachmentResolutions, "attachmentResolutions");
    }

    public int[] attachmentSamples() {
        return copy(this.attachmentSamples, "attachmentSamples");
    }

    public int[] attachmentEnabled() {
        return copy(this.attachmentEnabled, "attachmentEnabled");
    }

    private void validate() {
        requireNonNegative(this.generation, "generation");
        requireNonNegative(this.gBufferStagingCount, "gBufferStagingCount");
        requireNonNegative(this.firstGBufferStagingGeneration, "firstGBufferStagingGeneration");
        requireNonNegative(this.lastGBufferStagingGeneration, "lastGBufferStagingGeneration");
        requireNonNegative(this.gBufferStagingGeneration, "gBufferStagingGeneration");
        if (this.firstGBufferStagingGeneration > this.lastGBufferStagingGeneration) {
            throw new IllegalArgumentException(
                    "firstGBufferStagingGeneration must be less than or equal to lastGBufferStagingGeneration"
            );
        }
        if (this.gBufferStagingCount == 0
                && (this.firstGBufferStagingGeneration != 0 || this.lastGBufferStagingGeneration != 0)) {
            throw new IllegalArgumentException("empty G-buffer staging packet must use zero generation bounds");
        }

        requireMatchingLength(this.gBufferStagingCount, "stagingGenerations", this.stagingGenerations.length);
        requireMatchingLength(this.gBufferStagingCount, "passIds", this.passIds.length);
        requireMatchingLength(this.gBufferStagingCount, "numericPassIds", this.numericPassIds.length);
        requireMatchingLength(this.gBufferStagingCount, "widths", this.widths.length);
        requireMatchingLength(this.gBufferStagingCount, "heights", this.heights.length);
        requireMatchingLength(this.gBufferStagingCount * GBUFFER_DIMENSION_STRIDE, "dimensions", this.dimensions.length);
        requireMatchingLength(this.gBufferStagingCount, "attachmentPayloadOffsets", this.attachmentPayloadOffsets.length);
        requireMatchingLength(this.gBufferStagingCount, "attachmentPayloadCounts", this.attachmentPayloadCounts.length);
        requireMatchingLength(this.attachmentNames.length, "attachmentFormats", this.attachmentFormats.length);
        requireMatchingLength(this.attachmentNames.length, "attachmentResolutions", this.attachmentResolutions.length);
        requireMatchingLength(this.attachmentNames.length, "attachmentSamples", this.attachmentSamples.length);
        requireMatchingLength(this.attachmentNames.length, "attachmentEnabled", this.attachmentEnabled.length);

        requirePayloadWindowCoverage(
                this.attachmentPayloadOffsets,
                this.attachmentPayloadCounts,
                this.attachmentNames.length,
                "G-buffer attachment"
        );

        for (int index = 0; index < this.gBufferStagingCount; index++) {
            requireNonNegative(this.stagingGenerations[index], "stagingGenerations entries");
            requireText(this.passIds[index], "passIds entries");
            if (this.numericPassIds[index] <= 0) {
                throw new IllegalArgumentException("numericPassIds entries must be positive");
            }
            if (this.widths[index] < 0 || this.heights[index] < 0) {
                throw new IllegalArgumentException("G-buffer dimensions must be non-negative");
            }
            int dimensionOffset = index * GBUFFER_DIMENSION_STRIDE;
            requireMatchingValue(
                    this.widths[index],
                    "widths",
                    this.dimensions[dimensionOffset + GBUFFER_DIMENSION_WIDTH_OFFSET]
            );
            requireMatchingValue(
                    this.heights[index],
                    "heights",
                    this.dimensions[dimensionOffset + GBUFFER_DIMENSION_HEIGHT_OFFSET]
            );
        }
        for (int index = 0; index < this.attachmentNames.length; index++) {
            requireText(this.attachmentNames[index], "attachmentNames entries");
            requireText(this.attachmentFormats[index], "attachmentFormats entries");
            requireText(this.attachmentResolutions[index], "attachmentResolutions entries");
            if (this.attachmentSamples[index] <= 0) {
                throw new IllegalArgumentException("attachmentSamples entries must be positive");
            }
            int enabled = this.attachmentEnabled[index];
            if (enabled != 0 && enabled != 1) {
                throw new IllegalArgumentException("attachmentEnabled entries must be 0 or 1");
            }
        }
    }

    private static void requirePayloadWindowCoverage(int[] offsets, int[] counts, int payloadLength, String name) {
        int cursor = 0;
        for (int index = 0; index < offsets.length; index++) {
            int offset = offsets[index];
            int count = counts[index];
            if (offset < 0) {
                throw new IllegalArgumentException(name + " payload offsets must be non-negative");
            }
            if (count < 0) {
                throw new IllegalArgumentException(name + " payload counts must be non-negative");
            }
            if (offset != cursor) {
                throw new IllegalArgumentException(name + " payload offsets must be contiguous");
            }
            cursor = checkedPayloadCount(cursor, count, name);
            if (cursor > payloadLength) {
                throw new IllegalArgumentException(name + " payload windows exceed flattened payload length");
            }
        }
        if (cursor != payloadLength) {
            throw new IllegalArgumentException(name + " payload windows must cover flattened payload length");
        }
    }

    private static int checkedPayloadCount(int current, int increment, String name) {
        if (increment < 0) {
            throw new IllegalArgumentException(name + " count must be non-negative");
        }
        try {
            return Math.addExact(current, increment);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " count exceeds supported packet array length", exception);
        }
    }

    private static long firstGBufferStagingGeneration(List<NativeGBufferStagingUpload> uploads) {
        Objects.requireNonNull(uploads, "uploads");
        return uploads.stream()
                .mapToLong(NativeGBufferStagingUpload::generation)
                .min()
                .orElse(0L);
    }

    private static long lastGBufferStagingGeneration(List<NativeGBufferStagingUpload> uploads) {
        Objects.requireNonNull(uploads, "uploads");
        return uploads.stream()
                .mapToLong(NativeGBufferStagingUpload::generation)
                .max()
                .orElse(0L);
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireMatchingLength(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " length must be " + expected + " but was " + actual);
        }
    }

    private static void requireMatchingValue(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " packed dimension mismatch");
        }
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static long[] copy(long[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
