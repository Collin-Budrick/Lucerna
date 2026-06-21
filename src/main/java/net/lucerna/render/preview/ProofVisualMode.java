package net.lucerna.render.preview;

import net.lucerna.config.DebugOverlay;

import java.util.Locale;
import java.util.Map;

public final class ProofVisualMode {
    private static final String HIDE_PROOF_OVERLAYS_ENV = "LUCERNA_HIDE_PROOF_OVERLAYS";
    private static final String SHOW_PROOF_OVERLAYS_ENV = "LUCERNA_SHOW_PROOF_OVERLAYS";
    private static final String EXPERIMENTAL_VISUAL_STACK_ENV = "LUCERNA_EXPERIMENTAL_VISUAL_STACK";
    private static final String CPU_DIRECT_TEXTURE_COMPOSITE_ENV = "LUCERNA_ENABLE_CPU_DIRECT_TEXTURE_COMPOSITE";
    private static final String CONTROLLER_VALIDATION_ENV = "LUCERNA_CONTROLLER_VALIDATION";
    private static final String VALIDATION_VISUALS_ENV = "LUCERNA_VALIDATION_VISUALS";
    private static final String CONTROLLER_SCREENSHOT_REQUEST_ENV = "LUCERNA_CONTROLLER_SCREENSHOT_REQUEST";
    private static final String ROUND_ENV_PREFIX = "LUCERNA_ROUND";

    private ProofVisualMode() {
    }

    public static boolean directLightProofOverlayAllowed(DebugOverlay overlay) {
        return proofOverlayAllowed(overlay);
    }

    public static boolean round6GiProofOverlayAllowed(DebugOverlay overlay) {
        return proofOverlayAllowed(overlay);
    }

    public static boolean proofOverlaysHidden() {
        return envTruthy(System.getenv(HIDE_PROOF_OVERLAYS_ENV));
    }

    public static boolean experimentalVisualStackAllowed() {
        if (proofOverlaysHidden()) {
            return false;
        }
        Map<String, String> env = System.getenv();
        return envTruthy(env.get(EXPERIMENTAL_VISUAL_STACK_ENV))
                || envTruthy(env.get(VALIDATION_VISUALS_ENV));
    }

    public static boolean cpuDirectTextureCompositeAllowed() {
        return envTruthy(System.getenv(CPU_DIRECT_TEXTURE_COMPOSITE_ENV));
    }

    private static boolean proofOverlayAllowed(DebugOverlay overlay) {
        if (proofOverlaysHidden()) {
            return false;
        }
        return controllerValidationEnvironmentActive(System.getenv()) || proofDebugOverlayActive(overlay);
    }

    private static boolean proofDebugOverlayActive(DebugOverlay overlay) {
        return overlay == DebugOverlay.DIRECT_LIGHTING
                || overlay == DebugOverlay.FIRST_LIGHTING_QUALITY
                || overlay == DebugOverlay.FIRST_LIGHTING_PHYSICAL_PROOF
                || overlay == DebugOverlay.SHADER_DENOISE_TEMPORAL;
    }

    private static boolean controllerValidationEnvironmentActive(Map<String, String> env) {
        if (envTruthy(env.get(SHOW_PROOF_OVERLAYS_ENV))
                || envTruthy(env.get(CONTROLLER_VALIDATION_ENV))
                || envTruthy(env.get(VALIDATION_VISUALS_ENV))
                || envTruthy(env.get(CONTROLLER_SCREENSHOT_REQUEST_ENV))) {
            return true;
        }

        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (!hasText(entry.getValue())) {
                continue;
            }
            String name = entry.getKey().toUpperCase(Locale.ROOT);
            if (name.startsWith("LUCERNA_FINAL_PHYSICAL_COMPOSITE_")
                    || name.startsWith("LUCERNA_PHYSICAL_LIGHTING_")) {
                return true;
            }
            if (!name.startsWith(ROUND_ENV_PREFIX)) {
                continue;
            }
            if (isRoundDiagnosticEnv(name)) {
                if (envTruthy(entry.getValue())) {
                    return true;
                }
                continue;
            }
            if (isRoundControllerMetadataEnv(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRoundControllerMetadataEnv(String name) {
        return name.endsWith("_ARTIFACT_ROLE")
                || name.endsWith("_CAPTURE_MODE")
                || name.endsWith("_VISUAL_PROOF_OWNER")
                || name.endsWith("_SCENE_KIND")
                || name.endsWith("_SCENE_STATE");
    }

    private static boolean isRoundDiagnosticEnv(String name) {
        return name.endsWith("_DIAGNOSTIC");
    }

    private static boolean envTruthy(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized)
                || "1".equals(normalized)
                || "yes".equals(normalized)
                || "on".equals(normalized);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
