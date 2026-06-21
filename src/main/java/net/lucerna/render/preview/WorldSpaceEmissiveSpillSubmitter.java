package net.lucerna.render.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import net.lucerna.Lucerna;
import net.lucerna.LucernaController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorldSpaceEmissiveSpillSubmitter {
    private static final int SOURCE_SCAN_RADIUS = 10;
    private static final int SOURCE_SCAN_Y_RADIUS = 7;
    private static final int RECEIVER_SCAN_RADIUS = 4;
    private static final int MAX_SOURCE_BLOCKS = 10;
    private static final int MAX_RECEIVER_QUADS = 72;
    private static final int MAX_SECONDARY_BOUNCE_QUADS = 42;
    private static final int SECONDARY_BOUNCE_SCAN_RADIUS = 2;
    private static final double MAX_RECEIVER_DISTANCE = 4.75;
    private static final double MAX_SECONDARY_BOUNCE_DISTANCE = 3.20;
    private static final double FACE_EPSILON = 0.006;
    private static final double FACE_INSET = 0.055;
    private static boolean loggedSubmission;

    private WorldSpaceEmissiveSpillSubmitter() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState) {
        LucernaController controller = LucernaController.getInstance();
        if (!controller.isRendererActive() || !controller.getConfig().compositeMode().directLightingEnabled()) {
            return;
        }
        if (poseStack == null || collector == null || levelRenderState == null || levelRenderState.cameraRenderState == null) {
            return;
        }

        CameraRenderState camera = levelRenderState.cameraRenderState;
        if (camera.pos == null || !camera.initialized) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        SpillStats stats = submitNearbySpill(level, camera, primitives);
        if (stats.receiverQuadCount() + stats.secondaryBounceQuadCount() <= 0) {
            return;
        }

        primitives.submit(collector, camera, false);
        if (!loggedSubmission) {
            loggedSubmission = true;
            Lucerna.LOGGER.info(
                    "Lucerna world-space emissive spill submitted: worldSpaceEmissiveSpill=true cleanGameplayComposite=true "
                            + "experimentalVisualStack={} proofMarker=false screenSpaceBlobComposite=false sourceCount={} "
                            + "receiverQuadCount={} secondaryColoredBounce=true secondaryBounceQuadCount={} coloredPanelReceiverCount={} "
                            + "emissiveSourceBlocks=\"{}\" blockMaterialSceneData=true faceNormalData=true "
                            + "sourceColorMaterialData=true sourceReceiverDistanceFalloff=true depthAwareSubmitNode=true "
                            + "worldSpaceBlockFaceQuads=true materialTintBounce=true sourcePanelReceiverBounce=true "
                            + "realDepthTexture=false realPathTracedGi=false boundary=world-space-emissive-plus-material-colored-secondary-bounce-preview.",
                    ProofVisualMode.experimentalVisualStackAllowed(),
                    stats.sourceCount(),
                    stats.receiverQuadCount(),
                    stats.secondaryBounceQuadCount(),
                    stats.coloredPanelReceiverCount(),
                    stats.sourceIds()
            );
        }
    }

    private static SpillStats submitNearbySpill(ClientLevel level, CameraRenderState camera, DrawableGizmoPrimitives primitives) {
        BlockPos cameraBlock = BlockPos.containing(camera.pos.x, camera.pos.y, camera.pos.z);
        int minY = Math.max(level.getMinY(), cameraBlock.getY() - SOURCE_SCAN_Y_RADIUS);
        int maxY = Math.min(level.getMaxY() - 1, cameraBlock.getY() + SOURCE_SCAN_Y_RADIUS);
        List<SourceCandidate> sources = new ArrayList<>(MAX_SOURCE_BLOCKS);
        Map<String, Integer> sourceCountsByBlock = new HashMap<>();
        StringBuilder sourceIds = new StringBuilder();

        sourceSearch:
        for (int y = minY; y <= maxY; y++) {
            for (int z = cameraBlock.getZ() - SOURCE_SCAN_RADIUS; z <= cameraBlock.getZ() + SOURCE_SCAN_RADIUS; z++) {
                for (int x = cameraBlock.getX() - SOURCE_SCAN_RADIUS; x <= cameraBlock.getX() + SOURCE_SCAN_RADIUS; x++) {
                    BlockPos sourcePos = new BlockPos(x, y, z);
                    BlockState sourceState = level.getBlockState(sourcePos);
                    SourceLight sourceLight = sourceLight(sourceState);
                    if (sourceLight == null) {
                        continue;
                    }
                    Vec3 sourceCenter = Vec3.atCenterOf(sourcePos);
                    if (camera.pos.distanceToSqr(sourceCenter) > (SOURCE_SCAN_RADIUS + 2.0) * (SOURCE_SCAN_RADIUS + 2.0)) {
                        continue;
                    }

                    String sourceBlockId = sourceLight.blockId();
                    int sameBlockCount = sourceCountsByBlock.getOrDefault(sourceBlockId, 0);
                    if (sameBlockCount >= 2) {
                        continue;
                    }
                    sourceCountsByBlock.put(sourceBlockId, sameBlockCount + 1);
                    sources.add(new SourceCandidate(sourcePos, sourceCenter, sourceLight));
                    appendSourceId(sourceIds, sourceBlockId);
                    if (sources.size() >= MAX_SOURCE_BLOCKS) {
                        break sourceSearch;
                    }
                }
            }
        }

        int receiverQuadCount = 0;
        int secondaryBounceQuadCount = 0;
        int coloredPanelReceiverCount = 0;
        int perSourceBudget = sources.isEmpty()
                ? 0
                : Math.max(8, MAX_RECEIVER_QUADS / sources.size());
        int perSourceSecondaryBudget = sources.isEmpty()
                ? 0
                : Math.max(5, MAX_SECONDARY_BOUNCE_QUADS / sources.size());
        for (SourceCandidate source : sources) {
            int remainingBudget = MAX_RECEIVER_QUADS - receiverQuadCount;
            if (remainingBudget <= 0) {
                break;
            }
            int remainingSecondaryBudget = MAX_SECONDARY_BOUNCE_QUADS - secondaryBounceQuadCount;
            SourceSpillStats sourceStats = submitReceiversForSource(
                    level,
                    primitives,
                    camera.pos,
                    source.position(),
                    source.center(),
                    source.light(),
                    Math.min(perSourceBudget, remainingBudget),
                    Math.min(perSourceSecondaryBudget, Math.max(0, remainingSecondaryBudget))
            );
            receiverQuadCount += sourceStats.receiverQuadCount();
            secondaryBounceQuadCount += sourceStats.secondaryBounceQuadCount();
            coloredPanelReceiverCount += sourceStats.coloredPanelReceiverCount();
        }

        return new SpillStats(
                sources.size(),
                Math.min(receiverQuadCount, MAX_RECEIVER_QUADS),
                Math.min(secondaryBounceQuadCount, MAX_SECONDARY_BOUNCE_QUADS),
                coloredPanelReceiverCount,
                sourceIds.toString()
        );
    }

    private static SourceSpillStats submitReceiversForSource(
            ClientLevel level,
            DrawableGizmoPrimitives primitives,
            Vec3 cameraPosition,
            BlockPos sourcePos,
            Vec3 sourceCenter,
            SourceLight sourceLight,
            int maxSubmitted,
            int maxSecondarySubmitted
    ) {
        int submitted = 0;
        int secondarySubmitted = 0;
        int coloredPanelReceivers = 0;
        if (maxSubmitted <= 0) {
            return new SourceSpillStats(0, 0, 0);
        }
        int radius = RECEIVER_SCAN_RADIUS;
        int minY = Math.max(level.getMinY(), sourcePos.getY() - radius);
        int maxY = Math.min(level.getMaxY() - 1, sourcePos.getY() + radius);

        receiverSearch:
        for (int y = minY; y <= maxY; y++) {
            for (int z = sourcePos.getZ() - radius; z <= sourcePos.getZ() + radius; z++) {
                for (int x = sourcePos.getX() - radius; x <= sourcePos.getX() + radius; x++) {
                    BlockPos receiverPos = new BlockPos(x, y, z);
                    if (receiverPos.equals(sourcePos)) {
                        continue;
                    }
                    BlockState receiverState = level.getBlockState(receiverPos);
                    if (!isReceiverSurface(receiverState)) {
                        continue;
                    }

                    Vec3 receiverCenter = Vec3.atCenterOf(receiverPos);
                    double distance = receiverCenter.distanceTo(sourceCenter);
                    if (distance <= 0.75 || distance > MAX_RECEIVER_DISTANCE) {
                        continue;
                    }

                    FaceHit faceHit = bestReceiverFace(level, receiverPos, receiverCenter, sourceCenter, sourcePos, cameraPosition);
                    if (faceHit == null) {
                        continue;
                    }
                    double falloff = Math.max(0.0, 1.0 - (distance / MAX_RECEIVER_DISTANCE));
                    double strength = sourceLight.intensity() * faceHit.weight() * Math.sqrt(falloff);
                    if (strength < 0.135) {
                        continue;
                    }

                    double faceInset = Math.max(0.12, Math.min(0.34, 0.34 - strength * 0.18));
                    primitives.addQuad(
                            faceCorner(receiverPos, faceHit.face(), 0, faceInset),
                            faceCorner(receiverPos, faceHit.face(), 1, faceInset),
                            faceCorner(receiverPos, faceHit.face(), 2, faceInset),
                            faceCorner(receiverPos, faceHit.face(), 3, faceInset),
                            sourceLight.argb(strength)
                    );
                    MaterialTint materialTint = materialTint(receiverState);
                    if (materialTint != null) {
                        coloredPanelReceivers++;
                        if (secondarySubmitted < maxSecondarySubmitted) {
                            secondarySubmitted += submitSecondaryBounceReceivers(
                                    level,
                                    primitives,
                                    cameraPosition,
                                    sourcePos,
                                    receiverPos,
                                    receiverCenter,
                                    materialTint.bounceLight(sourceLight, strength),
                                    maxSecondarySubmitted - secondarySubmitted
                            );
                        }
                    }
                    submitted++;
                    if (submitted >= maxSubmitted) {
                        break receiverSearch;
                    }
                }
            }
        }
        return new SourceSpillStats(submitted, secondarySubmitted, coloredPanelReceivers);
    }

    private static int submitSecondaryBounceReceivers(
            ClientLevel level,
            DrawableGizmoPrimitives primitives,
            Vec3 cameraPosition,
            BlockPos primarySourcePos,
            BlockPos bounceSourcePos,
            Vec3 bounceSourceCenter,
            SourceLight bounceLight,
            int maxSubmitted
    ) {
        if (maxSubmitted <= 0 || bounceLight == null || bounceLight.intensity() <= 0.030) {
            return 0;
        }
        int submitted = 0;
        int radius = SECONDARY_BOUNCE_SCAN_RADIUS;
        int minY = Math.max(level.getMinY(), bounceSourcePos.getY() - radius);
        int maxY = Math.min(level.getMaxY() - 1, bounceSourcePos.getY() + radius);

        secondarySearch:
        for (int y = minY; y <= maxY; y++) {
            for (int z = bounceSourcePos.getZ() - radius; z <= bounceSourcePos.getZ() + radius; z++) {
                for (int x = bounceSourcePos.getX() - radius; x <= bounceSourcePos.getX() + radius; x++) {
                    BlockPos receiverPos = new BlockPos(x, y, z);
                    if (receiverPos.equals(primarySourcePos) || receiverPos.equals(bounceSourcePos)) {
                        continue;
                    }
                    BlockState receiverState = level.getBlockState(receiverPos);
                    if (!isReceiverSurface(receiverState)) {
                        continue;
                    }

                    Vec3 receiverCenter = Vec3.atCenterOf(receiverPos);
                    double distance = receiverCenter.distanceTo(bounceSourceCenter);
                    if (distance <= 0.80 || distance > MAX_SECONDARY_BOUNCE_DISTANCE) {
                        continue;
                    }

                    FaceHit faceHit = bestReceiverFace(
                            level,
                            receiverPos,
                            receiverCenter,
                            bounceSourceCenter,
                            bounceSourcePos,
                            cameraPosition
                    );
                    if (faceHit == null) {
                        continue;
                    }

                    double falloff = Math.max(0.0, 1.0 - (distance / MAX_SECONDARY_BOUNCE_DISTANCE));
                    double strength = bounceLight.intensity() * faceHit.weight() * Math.sqrt(falloff);
                    if (strength < 0.030) {
                        continue;
                    }

                    double faceInset = Math.max(0.14, Math.min(0.38, 0.36 - strength * 0.28));
                    primitives.addQuad(
                            faceCorner(receiverPos, faceHit.face(), 0, faceInset),
                            faceCorner(receiverPos, faceHit.face(), 1, faceInset),
                            faceCorner(receiverPos, faceHit.face(), 2, faceInset),
                            faceCorner(receiverPos, faceHit.face(), 3, faceInset),
                            bounceLight.argb(strength)
                    );
                    submitted++;
                    if (submitted >= maxSubmitted) {
                        break secondarySearch;
                    }
                }
            }
        }
        return submitted;
    }

    private static boolean isReceiverSurface(BlockState state) {
        if (state == null || state.isAir() || state.getLightEmission() > 0) {
            return false;
        }
        if (!state.getFluidState().isEmpty() || state.liquid()) {
            return false;
        }
        return state.canOcclude() && state.isSolidRender();
    }

    private static MaterialTint materialTint(BlockState state) {
        if (state == null || state.isAir()) {
            return null;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        if (!(blockId.contains("concrete")
                || blockId.contains("wool")
                || blockId.contains("terracotta")
                || blockId.contains("glazed_terracotta"))) {
            return null;
        }
        if (blockId.contains("red_")) {
            return new MaterialTint(blockId, 232, 58, 46, 0.62);
        }
        if (blockId.contains("blue_")) {
            return new MaterialTint(blockId, 58, 96, 235, 0.58);
        }
        if (blockId.contains("lime_")) {
            return new MaterialTint(blockId, 122, 220, 58, 0.64);
        }
        if (blockId.contains("green_")) {
            return new MaterialTint(blockId, 60, 160, 68, 0.56);
        }
        if (blockId.contains("orange_")) {
            return new MaterialTint(blockId, 245, 132, 36, 0.60);
        }
        if (blockId.contains("yellow_")) {
            return new MaterialTint(blockId, 250, 218, 62, 0.58);
        }
        if (blockId.contains("cyan_")) {
            return new MaterialTint(blockId, 58, 184, 214, 0.56);
        }
        if (blockId.contains("purple_") || blockId.contains("magenta_")) {
            return new MaterialTint(blockId, 172, 78, 224, 0.54);
        }
        return null;
    }

    private static FaceHit bestReceiverFace(
            ClientLevel level,
            BlockPos receiverPos,
            Vec3 receiverCenter,
            Vec3 sourceCenter,
            BlockPos sourcePos,
            Vec3 cameraPosition
    ) {
        Vec3 toSource = sourceCenter.subtract(receiverCenter);
        double distance = toSource.length();
        if (distance <= 0.0001) {
            return null;
        }
        Vec3 toSourceUnit = toSource.scale(1.0 / distance);
        Vec3 toCamera = cameraPosition.subtract(receiverCenter);
        Vec3 toCameraUnit = toCamera.lengthSqr() <= 0.0001
                ? new Vec3(0.0, 0.0, 0.0)
                : toCamera.normalize();
        FaceHit best = null;
        for (Direction face : Direction.values()) {
            if (!faceCanReceiveLight(level, receiverPos, face, sourcePos)) {
                continue;
            }
            Vec3 normal = faceNormal(face);
            double cameraVisibility = normal.dot(toCameraUnit);
            if (cameraVisibility < -0.10) {
                continue;
            }

            double normalDistance = normal.dot(toSource);
            double normalWeight = Math.max(0.0, normal.dot(toSourceUnit));
            double tangentDistanceSq = Math.max(0.0, distance * distance - normalDistance * normalDistance);
            double tangentReach = Math.max(0.0, 1.0 - Math.sqrt(tangentDistanceSq) / MAX_RECEIVER_DISTANCE);
            double tangentPlane = Math.max(0.0, 1.0 - Math.abs(normalDistance) / 1.35);
            double tangentWeight = tangentReach * tangentPlane * (0.38 + Math.max(0.0, cameraVisibility) * 0.22);
            double weight = Math.max(normalWeight, tangentWeight);
            if (weight < 0.12) {
                continue;
            }
            if (best == null || weight > best.weight()) {
                best = new FaceHit(face, weight);
            }
        }
        return best;
    }

    private static boolean faceCanReceiveLight(ClientLevel level, BlockPos receiverPos, Direction face, BlockPos sourcePos) {
        BlockPos adjacent = receiverPos.relative(face);
        if (adjacent.equals(sourcePos)) {
            return true;
        }
        BlockState adjacentState = level.getBlockState(adjacent);
        return adjacentState.isAir()
                || adjacentState.getLightEmission() > 0
                || adjacentState.getFluidState().is(FluidTags.LAVA);
    }

    private static SourceLight sourceLight(BlockState state) {
        if (state == null || state.isAir()) {
            return null;
        }
        boolean lava = state.getFluidState().is(FluidTags.LAVA);
        int emission = Math.max(0, state.getLightEmission());
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        boolean knownEmissiveBlock = blockId.contains("sea_lantern")
                || blockId.contains("glowstone")
                || blockId.contains("shroomlight")
                || blockId.contains("redstone_lamp")
                || blockId.contains("lantern")
                || blockId.contains("torch")
                || blockId.contains("conduit");
        if (emission <= 0 && !lava && !knownEmissiveBlock) {
            return null;
        }

        double intensity = Math.max(0.45, Math.min(1.0, (knownEmissiveBlock && emission <= 0 ? 15 : emission) / 15.0));
        if (lava || blockId.contains("lava")) {
            return new SourceLight(blockId, 255, 104, 22, Math.max(0.85, intensity));
        }
        if (blockId.contains("sea_lantern") || blockId.contains("conduit")) {
            return new SourceLight(blockId, 152, 238, 255, intensity);
        }
        if (blockId.contains("soul")) {
            return new SourceLight(blockId, 78, 196, 255, intensity);
        }
        if (blockId.contains("redstone_lamp")) {
            return new SourceLight(blockId, 255, 132, 58, intensity);
        }
        if (blockId.contains("shroomlight")) {
            return new SourceLight(blockId, 255, 118, 80, intensity);
        }
        if (blockId.contains("glowstone") || blockId.contains("lantern") || blockId.contains("torch")) {
            return new SourceLight(blockId, 255, 196, 82, intensity);
        }
        return new SourceLight(blockId, 255, 184, 96, intensity);
    }

    private static Vec3 faceNormal(Direction face) {
        return switch (face) {
            case UP -> new Vec3(0.0, 1.0, 0.0);
            case DOWN -> new Vec3(0.0, -1.0, 0.0);
            case EAST -> new Vec3(1.0, 0.0, 0.0);
            case WEST -> new Vec3(-1.0, 0.0, 0.0);
            case SOUTH -> new Vec3(0.0, 0.0, 1.0);
            case NORTH -> new Vec3(0.0, 0.0, -1.0);
        };
    }

    private static Vec3 faceCorner(BlockPos pos, Direction face, int corner, double inset) {
        double clampedInset = Math.max(FACE_INSET, Math.min(0.42, inset));
        double x0 = pos.getX() + clampedInset;
        double x1 = pos.getX() + 1.0 - clampedInset;
        double y0 = pos.getY() + clampedInset;
        double y1 = pos.getY() + 1.0 - clampedInset;
        double z0 = pos.getZ() + clampedInset;
        double z1 = pos.getZ() + 1.0 - clampedInset;
        return switch (face) {
            case UP -> switch (corner) {
                case 0 -> new Vec3(x0, pos.getY() + 1.0 + FACE_EPSILON, z0);
                case 1 -> new Vec3(x1, pos.getY() + 1.0 + FACE_EPSILON, z0);
                case 2 -> new Vec3(x1, pos.getY() + 1.0 + FACE_EPSILON, z1);
                default -> new Vec3(x0, pos.getY() + 1.0 + FACE_EPSILON, z1);
            };
            case DOWN -> switch (corner) {
                case 0 -> new Vec3(x0, pos.getY() - FACE_EPSILON, z1);
                case 1 -> new Vec3(x1, pos.getY() - FACE_EPSILON, z1);
                case 2 -> new Vec3(x1, pos.getY() - FACE_EPSILON, z0);
                default -> new Vec3(x0, pos.getY() - FACE_EPSILON, z0);
            };
            case EAST -> switch (corner) {
                case 0 -> new Vec3(pos.getX() + 1.0 + FACE_EPSILON, y0, z1);
                case 1 -> new Vec3(pos.getX() + 1.0 + FACE_EPSILON, y1, z1);
                case 2 -> new Vec3(pos.getX() + 1.0 + FACE_EPSILON, y1, z0);
                default -> new Vec3(pos.getX() + 1.0 + FACE_EPSILON, y0, z0);
            };
            case WEST -> switch (corner) {
                case 0 -> new Vec3(pos.getX() - FACE_EPSILON, y0, z0);
                case 1 -> new Vec3(pos.getX() - FACE_EPSILON, y1, z0);
                case 2 -> new Vec3(pos.getX() - FACE_EPSILON, y1, z1);
                default -> new Vec3(pos.getX() - FACE_EPSILON, y0, z1);
            };
            case SOUTH -> switch (corner) {
                case 0 -> new Vec3(x0, y0, pos.getZ() + 1.0 + FACE_EPSILON);
                case 1 -> new Vec3(x1, y0, pos.getZ() + 1.0 + FACE_EPSILON);
                case 2 -> new Vec3(x1, y1, pos.getZ() + 1.0 + FACE_EPSILON);
                default -> new Vec3(x0, y1, pos.getZ() + 1.0 + FACE_EPSILON);
            };
            case NORTH -> switch (corner) {
                case 0 -> new Vec3(x1, y0, pos.getZ() - FACE_EPSILON);
                case 1 -> new Vec3(x0, y0, pos.getZ() - FACE_EPSILON);
                case 2 -> new Vec3(x0, y1, pos.getZ() - FACE_EPSILON);
                default -> new Vec3(x1, y1, pos.getZ() - FACE_EPSILON);
            };
        };
    }

    private static void appendSourceId(StringBuilder builder, String blockId) {
        if (builder.indexOf(blockId) >= 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(',');
        }
        builder.append(blockId);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SourceLight(String blockId, int red, int green, int blue, double intensity) {
        int argb(double strength) {
            int alpha = clampInt((int) Math.round(20.0 + strength * 86.0), 14, 96);
            return (alpha << 24)
                    | (clampInt(this.red, 0, 255) << 16)
                    | (clampInt(this.green, 0, 255) << 8)
                    | clampInt(this.blue, 0, 255);
        }
    }

    private record MaterialTint(String blockId, int red, int green, int blue, double reflectance) {
        SourceLight bounceLight(SourceLight sourceLight, double incidentStrength) {
            double sourceEnergy = sourceLight == null ? 0.45 : sourceLight.intensity();
            double intensity = Math.max(0.0, Math.min(0.42, incidentStrength * this.reflectance * sourceEnergy));
            return new SourceLight(this.blockId, this.red, this.green, this.blue, intensity);
        }
    }

    private record FaceHit(Direction face, double weight) {
    }

    private record SourceCandidate(BlockPos position, Vec3 center, SourceLight light) {
    }

    private record SourceSpillStats(int receiverQuadCount, int secondaryBounceQuadCount, int coloredPanelReceiverCount) {
    }

    private record SpillStats(
            int sourceCount,
            int receiverQuadCount,
            int secondaryBounceQuadCount,
            int coloredPanelReceiverCount,
            String sourceIds
    ) {
    }
}
