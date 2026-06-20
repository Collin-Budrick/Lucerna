package net.lucerna.render.virtualization;

import net.lucerna.render.culling.Round9CullingDebugStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;

import java.util.Locale;

public record Round9CullingRuntimeStatus(
        boolean telemetryPresent,
        Long clusterCount,
        Long visibleClusterCount,
        Long culledClusterCount,
        Long offscreenClusterCount,
        Long frustumCandidateCount,
        Long frustumCulledCount,
        Long occlusionPlaceholderCount,
        Long indirectDrawCount,
        Long uploadBytes,
        Long generation,
        boolean actualGpuCullingExecuted,
        boolean gpuCullingPrerequisitesReady,
        String gpuCullingMissingPrerequisites,
        String gpuCullingBlockerReason,
        boolean occlusionReady,
        boolean indirectDrawReady,
        String executionModeLabel,
        String nativeMode,
        String cullingReason,
        boolean terrainRenderingChanged,
        Round9CullingDebugStatus source
) {
    private static final String UNKNOWN = "?";

    public Round9CullingRuntimeStatus {
        nativeMode = clean(nativeMode, "unreported");
        cullingReason = clean(cullingReason, "unreported");
        if (source == null) {
            source = new Round9CullingDebugStatus(
                    false,
                    "round9 culling telemetry unavailable",
                    "Cluster metadata: unavailable",
                    "Visibility counts: unavailable",
                    "Culling: unavailable",
                    "Indirect draw list: unavailable",
                    "Cluster upload: unavailable",
                    "Cluster generation: unavailable",
                    "Round 9 readiness: missing",
                    "Round 9 GPU culling readiness: missing",
                    "Round 9 frustum candidates: unavailable",
                    "Round 9 occlusion readiness: unavailable",
                    "Round 9 execution boundary: actualGpuExecuted=no",
                    "Round 9 evidence boundary: runtime status unavailable"
            );
        }
        gpuCullingMissingPrerequisites = clean(gpuCullingMissingPrerequisites, "gpu-dispatch-visibility-buffer-occlusion-query");
        gpuCullingBlockerReason = clean(gpuCullingBlockerReason, "actual-gpu-culling-not-proven");
        actualGpuCullingExecuted = actualGpuCullingExecuted && gpuCullingPrerequisitesReady;
        indirectDrawReady = indirectDrawReady && actualGpuCullingExecuted;
        executionModeLabel = clean(
                executionModeLabel,
                actualGpuCullingExecuted ? "actual-gpu-culling" : "conservative-cpu-status"
        );
    }

    public static Round9CullingRuntimeStatus fromSnapshot(LucernaStatusSnapshot snapshot) {
        Round9CullingDebugStatus source = Round9CullingDebugStatus.fromSnapshot(snapshot);
        String clusterLine = source.clusterMetadataLine();
        String visibilityLine = source.visibilityCountsLine();
        String cullingLine = source.cullingLine();
        String indirectLine = source.indirectDrawLine();
        String uploadLine = source.uploadLine();
        String generationLine = source.generationLine();
        return new Round9CullingRuntimeStatus(
                source.telemetryPresent(),
                firstLong(clusterLine, "clusters"),
                firstLong(visibilityLine, "visible", indirectLine, "visibleClusters"),
                firstLong(visibilityLine, "culled"),
                firstLong(visibilityLine, "offscreen"),
                firstLong(visibilityLine, "frustumCandidates", cullingLine, "frustumCandidates"),
                firstLong(cullingLine, "frustum"),
                firstLong(visibilityLine, "occlusionPlaceholder", cullingLine, "occlusionPlaceholder"),
                firstLong(indirectLine, "draws"),
                firstLong(uploadLine, "bytes"),
                firstLong(generationLine, "gen"),
                truthy(firstToken(cullingLine, "actualGpuExecuted")),
                truthy(firstToken(cullingLine, "gpuPrereqsReady")),
                firstToken(cullingLine, "missingPrereqs"),
                firstToken(cullingLine, "blocker"),
                truthy(firstToken(cullingLine, "occlusionReady")),
                truthy(firstToken(indirectLine, "ready")),
                firstToken(cullingLine, "modeLabel"),
                firstToken(cullingLine, "mode"),
                reasonFrom(source, cullingLine),
                parseTerrainRenderingChanged(cullingLine),
                source
        );
    }

    public String summary() {
        return "clusters=" + countLabel(clusterCount)
                + ",visible=" + countLabel(visibleClusterCount)
                + ",culled=" + countLabel(totalCulledOrOffscreen())
                + ",draws=" + countLabel(indirectDrawCount)
                + ",gpuExecuted=" + yesNo(actualGpuCullingExecuted)
                + ",mode=" + cullingModeLabel()
                + ",terrainUnchanged=" + yesNo(!terrainRenderingChanged);
    }

    public String clusterCountLine() {
        return "Round 9 clusters: total=" + countLabel(clusterCount)
                + " state=" + countState(clusterCount, "no cluster metadata yet")
                + " uploadBytes=" + countLabel(uploadBytes)
                + " generation=" + countLabel(generation);
    }

    public String visibilityCountLine() {
        return "Round 9 visibility: visible=" + countLabel(visibleClusterCount)
                + " culled=" + countLabel(culledClusterCount)
                + " offscreen=" + countLabel(offscreenClusterCount)
                + " frustumCandidates=" + countLabel(frustumCandidateCount)
                + " frustumCulled=" + countLabel(frustumCulledCount)
                + " occlusionPlaceholder=" + countLabel(occlusionPlaceholderCount)
                + " totalHidden=" + countLabel(totalCulledOrOffscreen())
                + " state=" + visibilityState();
    }

    public String indirectDrawCountLine() {
        return "Round 9 indirect draws: count=" + countLabel(indirectDrawCount)
                + " ready=" + yesNo(indirectDrawReady)
                + " state=" + indirectDrawState()
                + " source=" + (indirectDrawReady ? "gpu-ready telemetry" : "CPU/conservative runtime status");
    }

    public String cullingModeLine() {
        return "Round 9 culling mode: " + cullingModeLabel()
                + " actualGpuExecuted=" + yesNo(actualGpuCullingExecuted)
                + " prerequisitesReady=" + yesNo(gpuCullingPrerequisitesReady)
                + " nativeMode=" + nativeMode
                + " reason=" + cullingReason;
    }

    public String gpuCullingReadinessLine() {
        return "Round 9 GPU culling readiness: actualGpuExecuted=" + yesNo(actualGpuCullingExecuted)
                + " prerequisitesReady=" + yesNo(gpuCullingPrerequisitesReady)
                + " missingPrereqs=" + gpuCullingMissingPrerequisites
                + " blocker=" + gpuCullingBlockerReason
                + " modeLabel=" + executionModeLabel;
    }

    public String frustumCandidateLine() {
        return "Round 9 frustum candidates: candidates=" + countLabel(frustumCandidateCount)
                + " culled=" + countLabel(frustumCulledCount)
                + " visible=" + countLabel(visibleClusterCount)
                + " offscreen=" + countLabel(offscreenClusterCount);
    }

    public String occlusionReadinessLine() {
        return "Round 9 occlusion readiness: ready=" + yesNo(occlusionReady)
                + " placeholders=" + countLabel(occlusionPlaceholderCount)
                + " blocker=" + (occlusionReady ? "none" : "occlusion-query-or-hiz-not-proven");
    }

    public String terrainRenderingLine() {
        return "Round 9 terrain rendering unchanged: " + yesNo(!terrainRenderingChanged)
                + " (status/debug overlay only; no terrain draw replacement)";
    }

    public String uploadLine() {
        return source.uploadLine();
    }

    public String generationLine() {
        return source.generationLine();
    }

    public String invalidOrZeroLine() {
        return "Round 9 value guard: " + valueGuard();
    }

    public String readinessLine() {
        return "Round 9 runtime readiness: telemetry=" + yesNo(telemetryPresent)
                + " clusters=" + readiness(clusterCount)
                + " visibility=" + readiness(visibleClusterCount)
                + " culling=" + hiddenReadiness()
                + " gpuPrereqs=" + yesNo(gpuCullingPrerequisitesReady)
                + " gpuExecuted=" + yesNo(actualGpuCullingExecuted)
                + " indirect=" + readiness(indirectDrawCount)
                + " indirectReady=" + yesNo(indirectDrawReady);
    }

    public String evidenceBoundaryLine() {
        if (actualGpuCullingExecuted) {
            return "Round 9 evidence boundary: GPU culling execution reported by telemetry; terrain rendering replacement still requires controller proof";
        }
        return "Round 9 evidence boundary: CPU/conservative culling status is live; actual GPU culling not proven; terrain rendering remains vanilla/unchanged";
    }

    public boolean hasInvalidCounts() {
        if (clusterCount == null) {
            return false;
        }
        long visible = zeroIfNull(visibleClusterCount);
        long hidden = zeroIfNull(culledClusterCount) + zeroIfNull(offscreenClusterCount);
        return visible > clusterCount || hidden > clusterCount || visible + hidden > clusterCount;
    }

    public boolean hasNonzeroCulling() {
        return zeroIfNull(culledClusterCount) + zeroIfNull(offscreenClusterCount) > 0L;
    }

    public boolean hasReactiveVisibility() {
        return telemetryPresent && clusterCount != null && visibleClusterCount != null;
    }

    private Long totalCulledOrOffscreen() {
        if (culledClusterCount == null && offscreenClusterCount == null) {
            return null;
        }
        return zeroIfNull(culledClusterCount) + zeroIfNull(offscreenClusterCount);
    }

    private String cullingModeLabel() {
        if (!telemetryPresent) {
            return "unavailable";
        }
        if (actualGpuCullingExecuted) {
            return executionModeLabel;
        }
        if ("metadata-only-placeholder".equalsIgnoreCase(nativeMode)) {
            return "CPU/conservative first-pass";
        }
        if (nativeMode.toLowerCase(Locale.ROOT).contains("cpu")
                || nativeMode.toLowerCase(Locale.ROOT).contains("conservative")) {
            return nativeMode;
        }
        return "CPU/conservative first-pass";
    }

    private String visibilityState() {
        if (!telemetryPresent) {
            return "missing telemetry";
        }
        if (hasInvalidCounts()) {
            return "invalid(counts exceed clusters)";
        }
        if (clusterCount == null || visibleClusterCount == null) {
            return "incomplete";
        }
        if (clusterCount == 0L) {
            return "zero(no clusters)";
        }
        if (!hasNonzeroCulling()) {
            return "visible-only(no culled/offscreen clusters yet)";
        }
        return "reactive(nonzero hidden/offscreen)";
    }

    private String indirectDrawState() {
        if (indirectDrawCount == null) {
            return "missing";
        }
        if (indirectDrawCount < 0L) {
            return "invalid(negative)";
        }
        if (indirectDrawCount == 0L) {
            return "zero(no commands)";
        }
        if (visibleClusterCount != null && indirectDrawCount > visibleClusterCount) {
            return "invalid(draws exceed visible clusters)";
        }
        return "ready";
    }

    private String valueGuard() {
        if (!telemetryPresent) {
            return "missing telemetry";
        }
        if (!actualGpuCullingExecuted && gpuCullingPrerequisitesReady) {
            return "GPU prerequisites ready but execution not proven: " + gpuCullingBlockerReason;
        }
        if (hasInvalidCounts()) {
            return "invalid cluster/visibility relationship";
        }
        if (clusterCount != null && clusterCount == 0L) {
            return "zero cluster payload";
        }
        if (!hasReactiveVisibility()) {
            return "nonreactive or incomplete visibility counts";
        }
        if (!hasNonzeroCulling()) {
            return "no culled/offscreen clusters reported yet";
        }
        return "counts plausible and culling-reactive";
    }

    private String hiddenReadiness() {
        if (hasInvalidCounts()) {
            return "invalid";
        }
        Long hidden = totalCulledOrOffscreen();
        if (hidden == null) {
            return "missing";
        }
        return hidden > 0L ? "nonzero" : "zero";
    }

    private static String readiness(Long value) {
        if (value == null) {
            return "missing";
        }
        if (value < 0L) {
            return "invalid";
        }
        return value == 0L ? "zero" : "nonzero";
    }

    private static String countState(Long value, String zeroReason) {
        if (value == null) {
            return "missing";
        }
        if (value < 0L) {
            return "invalid(negative)";
        }
        if (value == 0L) {
            return "zero(" + zeroReason + ")";
        }
        return "ready";
    }

    private static String countLabel(Long value) {
        return value == null ? UNKNOWN : Long.toString(value);
    }

    private static long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    private static Long firstLong(String line, String key, String fallbackLine, String fallbackKey) {
        Long value = firstLong(line, key);
        return value == null ? firstLong(fallbackLine, fallbackKey) : value;
    }

    private static Long firstLong(String line, String key) {
        String token = firstToken(line, key);
        if (token.isBlank() || UNKNOWN.equals(token)) {
            return null;
        }
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String firstToken(String line, String key) {
        if (line == null || line.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        String prefix = key + "=";
        for (String token : line.split("[\\s,]+")) {
            if (token.startsWith(prefix)) {
                return token.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static boolean parseTerrainRenderingChanged(String cullingLine) {
        String value = firstToken(cullingLine, "terrainRenderingChanged");
        return "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    private static boolean truthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized)
                || "yes".equals(normalized)
                || "ready".equals(normalized)
                || "1".equals(normalized)
                || "executed".equals(normalized);
    }

    private static String reasonFrom(Round9CullingDebugStatus source, String cullingLine) {
        if (!source.telemetryPresent()) {
            return "awaiting Round 9 runtime telemetry";
        }
        Long hidden = firstLong(source.visibilityCountsLine(), "culled");
        Long offscreen = firstLong(source.visibilityCountsLine(), "offscreen");
        if (zeroIfNull(hidden) + zeroIfNull(offscreen) > 0L) {
            return "conservative CPU visibility rejected offscreen/hidden clusters";
        }
        String mode = firstToken(cullingLine, "mode");
        if ("metadata-only-placeholder".equalsIgnoreCase(mode)) {
            return "native mode still reports metadata placeholder; Java exposes runtime counts";
        }
        return "runtime visibility counts reported";
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
