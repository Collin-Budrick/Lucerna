package net.lucerna.render.culling;

public record ClusterBounds(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {
    public ClusterBounds {
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            throw new IllegalArgumentException("cluster bounds max values must be greater than or equal to min values");
        }
    }

    public double centerX() {
        return (this.minX + this.maxX) * 0.5D;
    }

    public double centerY() {
        return (this.minY + this.maxY) * 0.5D;
    }

    public double centerZ() {
        return (this.minZ + this.maxZ) * 0.5D;
    }

    public double extentX() {
        return this.maxX - this.minX;
    }

    public double extentY() {
        return this.maxY - this.minY;
    }

    public double extentZ() {
        return this.maxZ - this.minZ;
    }
}
