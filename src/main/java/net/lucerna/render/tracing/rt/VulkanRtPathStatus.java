package net.lucerna.render.tracing.rt;

public record VulkanRtPathStatus(
        VulkanRtCapabilityStatus capabilityStatus,
        BlasBuildStatus blasStatus,
        TlasBuildStatus tlasStatus,
        EntityAccelerationStructureUpdateSummary entityUpdateSummary,
        boolean fallbackActive,
        String fallbackReason,
        boolean hardwareRtExecutionClaimed,
        boolean realHardwareRtExecutionExecuted,
        String hardwareRtExecutionStatus,
        String evidenceBoundary
) {
    private static final String DEFAULT_BOUNDARY =
            "Round 10 RT path is status/contract scaffolding; hardware RT execution requires native telemetry.";
    private static final String DEFAULT_HARDWARE_RT_STATUS =
            "hardwareRtExecution=false; Java status contracts are ready but real RT execution is not proven.";

    public VulkanRtPathStatus(
            VulkanRtCapabilityStatus capabilityStatus,
            BlasBuildStatus blasStatus,
            TlasBuildStatus tlasStatus,
            EntityAccelerationStructureUpdateSummary entityUpdateSummary,
            boolean fallbackActive,
            String fallbackReason,
            boolean hardwareRtExecutionClaimed,
            String evidenceBoundary
    ) {
        this(
                capabilityStatus,
                blasStatus,
                tlasStatus,
                entityUpdateSummary,
                fallbackActive,
                fallbackReason,
                hardwareRtExecutionClaimed,
                false,
                DEFAULT_HARDWARE_RT_STATUS,
                evidenceBoundary
        );
    }

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
        realHardwareRtExecutionExecuted = hardwareRtExecutionClaimed && realHardwareRtExecutionExecuted;
        hardwareRtExecutionStatus = clean(
                hardwareRtExecutionStatus,
                realHardwareRtExecutionExecuted
                        ? "hardwareRtExecution=true; native telemetry explicitly reported RT entity execution."
                        : DEFAULT_HARDWARE_RT_STATUS
        );
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
                false,
                DEFAULT_HARDWARE_RT_STATUS,
                DEFAULT_BOUNDARY
        );
    }

    public String summary() {
        return this.capabilityStatus.summary()
                + ",blas=" + this.blasStatus.state()
                + ",tlas=" + this.tlasStatus.state()
                + ",entityAs={" + this.entityUpdateSummary.summary() + "}"
                + ",fallback=" + this.fallbackActive
                + ",fallbackReason=" + this.fallbackReason
                + ",hardwareRtExecutionClaimed=" + this.hardwareRtExecutionClaimed
                + ",realHardwareRtExecutionExecuted=" + this.realHardwareRtExecutionExecuted
                + ",hardwareRtExecutionStatus=" + this.hardwareRtExecutionStatus;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
