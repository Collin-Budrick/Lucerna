package net.lucerna.lighting;

public enum DenoiseSignalKind {
    DIFFUSE_GI("diffuse_gi"),
    DIRECT_SHADOWS("direct_shadows"),
    SPECULAR_PLACEHOLDER("specular_placeholder"),
    AMBIENT_OCCLUSION_PLACEHOLDER("ambient_occlusion_placeholder");

    private final String statusName;

    DenoiseSignalKind(String statusName) {
        this.statusName = statusName;
    }

    public String statusName() {
        return this.statusName;
    }
}
