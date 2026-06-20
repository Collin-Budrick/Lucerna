package net.lucerna.render.lighting.restir.status;

public record Round11RestirConfidenceStats(
        String minConfidence,
        String meanConfidence,
        String maxConfidence
) {
    public Round11RestirConfidenceStats {
        minConfidence = clean(minConfidence, "?");
        meanConfidence = clean(meanConfidence, "?");
        maxConfidence = clean(maxConfidence, "?");
    }

    public static Round11RestirConfidenceStats unavailable() {
        return new Round11RestirConfidenceStats("?", "?", "?");
    }

    public boolean hasAnyConfidence() {
        return isKnown(this.minConfidence) || isKnown(this.meanConfidence) || isKnown(this.maxConfidence);
    }

    public String compactLine() {
        return "min=" + this.minConfidence
                + " mean=" + this.meanConfidence
                + " max=" + this.maxConfidence;
    }

    private static boolean isKnown(String value) {
        return value != null && !value.isBlank() && !"?".equals(value);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return stripQuotes(value);
    }

    static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}
