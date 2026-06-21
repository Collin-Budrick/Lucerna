package net.lucerna.render.frame;

import net.lucerna.render.gbuffer.GBufferSceneDataAttachment;
import net.lucerna.render.gbuffer.GBufferSceneDataKind;
import net.lucerna.render.gbuffer.GBufferSceneDataReadiness;
import net.lucerna.render.gbuffer.GBufferTargetContract;
import net.lucerna.render.gbuffer.GBufferWriteIntent;
import net.lucerna.render.pass.LucernaFrameAttachmentMetadata;
import net.lucerna.render.pass.LucernaFramePassTarget;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record SceneFrameDataReadiness(
        long frameIndex,
        long gBufferGeneration,
        int expectedWidth,
        int expectedHeight,
        boolean frameConstantsReady,
        boolean frameTargetAvailable,
        boolean frameTargetSafe,
        boolean frameAttachmentMetadataOnly,
        boolean frameAttachmentJavaOpaque,
        boolean frameAttachmentNativeWritable,
        String frameTargetStatus,
        List<GBufferSceneDataReadiness> buffers,
        String sourceDescription
) {
    public SceneFrameDataReadiness {
        frameIndex = Math.max(0L, frameIndex);
        gBufferGeneration = Math.max(0L, gBufferGeneration);
        expectedWidth = Math.max(0, expectedWidth);
        expectedHeight = Math.max(0, expectedHeight);
        if (frameTargetStatus == null || frameTargetStatus.isBlank()) {
            frameTargetStatus = "frameTarget=unavailable";
        } else {
            frameTargetStatus = frameTargetStatus.trim();
        }
        Objects.requireNonNull(buffers, "buffers");
        buffers = List.copyOf(buffers);
        for (GBufferSceneDataReadiness buffer : buffers) {
            Objects.requireNonNull(buffer, "buffers must not contain null entries");
        }
        if (sourceDescription == null || sourceDescription.isBlank()) {
            sourceDescription = "Scene frame data readiness from frame constants, G-buffer intent, frame target, and explicit scene attachment views.";
        } else {
            sourceDescription = sourceDescription.trim();
        }
    }

    public static SceneFrameDataReadiness unavailable(String reason) {
        return from(
                LucernaFrameConstants.unavailable(),
                GBufferWriteIntent.empty(0L),
                null,
                List.of(),
                reason
        );
    }

    public static SceneFrameDataReadiness from(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            List<GBufferSceneDataAttachment> frameDataAttachments
    ) {
        return from(frameConstants, writeIntent, frameTarget, frameDataAttachments, null);
    }

    public static SceneFrameDataReadiness from(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            List<GBufferSceneDataAttachment> frameDataAttachments,
            String sourceDescription
    ) {
        LucernaFrameConstants resolvedConstants = frameConstants == null
                ? LucernaFrameConstants.unavailable()
                : frameConstants;
        GBufferWriteIntent resolvedWriteIntent = writeIntent == null
                ? GBufferWriteIntent.empty(resolvedConstants.frameIndex())
                : writeIntent;
        Map<GBufferSceneDataKind, GBufferSceneDataAttachment> attachmentsByKind = attachmentsByKind(frameDataAttachments);
        boolean targetAvailable = frameTarget != null && frameTarget.available();
        boolean targetSafe = frameTarget != null && frameTarget.safeForAttachment();
        LucernaFrameAttachmentMetadata metadata = frameTarget == null ? null : frameTarget.attachmentMetadata();
        boolean metadataOnly = metadata == null || metadata.metadataOnly();
        boolean javaOpaque = metadata != null && metadata.javaOpaque();
        boolean nativeWritable = frameTarget != null && frameTarget.nativeWritableAttachment();
        String targetStatus = targetStatusLabel(frameTarget, metadata);

        List<GBufferSceneDataReadiness> buffers = new ArrayList<>();
        for (GBufferSceneDataKind kind : GBufferSceneDataKind.frameReadinessTracked()) {
            buffers.add(GBufferSceneDataReadiness.from(
                    kind,
                    GBufferTargetContract.lucernaMain(),
                    resolvedWriteIntent,
                    targetAvailable,
                    targetSafe,
                    metadataOnly,
                    javaOpaque,
                    nativeWritable,
                    attachmentsByKind.get(kind)
            ));
        }

        return new SceneFrameDataReadiness(
                resolvedConstants.frameIndex(),
                resolvedWriteIntent.generation(),
                resolvedWriteIntent.width(),
                resolvedWriteIntent.height(),
                resolvedConstants.hasRequiredConstants(),
                targetAvailable,
                targetSafe,
                metadataOnly,
                javaOpaque,
                nativeWritable,
                targetStatus,
                buffers,
                sourceDescription
        );
    }

    public boolean readyForLighting() {
        return this.frameConstantsReady
                && this.buffers.stream()
                .filter(GBufferSceneDataReadiness::requiredForLighting)
                .allMatch(GBufferSceneDataReadiness::ready);
    }

    public Optional<GBufferSceneDataReadiness> buffer(GBufferSceneDataKind kind) {
        Objects.requireNonNull(kind, "kind");
        return this.buffers.stream()
                .filter(buffer -> buffer.kind() == kind)
                .findFirst();
    }

    public boolean trueDepthSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.DEPTH);
    }

    public boolean trueNormalSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.NORMAL);
    }

    public boolean trueMaterialIdSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.MATERIAL);
    }

    public boolean trueMotionHistorySamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.MOTION_HISTORY);
    }

    public boolean truePhysicalGiShadowSamplingReady() {
        if (!this.frameConstantsReady) {
            return false;
        }
        for (GBufferSceneDataKind kind : GBufferSceneDataKind.physicalGiShadowSamplingRequired()) {
            if (!trueSampledSceneDataReady(kind)) {
                return false;
            }
        }
        return true;
    }

    public boolean anySyntheticOrContractFrameData() {
        return this.buffers.stream()
                .anyMatch(buffer -> buffer.frameDataSynthetic() || buffer.frameDataContractOnly());
    }

    public List<GBufferSceneDataKind> syntheticOrContractKinds() {
        return this.buffers.stream()
                .filter(buffer -> buffer.frameDataSynthetic() || buffer.frameDataContractOnly())
                .map(GBufferSceneDataReadiness::kind)
                .toList();
    }

    public List<GBufferSceneDataKind> unreadyKinds() {
        return this.buffers.stream()
                .filter(buffer -> !buffer.ready())
                .map(GBufferSceneDataReadiness::kind)
                .toList();
    }

    public List<String> blockers() {
        List<String> blockers = new ArrayList<>();
        if (!this.frameConstantsReady) {
            blockers.add("frameConstants=missingRequiredConstants");
        }
        for (GBufferSceneDataReadiness buffer : this.buffers) {
            if (!buffer.ready()) {
                blockers.add(buffer.kind().label() + "=" + buffer.statusCode());
            }
        }
        return List.copyOf(blockers);
    }

    public List<String> trueSamplingBlockers() {
        List<String> blockers = new ArrayList<>();
        if (!this.frameConstantsReady) {
            blockers.add("frameConstants=missingRequiredConstants");
        }
        for (GBufferSceneDataKind kind : GBufferSceneDataKind.physicalGiShadowSamplingRequired()) {
            Optional<GBufferSceneDataReadiness> readiness = buffer(kind);
            if (readiness.isEmpty()) {
                blockers.add(kind.label() + "=readinessMissing");
                continue;
            }
            GBufferSceneDataReadiness buffer = readiness.get();
            if (!buffer.trueSampledSceneDataReady()) {
                blockers.add(kind.label()
                        + "=" + buffer.statusCode()
                        + ":source=" + buffer.frameDataSource().kind().label());
            }
        }
        return List.copyOf(blockers);
    }

    public String blockerSummary() {
        if (readyForLighting()) {
            return "ready";
        }
        return String.join(",", blockers());
    }

    public String trueSamplingBlockerSummary() {
        if (truePhysicalGiShadowSamplingReady()) {
            return "ready";
        }
        return String.join(",", trueSamplingBlockers());
    }

    public String statusLabel() {
        return "frameIndex=" + this.frameIndex
                + ", gBufferGeneration=" + this.gBufferGeneration
                + ", expectedExtent=" + this.expectedWidth + "x" + this.expectedHeight
                + ", frameConstantsReady=" + this.frameConstantsReady
                + ", frameTargetAvailable=" + this.frameTargetAvailable
                + ", frameTargetSafe=" + this.frameTargetSafe
                + ", frameAttachmentMetadataOnly=" + this.frameAttachmentMetadataOnly
                + ", frameAttachmentJavaOpaque=" + this.frameAttachmentJavaOpaque
                + ", frameAttachmentNativeWritable=" + this.frameAttachmentNativeWritable
                + ", readyForLighting=" + readyForLighting()
                + ", trueDepthSamplingReady=" + trueDepthSamplingReady()
                + ", trueNormalSamplingReady=" + trueNormalSamplingReady()
                + ", trueMaterialIdSamplingReady=" + trueMaterialIdSamplingReady()
                + ", trueMotionHistorySamplingReady=" + trueMotionHistorySamplingReady()
                + ", truePhysicalGiShadowSamplingReady=" + truePhysicalGiShadowSamplingReady()
                + ", anySyntheticOrContractFrameData=" + anySyntheticOrContractFrameData()
                + ", syntheticOrContractKinds=" + syntheticOrContractKinds()
                + ", trueSamplingBlockers=" + trueSamplingBlockerSummary()
                + ", blockers=" + blockerSummary();
    }

    private boolean trueSampledSceneDataReady(GBufferSceneDataKind kind) {
        return buffer(kind)
                .map(GBufferSceneDataReadiness::trueSampledSceneDataReady)
                .orElse(false);
    }

    private static Map<GBufferSceneDataKind, GBufferSceneDataAttachment> attachmentsByKind(
            List<GBufferSceneDataAttachment> frameDataAttachments
    ) {
        Map<GBufferSceneDataKind, GBufferSceneDataAttachment> attachmentsByKind =
                new EnumMap<>(GBufferSceneDataKind.class);
        if (frameDataAttachments == null) {
            return attachmentsByKind;
        }
        for (GBufferSceneDataAttachment attachment : frameDataAttachments) {
            Objects.requireNonNull(attachment, "frameDataAttachments must not contain null entries");
            attachmentsByKind.put(attachment.kind(), attachment);
        }
        return attachmentsByKind;
    }

    private static String targetStatusLabel(
            LucernaFramePassTarget frameTarget,
            LucernaFrameAttachmentMetadata metadata
    ) {
        if (frameTarget == null) {
            return "frameTarget=absent";
        }
        String metadataStatus = metadata == null ? "metadata=absent" : metadata.attachmentStatusLabel();
        return "available=" + frameTarget.available()
                + ", safeForAttachment=" + frameTarget.safeForAttachment()
                + ", worldColorTarget=" + frameTarget.worldColorTarget()
                + ", preservesHud=" + frameTarget.preservesHud()
                + ", " + metadataStatus;
    }
}
