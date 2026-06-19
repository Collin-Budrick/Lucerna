package net.lucerna.lighting;

import net.lucerna.render.frame.LucernaFrameConstants;

import java.util.ArrayList;
import java.util.List;

public record LightingFrameRequirements(
        boolean requiresFrameIndex,
        boolean requiresViewport,
        boolean requiresWorldState,
        boolean requiresCameraMatrices,
        boolean requiresQualityFlags,
        boolean requiresJitter
) {
    public static LightingFrameRequirements gBufferGiMilestone() {
        return new LightingFrameRequirements(true, true, true, true, true, false);
    }

    public boolean satisfiedBy(LucernaFrameConstants constants) {
        return this.missingFrom(constants).isEmpty();
    }

    public List<String> missingFrom(LucernaFrameConstants constants) {
        List<String> missing = new ArrayList<>();
        if (constants == null) {
            missing.add("frameConstants");
            return List.copyOf(missing);
        }
        if (this.requiresFrameIndex && !constants.hasFrameIndex()) {
            missing.add("frameIndex");
        }
        if (this.requiresViewport && !constants.hasViewport()) {
            missing.add("viewport");
        }
        if (this.requiresWorldState && !constants.hasWorldState()) {
            missing.add("worldState");
        }
        if (this.requiresCameraMatrices && !constants.hasCameraMatrices()) {
            missing.add("cameraMatrices");
        }
        if (this.requiresQualityFlags && (constants.flags() == null || !constants.flags().available())) {
            missing.add("qualityFlags");
        }
        if (this.requiresJitter && (constants.jitter() == null || !constants.jitter().available())) {
            missing.add("jitter");
        }
        return List.copyOf(missing);
    }
}
