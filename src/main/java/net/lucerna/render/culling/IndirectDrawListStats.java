package net.lucerna.render.culling;

public record IndirectDrawListStats(
        int drawCommandCount,
        int visibleClusterCount,
        int primitiveCount,
        int estimatedCommandBytes,
        long generation,
        boolean metadataOnly
) {
    private static final int VULKAN_INDEXED_INDIRECT_COMMAND_BYTES = 20;

    public IndirectDrawListStats {
        if (drawCommandCount < 0 || visibleClusterCount < 0 || primitiveCount < 0 || estimatedCommandBytes < 0) {
            throw new IllegalArgumentException("indirect draw counters must be non-negative");
        }
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public static IndirectDrawListStats metadataOnly(int visibleClusterCount, int primitiveCount, long generation) {
        int commandBytes = Math.multiplyExact(visibleClusterCount, VULKAN_INDEXED_INDIRECT_COMMAND_BYTES);
        return new IndirectDrawListStats(
                visibleClusterCount,
                visibleClusterCount,
                primitiveCount,
                commandBytes,
                generation,
                true
        );
    }

    public String compactLabel() {
        return "draws=" + this.drawCommandCount
                + " visibleClusters=" + this.visibleClusterCount
                + " primitives=" + this.primitiveCount
                + " bytes=" + this.estimatedCommandBytes
                + " gen=" + this.generation
                + " metadataOnly=" + this.metadataOnly;
    }
}
