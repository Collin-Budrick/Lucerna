package net.lucerna.render.preview;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.OptionalDouble;

public final class ShaderDenoiseOutputRenderTarget implements AutoCloseable {
    public static final GpuFormat FORMAT = GpuFormat.RGBA8_UNORM;
    public static final int BYTES_PER_PIXEL = 4;
    public static final int TEXTURE_USAGE = GpuTexture.USAGE_RENDER_ATTACHMENT
            | GpuTexture.USAGE_TEXTURE_BINDING
            | GpuTexture.USAGE_COPY_SRC;
    private static final int DEPTH_OR_LAYERS = 1;
    private static final int MIP_LEVELS = 1;
    private static final String TEXTURE_LABEL = "lucerna_shader_denoise_output_rgba";
    private static final String USAGE_LABEL = "public Mojang fragment shader-generated denoised diffuse output";
    private static final String OUTPUT_IDENTITY = "lucerna.denoise.diffuse/public-mojang-fragment-color-attachment";
    private static final String OUTPUT_BOUNDARY =
            "fragment color attachment, not compute dispatch or storage-image write";

    private final String textureLabel;
    private final String usageLabel;
    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuSampler sampler;
    private int width;
    private int height;
    private long allocationGeneration;
    private boolean closed;

    public ShaderDenoiseOutputRenderTarget() {
        this(TEXTURE_LABEL, USAGE_LABEL);
    }

    public ShaderDenoiseOutputRenderTarget(String textureLabel, String usageLabel) {
        this.textureLabel = normalizedLabel(textureLabel, TEXTURE_LABEL);
        this.usageLabel = normalizedLabel(usageLabel, USAGE_LABEL);
    }

    public StatusSnapshot ensure(GpuDevice device, int width, int height) {
        if (this.closed) {
            return snapshot(LifecycleStatus.CLOSED, false, false, this.usageLabel + " render target is closed");
        }
        if (width <= 0 || height <= 0) {
            return snapshot(
                    LifecycleStatus.INVALID_SIZE,
                    false,
                    false,
                    this.usageLabel + " render target dimensions must be positive"
            );
        }
        if (requiredBytes(width, height) < 0) {
            return snapshot(
                    LifecycleStatus.INVALID_SIZE,
                    false,
                    false,
                    this.usageLabel + " render target dimensions exceed supported byte count"
            );
        }
        if (device == null) {
            return snapshot(
                    LifecycleStatus.UNAVAILABLE,
                    false,
                    false,
                    "GpuDevice is not available for " + this.usageLabel + " render target allocation"
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
                this.allocationGeneration++;
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
            return snapshot(
                    LifecycleStatus.READY,
                    recreated,
                    !recreated,
                    "public Mojang RGBA8 " + this.usageLabel + " render target "
                            + (recreated ? "allocated" : "reused")
            );
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException | LinkageError exception) {
            closeTextureResource();
            closeSamplerResource();
            return snapshot(
                    LifecycleStatus.FAILED,
                    recreated,
                    false,
                    "public Mojang " + this.usageLabel + " render target allocation failed: "
                            + exceptionSummary(exception)
            );
        }
    }

    public StatusSnapshot forceRecreate(GpuDevice device, int width, int height) {
        if (this.closed) {
            return snapshot(LifecycleStatus.CLOSED, false, false, this.usageLabel + " render target is closed");
        }
        if (width <= 0 || height <= 0) {
            return snapshot(
                    LifecycleStatus.INVALID_SIZE,
                    false,
                    false,
                    this.usageLabel + " render target dimensions must be positive"
            );
        }
        if (requiredBytes(width, height) < 0) {
            return snapshot(
                    LifecycleStatus.INVALID_SIZE,
                    false,
                    false,
                    this.usageLabel + " render target dimensions exceed supported byte count"
            );
        }
        if (device == null) {
            return snapshot(
                    LifecycleStatus.UNAVAILABLE,
                    false,
                    false,
                    "GpuDevice is not available for " + this.usageLabel + " render target recreation"
            );
        }
        closeTextureResource();
        return ensure(device, width, height);
    }

    public StatusSnapshot releaseResources() {
        closeTextureResource();
        closeSamplerResource();
        return snapshot(
                this.closed ? LifecycleStatus.CLOSED : LifecycleStatus.UNAVAILABLE,
                false,
                false,
                "released " + this.usageLabel + " render target resources"
        );
    }

    public StatusSnapshot statusSnapshot() {
        if (this.closed) {
            return snapshot(LifecycleStatus.CLOSED, false, false, this.usageLabel + " render target is closed");
        }
        return snapshot(
                renderTargetReady() && samplingReady() ? LifecycleStatus.READY : LifecycleStatus.UNAVAILABLE,
                false,
                false,
                renderTargetReady()
                        ? this.usageLabel + " render target resources are allocated"
                        : this.usageLabel + " render target resources are not allocated"
        );
    }

    public GpuTexture texture() {
        return this.texture;
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

    public long allocationGeneration() {
        return this.allocationGeneration;
    }

    public boolean renderTargetReady() {
        return textureReady() && this.textureView != null && !this.textureView.isClosed();
    }

    public boolean samplingReady() {
        return renderTargetReady() && this.sampler != null;
    }

    public boolean textureReady() {
        return this.texture != null && !this.texture.isClosed();
    }

    @Override
    public void close() {
        this.closed = true;
        closeTextureResource();
        closeSamplerResource();
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

    private void closeSamplerResource() {
        if (this.sampler != null) {
            this.sampler.close();
            this.sampler = null;
        }
    }

    private StatusSnapshot snapshot(
            LifecycleStatus status,
            boolean textureRecreated,
            boolean textureReused,
            String reason
    ) {
        boolean textureReady = textureReady();
        boolean viewReady = renderTargetReady();
        boolean samplerReady = this.sampler != null;
        return new StatusSnapshot(
                status,
                textureReady,
                viewReady,
                samplerReady,
                textureRecreated,
                textureReused && viewReady,
                this.width,
                this.height,
                FORMAT,
                TEXTURE_USAGE,
                this.allocationGeneration,
                currentIdentityChecksum(),
                this.textureView,
                this.sampler,
                reason
        );
    }

    private long currentIdentityChecksum() {
        long checksum = 1125899906842597L;
        checksum = (31L * checksum) + this.width;
        checksum = (31L * checksum) + this.height;
        checksum = (31L * checksum) + TEXTURE_USAGE;
        checksum = (31L * checksum) + FORMAT.toString().hashCode();
        checksum = (31L * checksum) + this.textureLabel.hashCode();
        checksum = (31L * checksum) + Long.hashCode(this.allocationGeneration);
        checksum = (31L * checksum) + System.identityHashCode(this.texture);
        checksum = (31L * checksum) + System.identityHashCode(this.textureView);
        checksum = (31L * checksum) + System.identityHashCode(this.sampler);
        return checksum;
    }

    private static int requiredBytes(int width, int height) {
        if (width <= 0 || height <= 0) {
            return 0;
        }
        long byteCount = (long) width * (long) height * BYTES_PER_PIXEL;
        return byteCount > Integer.MAX_VALUE ? -1 : (int) byteCount;
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

    public enum LifecycleStatus {
        READY,
        UNAVAILABLE,
        INVALID_SIZE,
        FAILED,
        CLOSED
    }

    public record StatusSnapshot(
            LifecycleStatus status,
            boolean textureReady,
            boolean renderTargetReady,
            boolean samplerReady,
            boolean textureRecreated,
            boolean textureReused,
            int width,
            int height,
            GpuFormat format,
            int usage,
            long allocationGeneration,
            long identityChecksum,
            GpuTextureView textureView,
            GpuSampler sampler,
            String reason
    ) {
        public StatusSnapshot {
            if (status == null) {
                status = LifecycleStatus.FAILED;
            }
            width = Math.max(0, width);
            height = Math.max(0, height);
            if (format == null) {
                format = FORMAT;
            }
            textureReady = textureReady && textureView != null;
            renderTargetReady = renderTargetReady && textureReady;
            samplerReady = samplerReady && sampler != null;
            textureReused = textureReused && renderTargetReady && !textureRecreated;
            if (status == LifecycleStatus.READY && (!renderTargetReady || !samplerReady)) {
                status = LifecycleStatus.FAILED;
            }
            if (reason == null || reason.isBlank()) {
                reason = switch (status) {
                    case READY -> "shader denoise output render target is ready";
                    case UNAVAILABLE -> "shader denoise output render target is unavailable";
                    case INVALID_SIZE -> "shader denoise output render target dimensions are invalid";
                    case FAILED -> "shader denoise output render target allocation failed";
                    case CLOSED -> "shader denoise output render target is closed";
                };
            } else {
                reason = reason.trim();
            }
        }

        public boolean availableForRenderPass() {
            return this.status == LifecycleStatus.READY
                    && this.renderTargetReady
                    && this.textureView != null;
        }

        public boolean availableForSampling() {
            return this.availableForRenderPass()
                    && this.samplerReady
                    && this.sampler != null;
        }

        public String identityKey() {
            return Long.toUnsignedString(this.identityChecksum, 16);
        }

        public String summary() {
            return "status=" + this.status
                    + ",textureReady=" + this.textureReady
                    + ",renderTargetReady=" + this.renderTargetReady
                    + ",samplerReady=" + this.samplerReady
                    + ",availableForRenderPass=" + this.availableForRenderPass()
                    + ",availableForSampling=" + this.availableForSampling()
                    + ",textureRecreated=" + this.textureRecreated
                    + ",textureReused=" + this.textureReused
                    + ",extent=" + this.width + "x" + this.height
                    + ",format=" + this.format
                    + ",usage=" + this.usage
                    + ",allocationGeneration=" + this.allocationGeneration
                    + ",identityKey=" + this.identityKey()
                    + ",view=" + (this.textureView == null ? "none" : "present")
                    + ",sampler=" + (this.sampler == null ? "none" : "present")
                    + ",outputIdentity=\"" + OUTPUT_IDENTITY + "\""
                    + ",outputWriteKind=fragment-color-attachment"
                    + ",computeDispatch=false"
                    + ",storageImageWrite=false"
                    + ",shaderDenoiseOutputBoundary=\"" + OUTPUT_BOUNDARY + "\""
                    + ",reason=" + this.reason;
        }
    }
}
