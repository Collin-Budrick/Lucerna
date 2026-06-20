package net.lucerna.render.tracing.hybrid;

public record HybridHitCandidate(
        HybridHitSource source,
        boolean hit,
        float distance,
        int materialId,
        int expectedMaterialId,
        boolean opaque,
        boolean emissive,
        boolean sourceAvailable,
        String fallbackReason,
        String debugLabel
) {
    public HybridHitCandidate {
        if (source == null) {
            source = HybridHitSource.MISS;
        }
        if (!hit) {
            distance = Float.POSITIVE_INFINITY;
        } else if (!Float.isFinite(distance) || distance < 0.0F) {
            distance = 0.0F;
        }
        fallbackReason = clean(fallbackReason, sourceAvailable ? "available" : "source unavailable");
        debugLabel = clean(debugLabel, source.displayName());
    }

    public static HybridHitCandidate screenSpace(
            float distance,
            int materialId,
            int expectedMaterialId,
            boolean opaque,
            boolean emissive
    ) {
        return surface(HybridHitSource.SCREEN_SPACE, distance, materialId, expectedMaterialId, opaque, emissive, true, "");
    }

    public static HybridHitCandidate voxel(
            float distance,
            int materialId,
            int expectedMaterialId,
            boolean opaque,
            boolean emissive,
            boolean voxelAvailable,
            String fallbackReason
    ) {
        return surface(
                HybridHitSource.VOXEL,
                distance,
                materialId,
                expectedMaterialId,
                opaque,
                emissive,
                voxelAvailable,
                fallbackReason
        );
    }

    public static HybridHitCandidate hardwareRt(
            float distance,
            int materialId,
            int expectedMaterialId,
            boolean opaque,
            boolean emissive,
            boolean hardwareRtAvailable,
            String fallbackReason
    ) {
        return surface(
                HybridHitSource.HARDWARE_RT,
                distance,
                materialId,
                expectedMaterialId,
                opaque,
                emissive,
                hardwareRtAvailable,
                fallbackReason
        );
    }

    public static HybridHitCandidate sky(String debugLabel) {
        return new HybridHitCandidate(
                HybridHitSource.SKY,
                false,
                Float.POSITIVE_INFINITY,
                -1,
                -1,
                false,
                false,
                true,
                "sky fallback",
                debugLabel
        );
    }

    public static HybridHitCandidate miss(String reason) {
        return new HybridHitCandidate(
                HybridHitSource.MISS,
                false,
                Float.POSITIVE_INFINITY,
                -1,
                -1,
                false,
                false,
                true,
                reason,
                "miss"
        );
    }

    public boolean usableSurfaceHit() {
        return this.hit && this.source.surfaceSource() && this.sourceAvailable;
    }

    public String summary() {
        return this.source.telemetryKey()
                + ":hit=" + yesNo(this.hit)
                + ",available=" + yesNo(this.sourceAvailable)
                + ",distance=" + distanceLabel(this.distance)
                + ",material=" + materialLabel(this.materialId)
                + ",expected=" + materialLabel(this.expectedMaterialId)
                + ",opaque=" + yesNo(this.opaque)
                + ",emissive=" + yesNo(this.emissive);
    }

    private static HybridHitCandidate surface(
            HybridHitSource source,
            float distance,
            int materialId,
            int expectedMaterialId,
            boolean opaque,
            boolean emissive,
            boolean sourceAvailable,
            String fallbackReason
    ) {
        return new HybridHitCandidate(
                source,
                true,
                distance,
                materialId,
                expectedMaterialId,
                opaque,
                emissive,
                sourceAvailable,
                fallbackReason,
                source.displayName()
        );
    }

    private static String distanceLabel(float distance) {
        return Float.isFinite(distance) ? Float.toString(distance) : "inf";
    }

    private static String materialLabel(int materialId) {
        return materialId >= 0 ? Integer.toString(materialId) : "unknown";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
