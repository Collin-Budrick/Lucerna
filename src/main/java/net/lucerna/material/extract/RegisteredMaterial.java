package net.lucerna.material.extract;

import net.lucerna.material.LucernaMaterial;

import java.util.Objects;

public record RegisteredMaterial(
        ExtractedMaterial extracted,
        LucernaMaterial material,
        MaterialUploadMetadata uploadMetadata
) {
    public RegisteredMaterial {
        Objects.requireNonNull(extracted, "extracted");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(uploadMetadata, "uploadMetadata");
    }

    public static RegisteredMaterial from(ExtractedMaterial extracted, LucernaMaterial material) {
        return new RegisteredMaterial(extracted, material, extracted.toUploadMetadata(material));
    }
}
