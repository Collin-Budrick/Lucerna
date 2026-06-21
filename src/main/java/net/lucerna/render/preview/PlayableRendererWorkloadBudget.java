package net.lucerna.render.preview;

import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;
import net.lucerna.config.LucernaConfig;
import net.lucerna.config.QualityPreset;

import java.util.Locale;

public final class PlayableRendererWorkloadBudget {
    private static final String ENABLE_ENV = "LUCERNA_PLAYABLE_PHYSICAL_RENDERER";
    private static final String CADENCE_ENV = "LUCERNA_PLAYABLE_PHYSICAL_RENDERER_CADENCE_TICKS";
    private static final int MIN_CADENCE_TICKS = 1;
    private static final int MAX_CADENCE_TICKS = 600;

    private long tickIndex;

    public Decision nextDecision(LucernaConfig config, BackendStatus backendStatus) {
        boolean rendererEnabled = config != null && config.rendererEnabled();
        boolean sodiumVulkan = backendStatus != null && backendStatus.kind() == BackendKind.SODIUM_VULKAN;
        boolean envEnabled = envTruthy(ENABLE_ENV);
        int cadenceTicks = cadenceTicks(config);
        long nextTick = ++this.tickIndex;
        boolean budgeted = rendererEnabled && sodiumVulkan && envEnabled;
        boolean dispatchThisTick = budgeted && ((nextTick - 1L) % cadenceTicks == 0L);
        String reason;
        if (!rendererEnabled) {
            reason = "renderer config is disabled";
        } else if (!sodiumVulkan) {
            reason = "backend is not Sodium Vulkan";
        } else if (!envEnabled) {
            reason = "explicit playable physical renderer env gate is disabled";
        } else if (dispatchThisTick) {
            reason = "budget cadence selected this tick for physical renderer dispatch";
        } else {
            reason = "budget cadence skipped this tick to keep gameplay playable";
        }
        return new Decision(budgeted, cadenceTicks, dispatchThisTick, nextTick, reason);
    }

    public void reset() {
        this.tickIndex = 0L;
    }

    public static boolean playablePhysicalRendererEnvEnabled() {
        return envTruthy(ENABLE_ENV);
    }

    public static String cadenceContractSummary(LucernaConfig config, BackendStatus backendStatus) {
        boolean rendererEnabled = config != null && config.rendererEnabled();
        boolean sodiumVulkan = backendStatus != null && backendStatus.kind() == BackendKind.SODIUM_VULKAN;
        boolean envEnabled = playablePhysicalRendererEnvEnabled();
        int cadenceTicks = cadenceTicks(config);
        return "playablePhysicalRendererBudgeted=" + (rendererEnabled && sodiumVulkan && envEnabled)
                + ",normalPlayablePath=" + (!envEnabled)
                + ",rendererEnabled=" + rendererEnabled
                + ",sodiumVulkan=" + sodiumVulkan
                + ",envGateEnabled=" + envEnabled
                + ",dispatchCadenceTicks=" + cadenceTicks
                + ",heavyProofWorkload=false"
                + ",budgetContract=opt-in-cadenced-physical-dispatch";
    }

    private static int cadenceTicks(LucernaConfig config) {
        int defaultCadence = defaultCadenceTicks(config == null ? null : config.qualityPreset());
        return clampInt(parsePositiveInt(System.getenv(CADENCE_ENV), defaultCadence), MIN_CADENCE_TICKS, MAX_CADENCE_TICKS);
    }

    private static int defaultCadenceTicks(QualityPreset preset) {
        if (preset == QualityPreset.EXPERIMENTAL) {
            return 10;
        }
        if (preset == QualityPreset.QUALITY) {
            return 20;
        }
        if (preset == QualityPreset.PERFORMANCE) {
            return 80;
        }
        return 40;
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean envTruthy(String key) {
        String value = System.getenv(key);
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("1")
                || normalized.equals("true")
                || normalized.equals("yes")
                || normalized.equals("on");
    }

    public record Decision(
            boolean playablePhysicalRendererBudgeted,
            int dispatchCadenceTicks,
            boolean dispatchThisTick,
            long tickIndex,
            String reason
    ) {
        public boolean normalPlayablePath() {
            return !this.playablePhysicalRendererBudgeted;
        }

        public boolean proofDispatchBudgetedThisTick() {
            return this.playablePhysicalRendererBudgeted && this.dispatchThisTick;
        }

        public String compactStatusSummary() {
            return "playablePhysicalRendererBudgeted=" + this.playablePhysicalRendererBudgeted
                    + ",normalPlayablePath=" + this.normalPlayablePath()
                    + ",dispatchCadenceTicks=" + this.dispatchCadenceTicks
                    + ",dispatchThisTick=" + this.dispatchThisTick
                    + ",proofDispatchBudgetedThisTick=" + this.proofDispatchBudgetedThisTick()
                    + ",tickIndex=" + this.tickIndex
                    + ",heavyProofWorkload=false"
                    + ",reason=\"" + this.reason + "\"";
        }

        public String blockerSummary() {
            if (this.playablePhysicalRendererBudgeted) {
                return this.dispatchThisTick
                        ? "none; cadence selected this tick"
                        : "cadence skip; proof dispatch deferred to preserve playable FPS";
            }
            return this.reason;
        }
    }
}
