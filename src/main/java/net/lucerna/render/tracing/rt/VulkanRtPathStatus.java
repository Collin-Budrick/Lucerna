package net.lucerna.render.tracing.rt;

public record VulkanRtPathStatus(
        VulkanRtCapabilityStatus capabilityStatus,
        BlasBuildStatus blasStatus,
        TlasBuildStatus tlasStatus,
        EntityAccelerationStructureUpdateSummary entityUpdateSummary,
        boolean fallbackActive,
        String fallbackReason,
        boolean hardwareRtExecutionClaimed,
        String evidenceBoundary
) {
    private static final String DEFAULT_BOUNDARY =
            "Round 10 RT path is status/contract scaffolding; hardware RT execution requires native telemetry.";

    public VulkanRtPathStatus {
        if (capabilityStatus == null) {
            capabilityStatus = VulkanRtCapabilityStatus.unavailable(
                    "unwired",
                    "No Vulkan RT capability detector is wired."
            );
        }
        if (blasStatus == null) {
            blasStatus = BlasBuildStatus.fallback(capabilityStatus.fallbackReason());
        }
        if (tlasStatus == null) {
            tlasStatus = TlasBuildStatus.fallback(capabilityStatus.fallbackReason());
        }
        if (entityUpdateSummary == null) {
            entityUpdateSummary = EntityAccelerationStructureUpdateSummary.empty(0L, "unwired");
        }

        fallbackActive = fallbackActive
                || capabilityStatus.fallbackActive()
                || blasStatus.state() == AccelerationStructureUpdateState.FALLBACK_UNAVAILABLE
                || tlasStatus.state() == AccelerationStructureUpdateState.FALLBACK_UNAVAILABLE;
        fallbackReason = clean(
                fallbackReason,
                fallbackActive ? capabilityStatus.fallbackReason() : "RT metadata path is ready; native execution is still unproven."
        );
        hardwareRtExecutionClaimed = capabilityStatus.hardwareRayTracingReady()
                && blasStatus.hardwareRtExecutionClaimed()
                && tlasStatus.hardwareRtExecutionClaimed()
                && hardwareRtExecutionClaimed;
        evidenceBoundary = clean(evidenceBoundary, DEFAULT_BOUNDARY);
    }

    public static VulkanRtPathStatus fallbackUnavailable(String source, String reason) {
        VulkanRtCapabilityStatus capability = VulkanRtCapabilityStatus.unavailable(source, reason);
        return new VulkanRtPathStatus(
                capability,
                BlasBuildStatus.fallback(reason),
                TlasBuildStatus.fallback(reason),
                EntityAccelerationStructureUpdateSummary.empty(0L, source),
                true,
                reason,
                false,
                DEFAULT_BOUNDARY
        );
    }

    public String summary() {
        return this.capabilityStatus.summary()
                + ",blas=" + this.blasStatus.state()
                + ",tlas=" + this.tlasStatus.state()
                + ",entityAs={" + this.entityUpdateSummary.summary() + "}"
                + ",fallback=" + this.fallbackActive
                + ",hardwareRtExecutionClaimed=" + this.hardwareRtExecutionClaimed;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
