package net.lucerna.render.gbuffer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record GBufferTargetContract(
        String passId,
        int numericPassId,
        List<GBufferAttachmentContract> attachments
) {
    public static final String MAIN_PASS_ID = "lucerna.gbuffer.main";
    public static final int MAIN_NUMERIC_PASS_ID = 100;

    public static final String DEPTH = "lucerna.gbuffer.depth";
    public static final String NORMAL_ROUGHNESS = "lucerna.gbuffer.normalRoughness";
    public static final String ALBEDO_OPACITY = "lucerna.gbuffer.albedoOpacity";
    public static final String MATERIAL_ID = "lucerna.gbuffer.materialId";
    public static final String EMISSIVE = "lucerna.gbuffer.emissive";
    public static final String MOTION_HISTORY = "lucerna.gbuffer.motionHistory";

    public GBufferTargetContract {
        Objects.requireNonNull(passId, "passId");
        if (passId.isBlank()) {
            throw new IllegalArgumentException("passId must not be blank");
        }
        if (numericPassId <= 0) {
            throw new IllegalArgumentException("numericPassId must be positive");
        }
        Objects.requireNonNull(attachments, "attachments");
        attachments = List.copyOf(attachments);
        if (attachments.isEmpty()) {
            throw new IllegalArgumentException("attachments must not be empty");
        }
        Set<String> attachmentNames = new HashSet<>();
        for (GBufferAttachmentContract attachment : attachments) {
            Objects.requireNonNull(attachment, "attachments must not contain null entries");
            if (!passId.equals(attachment.ownerPass())) {
                throw new IllegalArgumentException("attachment ownerPass must match target passId");
            }
            if (!attachmentNames.add(attachment.name())) {
                throw new IllegalArgumentException("attachment names must be unique");
            }
        }
    }

    public static GBufferTargetContract lucernaMain() {
        return new GBufferTargetContract(
                MAIN_PASS_ID,
                MAIN_NUMERIC_PASS_ID,
                List.of(
                        new GBufferAttachmentContract(
                                DEPTH,
                                MAIN_PASS_ID,
                                "D32_SFLOAT",
                                "full",
                                1,
                                List.of("depth_stencil_attachment", "sampled"),
                                List.of(
                                        "lucerna.lighting.direct",
                                        "lucerna.lighting.gi",
                                        "lucerna.denoise.diffuse",
                                        "lucerna.debug.overlay"
                                )
                        ),
                        new GBufferAttachmentContract(
                                NORMAL_ROUGHNESS,
                                MAIN_PASS_ID,
                                "R16G16B16A16_SFLOAT",
                                "full",
                                1,
                                List.of("color_attachment", "sampled"),
                                List.of(
                                        "lucerna.lighting.direct",
                                        "lucerna.lighting.gi",
                                        "lucerna.denoise.diffuse",
                                        "lucerna.debug.overlay"
                                )
                        ),
                        new GBufferAttachmentContract(
                                ALBEDO_OPACITY,
                                MAIN_PASS_ID,
                                "R8G8B8A8_UNORM",
                                "full",
                                1,
                                List.of("color_attachment", "sampled"),
                                List.of(
                                        "lucerna.lighting.direct",
                                        "lucerna.composite.final",
                                        "lucerna.debug.overlay"
                                )
                        ),
                        new GBufferAttachmentContract(
                                MATERIAL_ID,
                                MAIN_PASS_ID,
                                "R32_UINT",
                                "full",
                                1,
                                List.of("color_attachment", "sampled", "transfer_src"),
                                List.of(
                                        "lucerna.lighting.direct",
                                        "lucerna.lighting.gi",
                                        "lucerna.debug.overlay"
                                )
                        ),
                        new GBufferAttachmentContract(
                                EMISSIVE,
                                MAIN_PASS_ID,
                                "R16G16B16A16_SFLOAT",
                                "full",
                                1,
                                List.of("color_attachment", "sampled"),
                                List.of(
                                        "lucerna.lighting.direct",
                                        "lucerna.lighting.gi",
                                        "lucerna.debug.overlay"
                                )
                        ),
                        new GBufferAttachmentContract(
                                MOTION_HISTORY,
                                MAIN_PASS_ID,
                                "R16G16B16A16_SFLOAT",
                                "full",
                                1,
                                List.of("color_attachment", "sampled"),
                                List.of(
                                        "lucerna.denoise.diffuse",
                                        "lucerna.debug.overlay"
                                )
                        )
                )
        );
    }

    public Optional<GBufferAttachmentContract> attachment(String name) {
        Objects.requireNonNull(name, "name");
        return this.attachments.stream()
                .filter(attachment -> attachment.isNamed(name))
                .findFirst();
    }

    public List<String> attachmentNames() {
        return this.attachments.stream()
                .map(GBufferAttachmentContract::name)
                .toList();
    }
}
