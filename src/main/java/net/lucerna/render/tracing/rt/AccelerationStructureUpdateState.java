package net.lucerna.render.tracing.rt;

public enum AccelerationStructureUpdateState {
    NOT_REQUESTED("No acceleration-structure work was requested."),
    FALLBACK_UNAVAILABLE("Required Vulkan RT capability is unavailable; fallback path is active."),
    PENDING_INPUTS("Acceleration-structure inputs are incomplete."),
    METADATA_READY("Acceleration-structure metadata is ready for a future native build."),
    BUILD_QUEUED("Acceleration-structure build work is queued but not proven complete."),
    BUILT_ON_DEVICE("Native telemetry reports a completed device acceleration-structure build."),
    FAILED("Acceleration-structure build/update failed.");

    private final String description;

    AccelerationStructureUpdateState(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
