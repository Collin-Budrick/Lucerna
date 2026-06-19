package net.lucerna.lighting;

public record DenoiseSignalInputContract(
        DenoiseSignalKind kind,
        String resourceName,
        String evidenceLabel,
        boolean available,
        boolean required,
        boolean placeholder,
        long generation,
        int width,
        int height,
        int sampleCount,
        int rayCount,
        int cacheReadCount,
        String readinessReason
) {
    public DenoiseSignalInputContract {
        if (kind == null) {
            kind = DenoiseSignalKind.DIFFUSE_GI;
        }
        resourceName = normalizeText(resourceName, kind.statusName());
        evidenceLabel = normalizeText(evidenceLabel, kind.statusName() + "_input");
        generation = Math.max(0L, generation);
        width = Math.max(0, width);
        height = Math.max(0, height);
        sampleCount = Math.max(0, sampleCount);
        rayCount = Math.max(0, rayCount);
        cacheReadCount = Math.max(0, cacheReadCount);
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? defaultReason(available, required, placeholder)
                : readinessReason;
    }

    public static DenoiseSignalInputContract diffuseGi(
            boolean available,
            long generation,
            int width,
            int height,
            int sampleCount,
            int rayCount,
            int cacheReadCount,
            String readinessReason
    ) {
        return new DenoiseSignalInputContract(
                DenoiseSignalKind.DIFFUSE_GI,
                "lucerna.lighting.diffuseGi",
                "raw_diffuse_gi_input",
                available,
                true,
                false,
                generation,
                width,
                height,
                sampleCount,
                rayCount,
                cacheReadCount,
                readinessReason
        );
    }

    public static DenoiseSignalInputContract directShadows(
            boolean available,
            long generation,
            int width,
            int height,
            int sampleCount,
            int rayCount,
            String readinessReason
    ) {
        return new DenoiseSignalInputContract(
                DenoiseSignalKind.DIRECT_SHADOWS,
                "lucerna.lighting.direct",
                "raw_direct_shadow_input",
                available,
                true,
                false,
                generation,
                width,
                height,
                sampleCount,
                rayCount,
                0,
                readinessReason
        );
    }

    public static DenoiseSignalInputContract optionalPlaceholder(DenoiseSignalKind kind, String readinessReason) {
        return new DenoiseSignalInputContract(
                kind,
                kind == null ? "optional_signal" : kind.statusName(),
                "optional_placeholder_input",
                false,
                false,
                true,
                0L,
                0,
                0,
                0,
                0,
                0,
                readinessReason
        );
    }

    public boolean readyForDenoise() {
        return this.available && !this.placeholder && this.width > 0 && this.height > 0;
    }

    public long pixelCount() {
        return (long) this.width * (long) this.height;
    }

    public String statusLabel() {
        return this.kind.statusName()
                + ":available=" + this.available
                + ",required=" + this.required
                + ",placeholder=" + this.placeholder
                + ",resource=" + this.resourceName
                + ",evidence=" + this.evidenceLabel
                + ",generation=" + this.generation
                + ",size=" + this.width + "x" + this.height
                + ",samples=" + this.sampleCount
                + ",rays=" + this.rayCount
                + ",cacheReads=" + this.cacheReadCount
                + ",reason=" + this.readinessReason;
    }

    private static String defaultReason(boolean available, boolean required, boolean placeholder) {
        if (placeholder) {
            return "optional signal placeholder, no denoise input claim";
        }
        if (available) {
            return "signal input available for denoise contract";
        }
        return required ? "required signal input unavailable" : "optional signal input unavailable";
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
