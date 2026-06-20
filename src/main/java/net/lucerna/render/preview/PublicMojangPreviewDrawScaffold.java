package net.lucerna.render.preview;

import com.mojang.blaze3d.pipeline.RenderPipeline;

public record PublicMojangPreviewDrawScaffold(
        boolean available,
        boolean drawPrepared,
        boolean drawCallsIssued,
        String pipelineLocation,
        String requiredTextureBinding,
        String drawMode,
        int firstVertex,
        int vertexCount,
        int instanceCount,
        int firstInstance,
        String reason
) {
    public PublicMojangPreviewDrawScaffold {
        if (pipelineLocation == null || pipelineLocation.isBlank()) {
            pipelineLocation = "unknown";
        } else {
            pipelineLocation = pipelineLocation.trim();
        }
        if (requiredTextureBinding == null || requiredTextureBinding.isBlank()) {
            requiredTextureBinding = "none";
        } else {
            requiredTextureBinding = requiredTextureBinding.trim();
        }
        if (drawMode == null || drawMode.isBlank()) {
            drawMode = "unknown";
        } else {
            drawMode = drawMode.trim();
        }
        firstVertex = Math.max(0, firstVertex);
        vertexCount = Math.max(0, vertexCount);
        instanceCount = Math.max(0, instanceCount);
        firstInstance = Math.max(0, firstInstance);
        drawPrepared = drawPrepared && available;
        drawCallsIssued = drawCallsIssued && drawPrepared;
        if (reason == null || reason.isBlank()) {
            reason = available
                    ? "public Mojang preview draw scaffold is available"
                    : "public Mojang preview draw scaffold is unavailable";
        } else {
            reason = reason.trim();
        }
    }

    public static PublicMojangPreviewDrawScaffold unavailable(String reason) {
        return new PublicMojangPreviewDrawScaffold(
                false,
                false,
                false,
                "unknown",
                "none",
                "none",
                0,
                0,
                0,
                0,
                reason
        );
    }

    public static PublicMojangPreviewDrawScaffold prepared(
            RenderPipeline pipeline,
            String requiredTextureBinding,
            String drawMode,
            int firstVertex,
            int vertexCount,
            int instanceCount,
            int firstInstance,
            String reason
    ) {
        String pipelineLocation = pipeline == null || pipeline.getLocation() == null
                ? "unknown"
                : pipeline.getLocation().toString();
        return new PublicMojangPreviewDrawScaffold(
                pipeline != null,
                pipeline != null,
                false,
                pipelineLocation,
                requiredTextureBinding,
                drawMode,
                firstVertex,
                vertexCount,
                instanceCount,
                firstInstance,
                reason
        );
    }

    public static PublicMojangPreviewDrawScaffold issued(
            RenderPipeline pipeline,
            String requiredTextureBinding,
            String drawMode,
            int firstVertex,
            int vertexCount,
            int instanceCount,
            int firstInstance,
            String reason
    ) {
        String pipelineLocation = pipeline == null || pipeline.getLocation() == null
                ? "unknown"
                : pipeline.getLocation().toString();
        return new PublicMojangPreviewDrawScaffold(
                pipeline != null,
                pipeline != null,
                pipeline != null,
                pipelineLocation,
                requiredTextureBinding,
                drawMode,
                firstVertex,
                vertexCount,
                instanceCount,
                firstInstance,
                reason
        );
    }

    public String summary() {
        return "available=" + this.available
                + ",prepared=" + this.drawPrepared
                + ",drawCallsIssued=" + this.drawCallsIssued
                + ",pipeline=" + this.pipelineLocation
                + ",textureBinding=" + this.requiredTextureBinding
                + ",mode=" + this.drawMode
                + ",draw=RenderPass.draw(vertexCount="
                + this.vertexCount
                + ",instanceCount="
                + this.instanceCount
                + ",firstVertex="
                + this.firstVertex
                + ",firstInstance="
                + this.firstInstance
                + "),reason="
                + this.reason;
    }
}
