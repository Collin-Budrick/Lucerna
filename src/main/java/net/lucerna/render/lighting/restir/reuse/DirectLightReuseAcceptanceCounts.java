package net.lucerna.render.lighting.restir.reuse;

public record DirectLightReuseAcceptanceCounts(
        int acceptedCount,
        int rejectedCount,
        int invalidatedCount,
        int missingSourceCount
) {
    public DirectLightReuseAcceptanceCounts {
        acceptedCount = Math.max(0, acceptedCount);
        rejectedCount = Math.max(0, rejectedCount);
        invalidatedCount = Math.max(0, invalidatedCount);
        missingSourceCount = Math.max(0, missingSourceCount);
    }

    public static DirectLightReuseAcceptanceCounts empty() {
        return new DirectLightReuseAcceptanceCounts(0, 0, 0, 0);
    }

    public int consideredCount() {
        return this.acceptedCount + this.rejectedCount + this.invalidatedCount + this.missingSourceCount;
    }

    public boolean acceptedAny() {
        return this.acceptedCount > 0;
    }

    public String summary(String prefix) {
        String label = cleanPrefix(prefix);
        return label + ".acceptedCount=" + this.acceptedCount
                + "," + label + ".rejectedCount=" + this.rejectedCount
                + "," + label + ".invalidatedCount=" + this.invalidatedCount
                + "," + label + ".missingSourceCount=" + this.missingSourceCount;
    }

    private static String cleanPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "reuse";
        }
        return prefix.trim();
    }
}
