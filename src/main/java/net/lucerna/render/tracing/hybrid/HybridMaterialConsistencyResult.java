package net.lucerna.render.tracing.hybrid;

public record HybridMaterialConsistencyResult(
        boolean checked,
        boolean consistent,
        HybridHitSource source,
        int materialId,
        int expectedMaterialId,
        String reason
) {
    public HybridMaterialConsistencyResult {
        if (source == null) {
            source = HybridHitSource.MISS;
        }
        reason = clean(reason, checked ? "material ids match" : "material check skipped");
    }

    public static HybridMaterialConsistencyResult evaluate(HybridHitCandidate candidate) {
        if (candidate == null) {
            return new HybridMaterialConsistencyResult(false, false, HybridHitSource.MISS, -1, -1, "candidate missing");
        }
        if (!candidate.usableSurfaceHit()) {
            return new HybridMaterialConsistencyResult(
                    false,
                    false,
                    candidate.source(),
                    candidate.materialId(),
                    candidate.expectedMaterialId(),
                    "not a usable surface hit"
            );
        }
        if (candidate.materialId() < 0) {
            return new HybridMaterialConsistencyResult(
                    true,
                    false,
                    candidate.source(),
                    candidate.materialId(),
                    candidate.expectedMaterialId(),
                    "resolved hit has no material id"
            );
        }
        if (candidate.expectedMaterialId() < 0) {
            return new HybridMaterialConsistencyResult(
                    false,
                    true,
                    candidate.source(),
                    candidate.materialId(),
                    candidate.expectedMaterialId(),
                    "no expected material id supplied"
            );
        }
        boolean match = candidate.materialId() == candidate.expectedMaterialId();
        return new HybridMaterialConsistencyResult(
                true,
                match,
                candidate.source(),
                candidate.materialId(),
                candidate.expectedMaterialId(),
                match ? "material ids match" : "material id mismatch"
        );
    }

    public String summary() {
        return this.source.telemetryKey()
                + ":checked=" + yesNo(this.checked)
                + ",consistent=" + yesNo(this.consistent)
                + ",material=" + materialLabel(this.materialId)
                + ",expected=" + materialLabel(this.expectedMaterialId)
                + ",reason=" + this.reason;
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
