package net.lucerna.render.frame;

import net.lucerna.render.gbuffer.GBufferSceneDataAttachment;
import net.lucerna.render.gbuffer.GBufferSceneDataKind;
import net.lucerna.render.gbuffer.GBufferSceneDataReadiness;
import net.lucerna.render.gbuffer.GBufferSceneDataSamplingEvidence;
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

    public static SceneFrameDataReadiness fromLiveFrameTarget(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget
    ) {
        return fromLiveFrameTarget(frameConstants, writeIntent, frameTarget, (String) null);
    }

    public static SceneFrameDataReadiness fromLiveFrameTarget(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            String depthSamplingMarker
    ) {
        return from(
                frameConstants,
                writeIntent,
                frameTarget,
                liveFrameTargetAttachments(frameTarget, depthSamplingMarker, null),
                "Scene frame data readiness derived from the live Minecraft frame target; depth texel sampling remains blocked until controller shader-pass proof is reported."
        );
    }

    public static SceneFrameDataReadiness fromLiveFrameTarget(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            String depthSamplingMarker,
            GBufferSceneDataSamplingEvidence depthSamplingEvidence
    ) {
        boolean depthSamplingReady = depthSamplingEvidence != null && depthSamplingEvidence.provesDepthSampling();
        return from(
                frameConstants,
                writeIntent,
                frameTarget,
                liveFrameTargetAttachments(frameTarget, depthSamplingMarker, depthSamplingEvidence),
                depthSamplingReady
                        ? "Scene frame data readiness derived from the live Minecraft frame target with concrete depth texel sample evidence."
                        : "Scene frame data readiness derived from the live Minecraft frame target; supplied depth sampling evidence is unavailable or not yet sufficient."
        );
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

    public boolean depthViewPresent() {
        return buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::depthViewPresent)
                .orElse(false);
    }

    public boolean depthTextureSampleBindingReady() {
        return buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::depthTextureSampleBindingReady)
                .orElse(false);
    }

    public boolean depthSamplingEvidenceReady() {
        return buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::depthSamplingEvidenceReady)
                .orElse(false);
    }

    public int depthSampleCount() {
        return buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::depthSampleCount)
                .orElse(0);
    }

    public int sceneSampleCount(GBufferSceneDataKind kind) {
        Objects.requireNonNull(kind, "kind");
        return buffer(kind)
                .map(GBufferSceneDataReadiness::sceneSampleCount)
                .orElse(0);
    }

    public double minDepth() {
        return depthSamplingEvidenceReady()
                ? buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::minNormalizedDepth)
                .orElse(0.0D)
                : 0.0D;
    }

    public double maxDepth() {
        return depthSamplingEvidenceReady()
                ? buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::maxNormalizedDepth)
                .orElse(0.0D)
                : 0.0D;
    }

    public String depthSamplingMarker() {
        return buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::depthSamplingMarker)
                .orElse("depth-readiness-missing");
    }

    public String depthSamplingBlocker() {
        return buffer(GBufferSceneDataKind.DEPTH)
                .map(GBufferSceneDataReadiness::depthSamplingBlocker)
                .orElse("Depth readiness entry is missing.");
    }

    public boolean trueNormalSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.NORMAL);
    }

    public int normalSampleCount() {
        return sceneSampleCount(GBufferSceneDataKind.NORMAL);
    }

    public boolean trueMaterialIdSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.MATERIAL);
    }

    public int materialIdSampleCount() {
        return sceneSampleCount(GBufferSceneDataKind.MATERIAL);
    }

    public boolean trueAlbedoSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.ALBEDO);
    }

    public int albedoSampleCount() {
        return sceneSampleCount(GBufferSceneDataKind.ALBEDO);
    }

    public boolean trueEmissiveSamplingReady() {
        return trueSampledSceneDataReady(GBufferSceneDataKind.EMISSIVE);
    }

    public int emissiveSampleCount() {
        return sceneSampleCount(GBufferSceneDataKind.EMISSIVE);
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

    public boolean trueLightingSceneDataSamplingReady() {
        if (!this.frameConstantsReady) {
            return false;
        }
        for (GBufferSceneDataKind kind : GBufferSceneDataKind.lightingRequired()) {
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

    public List<String> trueLightingSceneDataBlockers() {
        List<String> blockers = new ArrayList<>();
        if (!this.frameConstantsReady) {
            blockers.add("frameConstants=missingRequiredConstants");
        }
        for (GBufferSceneDataKind kind : GBufferSceneDataKind.lightingRequired()) {
            Optional<GBufferSceneDataReadiness> readiness = buffer(kind);
            if (readiness.isEmpty()) {
                blockers.add(kind.label() + "=readinessMissing");
                continue;
            }
            GBufferSceneDataReadiness buffer = readiness.get();
            if (!buffer.trueSampledSceneDataReady()) {
                blockers.add(kind.label()
                        + "=" + buffer.statusCode()
                        + ":source=" + buffer.frameDataSource().kind().label()
                        + ":samples=" + buffer.sceneSampleCount());
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

    public String trueLightingSceneDataBlockerSummary() {
        if (trueLightingSceneDataSamplingReady()) {
            return "ready";
        }
        return String.join(",", trueLightingSceneDataBlockers());
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
                + ", depthViewPresent=" + depthViewPresent()
                + ", depthTextureSampleBindingReady=" + depthTextureSampleBindingReady()
                + ", depthSamplingEvidenceReady=" + depthSamplingEvidenceReady()
                + ", depthSampleCount=" + depthSampleCount()
                + ", minDepth=" + minDepth()
                + ", maxDepth=" + maxDepth()
                + ", depthSamplingMarker=" + depthSamplingMarker()
                + ", depthSamplingBlocker=" + depthSamplingBlocker()
                + ", trueNormalSamplingReady=" + trueNormalSamplingReady()
                + ", normalSampleCount=" + normalSampleCount()
                + ", trueMaterialIdSamplingReady=" + trueMaterialIdSamplingReady()
                + ", materialIdSampleCount=" + materialIdSampleCount()
                + ", trueAlbedoSamplingReady=" + trueAlbedoSamplingReady()
                + ", albedoSampleCount=" + albedoSampleCount()
                + ", trueEmissiveSamplingReady=" + trueEmissiveSamplingReady()
                + ", emissiveSampleCount=" + emissiveSampleCount()
                + ", trueMotionHistorySamplingReady=" + trueMotionHistorySamplingReady()
                + ", truePhysicalGiShadowSamplingReady=" + truePhysicalGiShadowSamplingReady()
                + ", trueLightingSceneDataSamplingReady=" + trueLightingSceneDataSamplingReady()
                + ", anySyntheticOrContractFrameData=" + anySyntheticOrContractFrameData()
                + ", syntheticOrContractKinds=" + syntheticOrContractKinds()
                + ", trueSamplingBlockers=" + trueSamplingBlockerSummary()
                + ", trueLightingSceneDataBlockers=" + trueLightingSceneDataBlockerSummary()
                + ", blockers=" + blockerSummary();
    }

    private static List<GBufferSceneDataAttachment> liveFrameTargetAttachments(
            LucernaFramePassTarget frameTarget,
            String depthSamplingMarker,
            GBufferSceneDataSamplingEvidence depthSamplingEvidence
    ) {
        if (frameTarget == null || frameTarget.attachmentMetadata() == null) {
            return List.of();
        }
        LucernaFrameAttachmentMetadata metadata = frameTarget.attachmentMetadata();
        if (!metadata.depthViewPresent()) {
            return List.of();
        }
        String marker = depthSamplingMarker == null || depthSamplingMarker.isBlank()
                ? "public-mojang-depth-view-shader-proof-required"
                : depthSamplingMarker.trim();
        String depthBlocker = depthSamplingEvidence == null
                ? "actual depth texel sampling requires controller proof from a shader pass"
                : depthSamplingEvidence.depthSamplingBlocker();
        String description = marker
                + "; format=" + metadata.depthFormat()
                + "; layout=" + metadata.depthLayout()
                + "; depthHandle=" + metadata.depthImageHandle()
                + "; depthViewHandle=" + metadata.depthImageViewHandle()
                + "; blocker=" + depthBlocker;
        if (depthSamplingEvidence != null && depthSamplingEvidence.provesDepthSampling()) {
            String sampledDescription = marker
                    + "; format=" + metadata.depthFormat()
                    + "; layout=" + metadata.depthLayout()
                    + "; depthHandle=" + metadata.depthImageHandle()
                    + "; depthViewHandle=" + metadata.depthImageViewHandle()
                    + "; sampleCount=" + depthSamplingEvidence.sampleCount()
                    + "; nonzeroDepthSampleCount=" + depthSamplingEvidence.nonzeroDepthSampleCount()
                    + "; source=live-frame-target-depth-sampled-by-shader-pass";
            return List.of(GBufferSceneDataAttachment.mojangSampled(
                    GBufferSceneDataKind.DEPTH,
                    metadata.width(),
                    metadata.height(),
                    metadata.depthFormat(),
                    metadata.depthLayout(),
                    metadata.depthImageHandle(),
                    metadata.depthImageViewHandle(),
                    sampledDescription,
                    depthSamplingEvidence
            ));
        }
        return List.of(GBufferSceneDataAttachment.publicMojangDepthView(
                metadata.width(),
                metadata.height(),
                metadata.depthFormat(),
                metadata.depthLayout(),
                metadata.depthImageHandle(),
                metadata.depthImageViewHandle(),
                description
        ));
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
