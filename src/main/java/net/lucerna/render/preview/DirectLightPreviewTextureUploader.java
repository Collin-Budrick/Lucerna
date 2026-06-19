package net.lucerna.render.preview;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;

public final class DirectLightPreviewTextureUploader implements AutoCloseable {
    public static final GpuFormat FORMAT = GpuFormat.RGBA8_UNORM;
    public static final int BYTES_PER_PIXEL = 4;
    private static final int MIP_LEVEL = 0;
    private static final int ARRAY_LAYER = 0;
    private static final int ORIGIN_X = 0;
    private static final int ORIGIN_Y = 0;
    private static final int DEPTH_OR_LAYERS = 1;
    private static final int MIP_LEVELS = 1;
    private static final int TEXTURE_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;
    private static final String TEXTURE_LABEL = "lucerna_direct_light_preview_rgba";
    private static final String UPLOAD_USAGE_LABEL = "direct-light preview";

    private final String textureLabel;
    private final String uploadUsageLabel;
    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuSampler sampler;
    private int width;
    private int height;
    private boolean closed;

    public DirectLightPreviewTextureUploader() {
        this(TEXTURE_LABEL, UPLOAD_USAGE_LABEL);
    }

    public DirectLightPreviewTextureUploader(String textureLabel, String uploadUsageLabel) {
        this.textureLabel = normalizedLabel(textureLabel, TEXTURE_LABEL);
        this.uploadUsageLabel = normalizedLabel(uploadUsageLabel, UPLOAD_USAGE_LABEL);
    }

    public DirectLightPreviewTextureUploadResult upload(
            GpuDevice device,
            CommandEncoder commandEncoder,
            ByteBuffer rgbaPixels,
            int width,
            int height
    ) {
        if (this.closed) {
            return unavailable(width, height, this.uploadUsageLabel + " texture uploader is closed");
        }
        if (device == null) {
            return unavailable(width, height, "GpuDevice is not available for " + this.uploadUsageLabel + " texture upload");
        }
        if (commandEncoder == null) {
            return unavailable(width, height, "CommandEncoder is not available for " + this.uploadUsageLabel + " texture upload");
        }
        if (rgbaPixels == null) {
            return invalid(width, height, 0, "RGBA source buffer is not available");
        }
        int requiredBytes = requiredBytes(width, height);
        if (requiredBytes == 0) {
            return invalid(width, height, rgbaPixels.remaining(), "RGBA preview dimensions must be positive");
        }
        if (requiredBytes < 0) {
            return invalid(width, height, rgbaPixels.remaining(), "RGBA preview dimensions exceed supported byte count");
        }
        if (rgbaPixels.remaining() < requiredBytes) {
            return invalid(
                    width,
                    height,
                    rgbaPixels.remaining(),
                    "RGBA source buffer has fewer bytes than width * height * 4"
            );
        }

        boolean recreated = false;
        try {
            if (requiresTextureRecreate(width, height)) {
                closeTextureResource();
                this.texture = device.createTexture(
                        this.textureLabel,
                        TEXTURE_USAGE,
                        FORMAT,
                        width,
                        height,
                        DEPTH_OR_LAYERS,
                        MIP_LEVELS
                );
                this.textureView = device.createTextureView(this.texture);
                this.width = width;
                this.height = height;
                recreated = true;
            }
            if (this.sampler == null) {
                this.sampler = device.createSampler(
                        AddressMode.CLAMP_TO_EDGE,
                        AddressMode.CLAMP_TO_EDGE,
                        FilterMode.NEAREST,
                        FilterMode.NEAREST,
                        1,
                        OptionalDouble.empty()
                );
            }

            ByteBuffer uploadBuffer = rgbaPixels.duplicate();
            uploadBuffer.limit(uploadBuffer.position() + requiredBytes);
            commandEncoder.writeToTexture(
                    this.texture,
                    uploadBuffer,
                    MIP_LEVEL,
                    ARRAY_LAYER,
                    ORIGIN_X,
                    ORIGIN_Y,
                    width,
                    height
            );
            return result(
                    DirectLightPreviewTextureUploadStatus.UPLOADED,
                    true,
                    true,
                    recreated,
                    width,
                    height,
                    requiredBytes,
                    rgbaPixels.remaining(),
                    this.textureView,
                    this.sampler,
                    "RGBA " + this.uploadUsageLabel + " image uploaded through public Mojang GpuDevice/CommandEncoder APIs"
            );
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException | LinkageError exception) {
            return result(
                    DirectLightPreviewTextureUploadStatus.FAILED,
                    this.textureView != null,
                    false,
                    recreated,
                    width,
                    height,
                    requiredBytes,
                    rgbaPixels.remaining(),
                    this.textureView,
                    this.sampler,
                    "public Mojang " + this.uploadUsageLabel + " texture upload failed: " + exceptionSummary(exception)
            );
        }
    }

    public GpuTextureView textureView() {
        return this.textureView;
    }

    public GpuSampler sampler() {
        return this.sampler;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean textureReady() {
        return this.textureView != null && this.texture != null && !this.texture.isClosed();
    }

    @Override
    public void close() {
        this.closed = true;
        closeTextureResource();
        if (this.sampler != null) {
            this.sampler.close();
            this.sampler = null;
        }
    }

    private boolean requiresTextureRecreate(int width, int height) {
        return this.texture == null
                || this.texture.isClosed()
                || this.textureView == null
                || this.textureView.isClosed()
                || this.width != width
                || this.height != height;
    }

    private void closeTextureResource() {
        if (this.textureView != null) {
            this.textureView.close();
            this.textureView = null;
        }
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
        this.width = 0;
        this.height = 0;
    }

    private static int requiredBytes(int width, int height) {
        if (width <= 0 || height <= 0) {
            return 0;
        }
        long byteCount = (long) width * (long) height * BYTES_PER_PIXEL;
        return byteCount > Integer.MAX_VALUE ? -1 : (int) byteCount;
    }

    private static DirectLightPreviewTextureUploadResult unavailable(int width, int height, String reason) {
        return result(
                DirectLightPreviewTextureUploadStatus.UNAVAILABLE,
                false,
                false,
                false,
                Math.max(0, width),
                Math.max(0, height),
                Math.max(0, requiredBytes(width, height)),
                0,
                null,
                null,
                reason
        );
    }

    private static DirectLightPreviewTextureUploadResult invalid(
            int width,
            int height,
            int suppliedBytes,
            String reason
    ) {
        return result(
                DirectLightPreviewTextureUploadStatus.INVALID_INPUT,
                false,
                false,
                false,
                Math.max(0, width),
                Math.max(0, height),
                Math.max(0, requiredBytes(width, height)),
                suppliedBytes,
                null,
                null,
                reason
        );
    }

    private static DirectLightPreviewTextureUploadResult result(
            DirectLightPreviewTextureUploadStatus status,
            boolean textureReady,
            boolean uploadSubmitted,
            boolean textureRecreated,
            int width,
            int height,
            int requiredBytes,
            int suppliedBytes,
            GpuTextureView textureView,
            GpuSampler sampler,
            String reason
    ) {
        return new DirectLightPreviewTextureUploadResult(
                status,
                textureReady,
                uploadSubmitted,
                textureRecreated,
                width,
                height,
                requiredBytes,
                suppliedBytes,
                FORMAT,
                textureView,
                sampler,
                reason
        );
    }

    private static String exceptionSummary(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message.trim();
    }

    private static String normalizedLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
