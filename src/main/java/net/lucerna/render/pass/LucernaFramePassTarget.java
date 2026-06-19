package net.lucerna.render.pass;

public record LucernaFramePassTarget(
        Object renderPass,
        Object commandEncoder,
        Object colorTarget,
        Object depthTarget,
        boolean safeMojangRenderPass,
        LucernaFramePassPhase phase,
        String description
) {
    public LucernaFramePassTarget {
        if (phase == null) {
            phase = LucernaFramePassPhase.UNKNOWN;
        }
        if (description == null || description.isBlank()) {
            description = renderPass == null
                    ? "No frame pass target was supplied."
                    : "Opaque frame pass target.";
        } else {
            description = description.trim();
        }
    }

    public static LucernaFramePassTarget absent(String reason) {
        return new LucernaFramePassTarget(null, null, null, null, false, LucernaFramePassPhase.UNKNOWN, reason);
    }

    public static LucernaFramePassTarget unsafe(Object renderPass, String reason) {
        return new LucernaFramePassTarget(renderPass, null, null, null, false, LucernaFramePassPhase.UNKNOWN, reason);
    }

    public static LucernaFramePassTarget safeMojangRenderPass(
            Object renderPass,
            Object commandEncoder,
            Object colorTarget,
            Object depthTarget,
            String description
    ) {
        return safeWorldColorBeforeHud(renderPass, commandEncoder, colorTarget, depthTarget, description);
    }

    public static LucernaFramePassTarget safeWorldColorBeforeHud(
            Object renderPass,
            Object commandEncoder,
            Object colorTarget,
            Object depthTarget,
            String description
    ) {
        return new LucernaFramePassTarget(
                renderPass,
                commandEncoder,
                colorTarget,
                depthTarget,
                true,
                LucernaFramePassPhase.WORLD_COLOR_BEFORE_HUD,
                description
        );
    }

    public boolean available() {
        return this.renderPass != null;
    }

    public boolean safeForAttachment() {
        return this.available() && this.safeMojangRenderPass && this.phase.safeForLightingComposite();
    }

    public boolean preservesHud() {
        return this.phase.hudPreserving();
    }

    public boolean worldColorTarget() {
        return this.phase.worldColorTarget();
    }
}
