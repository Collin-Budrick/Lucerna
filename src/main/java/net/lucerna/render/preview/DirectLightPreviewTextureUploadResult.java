package net.lucerna.render.preview;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

public record DirectLightPreviewTextureUploadResult(
        DirectLightPreviewTextureUploadStatus status,
        boolean textureReady,
        boolean uploadSubmitted,
        boolean textureRecreated,
        int width,
        int height,
        int requiredBytes,
        int suppliedBytes,
        GpuFormat format,
        GpuTextureView textureView,
        GpuSampler sampler,
        String reason
) {
    public DirectLightPreviewTextureUploadResult {
        if (status == null) {
            status = DirectLightPreviewTextureUploadStatus.FAILED;
        }
        width = Math.max(0, width);
        height = Math.max(0, height);
        requiredBytes = Math.max(0, requiredBytes);
        suppliedBytes = Math.max(0, suppliedBytes);
        textureReady = textureReady && textureView != null;
        uploadSubmitted = uploadSubmitted && textureReady && status == DirectLightPreviewTextureUploadStatus.UPLOADED;
        if (format == null) {
            format = GpuFormat.RGBA8_UNORM;
        }
        if (reason == null || reason.isBlank()) {
            reason = switch (status) {
                case UPLOADED -> "direct-light preview texture upload completed";
                case UNAVAILABLE -> "direct-light preview texture upload is unavailable";
                case INVALID_INPUT -> "direct-light preview texture upload input is invalid";
                case FAILED -> "direct-light preview texture upload failed";
            };
        } else {
            reason = reason.trim();
        }
    }

    public boolean availableForDraw() {
        return this.textureReady && this.textureView != null && this.sampler != null;
    }

    public String summary() {
        return "status=" + this.status
                + ",textureReady=" + this.textureReady
                + ",uploadSubmitted=" + this.uploadSubmitted
                + ",textureRecreated=" + this.textureRecreated
                + ",extent=" + this.width + "x" + this.height
                + ",format=" + this.format
                + ",requiredBytes=" + this.requiredBytes
                + ",suppliedBytes=" + this.suppliedBytes
                + ",view=" + (this.textureView == null ? "none" : "present")
                + ",sampler=" + (this.sampler == null ? "none" : "present")
                + ",reason=" + this.reason;
    }

    public static DirectLightPreviewTextureUploadResult unavailable(String reason) {
        return new DirectLightPreviewTextureUploadResult(
                DirectLightPreviewTextureUploadStatus.UNAVAILABLE,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                GpuFormat.RGBA8_UNORM,
                null,
                null,
                reason
        );
    }
}
