package net.lucerna.render.pass;

public record LucernaJavaOpaqueRenderObjects(
        boolean renderPassPresent,
        boolean commandEncoderPresent,
        boolean colorTargetPresent,
        boolean depthTargetPresent,
        String description
) {
    private static final String UNKNOWN_LABEL = "unknown";

    public LucernaJavaOpaqueRenderObjects {
        if (description == null || description.isBlank()) {
            description = present()
                    ? "Java opaque render objects are present without native handles."
                    : UNKNOWN_LABEL;
        } else {
            description = description.trim();
        }
    }

    public static LucernaJavaOpaqueRenderObjects none() {
        return new LucernaJavaOpaqueRenderObjects(
                false,
                false,
                false,
                false,
                "No Java opaque render objects supplied."
        );
    }

    public static LucernaJavaOpaqueRenderObjects of(
            Object renderPass,
            Object commandEncoder,
            Object colorTarget,
            Object depthTarget,
            String description
    ) {
        return new LucernaJavaOpaqueRenderObjects(
                renderPass != null,
                commandEncoder != null,
                colorTarget != null,
                depthTarget != null,
                description
        );
    }

    public boolean present() {
        return this.renderPassPresent
                || this.commandEncoderPresent
                || this.colorTargetPresent
                || this.depthTargetPresent;
    }

    public boolean colorTargetOnly() {
        return this.colorTargetPresent
                && !this.renderPassPresent
                && !this.commandEncoderPresent
                && !this.depthTargetPresent;
    }

    public String statusLabel() {
        return "javaOpaque=" + present()
                + ", renderPass=" + this.renderPassPresent
                + ", commandEncoder=" + this.commandEncoderPresent
                + ", colorTarget=" + this.colorTargetPresent
                + ", depthTarget=" + this.depthTargetPresent;
    }
}
