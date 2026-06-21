package net.lucerna.render.pass;

public record LucernaFramePassTarget(
        Object renderPass,
        Object commandEncoder,
        Object colorTarget,
        Object depthTarget,
        boolean safeMojangRenderPass,
        LucernaFramePassPhase phase,
        String description,
        LucernaFrameAttachmentMetadata attachmentMetadata
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
        if (attachmentMetadata == null) {
            attachmentMetadata = LucernaFrameAttachmentMetadata.metadataOnly(phase, description);
        }
    }

    public LucernaFramePassTarget(
            Object renderPass,
            Object commandEncoder,
            Object colorTarget,
            Object depthTarget,
            boolean safeMojangRenderPass,
            LucernaFramePassPhase phase,
            String description
    ) {
        this(
                renderPass,
                commandEncoder,
                colorTarget,
                depthTarget,
                safeMojangRenderPass,
                phase,
                description,
                LucernaFrameAttachmentMetadata.metadataOnly(phase, description)
        );
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
        return safeWorldColorBeforeHud(renderPass, commandEncoder, colorTarget, depthTarget, description, null);
    }

    public static LucernaFramePassTarget safeWorldColorBeforeHud(
            Object renderPass,
            Object commandEncoder,
            Object colorTarget,
            Object depthTarget,
            String description,
            LucernaFrameAttachmentMetadata attachmentMetadata
    ) {
        return new LucernaFramePassTarget(
                renderPass,
                commandEncoder,
                colorTarget,
                depthTarget,
                true,
                LucernaFramePassPhase.WORLD_COLOR_BEFORE_HUD,
                description,
                attachmentMetadata == null
                        ? LucernaFrameAttachmentMetadata.metadataOnly(
                                LucernaFramePassPhase.WORLD_COLOR_BEFORE_HUD,
                                description
                        )
                        : attachmentMetadata
        );
    }

    public boolean available() {
        return this.renderPass != null;
    }

    public boolean safeForAttachment() {
        return this.available() && this.safeMojangRenderPass && this.phase.safeForLightingComposite();
    }

    public boolean metadataOnlyAttachment() {
        return this.attachmentMetadata.metadataOnly();
    }

    public boolean attachmentMetadataPhaseMatches() {
        return this.attachmentMetadata.phase() == this.phase;
    }

    public boolean nativeWritableAttachment() {
        return this.safeForAttachment()
                && this.attachmentMetadataPhaseMatches()
                && this.attachmentMetadata.nativeWritable();
    }

    public boolean finalCompositeWorldColorAttachmentReady() {
        return this.safeForAttachment()
                && this.attachmentMetadataPhaseMatches()
                && this.attachmentMetadata.finalCompositeColorAttachmentReady();
    }

    public boolean finalCompositeDepthSampleBindingReady() {
        return finalCompositeWorldColorAttachmentReady()
                && this.depthTarget != null
                && this.attachmentMetadata.finalCompositeDepthSampleBindingReady();
    }

    public String finalCompositeSafetyStatusLabel() {
        return "finalCompositeWorldColorAttachmentReady=" + finalCompositeWorldColorAttachmentReady()
                + ", finalCompositeDepthSampleBindingReady=" + finalCompositeDepthSampleBindingReady()
                + ", finalCompositeBeforeHandHud=" + preservesHud()
                + ", finalCompositeTouchesHud=false"
                + ", finalCompositeTouchesHand=false"
                + ", finalCompositeTouchesPostHud=false"
                + ", finalCompositeTranslucencyBoundary=" + this.phase.name();
    }

    public boolean preservesHud() {
        return this.phase.hudPreserving();
    }

    public boolean worldColorTarget() {
        return this.phase.worldColorTarget();
    }
}
