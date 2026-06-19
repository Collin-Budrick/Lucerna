package net.lucerna.lighting;

public record DenoiseSignalInputContract(
        DenoiseSignalKind kind,
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
}
