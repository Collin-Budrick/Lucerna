package net.lucerna.lighting;

public record LightingPassState(
        boolean directLightingEnabled,
        boolean diffuseGiEnabled,
        boolean denoiseEnabled,
        int directLightCandidateCount,
        int giInternalScale
) {
    public static LightingPassState firstMilestoneDefaults() {
        return new LightingPassState(true, true, true, 8, 2);
    }

    public LightingFrameRequirements frameRequirements() {
        return LightingFrameRequirements.gBufferGiMilestone();
    }
}
