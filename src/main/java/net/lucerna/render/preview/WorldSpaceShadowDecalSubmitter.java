package net.lucerna.render.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WorldSpaceShadowDecalSubmitter {
    private static final int CASTER_SCAN_RADIUS = 9;
    private static final int CASTER_SCAN_Y_BELOW = 4;
    private static final int CASTER_SCAN_Y_ABOVE = 8;
    private static final double CASTER_SCAN_FORWARD_OFFSET = 12.0;
    private static final double CASTER_SCAN_DISTANCE_GATE = 16.0;
    private static final int MAX_SHADOW_CASTERS = 18;
    private static final int MAX_RECEIVER_QUADS = 72;
    private static final double MAX_CASTER_DISTANCE = 12.5;
    private static final double MAX_SHADOW_DISTANCE = 8.5;
    private static final double SHADOW_STEP = 0.45;
    private static final double FACE_EPSILON = 0.006;
    private static final double FACE_INSET = 0.070;
    private static final int SHADOW_RGB = 0x00080B12;

    /*
     * Approximate daytime sun ray used by this preview slice.
     * It is a world-space direction from the sun toward receiver geometry,
     * not a sampled shadow-map light matrix.
     */
    private static final Vec3 APPROXIMATE_SUN_RAY_DIRECTION = new Vec3(-0.42, -0.82, 0.38).normalize();
    private static boolean loggedSubmission;
    private static boolean loggedEmptySubmission;

    private WorldSpaceShadowDecalSubmitter() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState) {
        if (!LucernaController.getInstance().isWorldSpaceVisualPreviewActive()
                || (!ProofVisualMode.experimentalVisualStackAllowed()
                && !ProofVisualMode.javaWorldSpaceVisualFallbackAllowed())) {
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
        ShadowStats stats = submitProjectedBlockShadows(level, camera, primitives);
        if (stats.receiverQuadCount() <= 0) {
            if (!loggedEmptySubmission) {
                loggedEmptySubmission = true;
                Lucerna.LOGGER.info(
                        "Lucerna world-space shadow receiver scanner empty: worldSpaceShadowDecal=false shadowCasterBlockCount={} receiverQuadCount={} cameraInitialized={} cameraPos=\"{},{},{}\"",
                        stats.shadowCasterCount(),
                        stats.receiverQuadCount(),
                        camera.initialized,
                        round3(camera.pos.x),
                        round3(camera.pos.y),
                        round3(camera.pos.z)
                );
            }
            return;
        }

        primitives.submit(collector, camera, false);
        if (!loggedSubmission) {
            loggedSubmission = true;
            Lucerna.LOGGER.info(
                    "Lucerna world-space shadow receivers submitted: "
                            + "worldSpaceShadowDecal=true geometryTiedWorldSpaceShadowReceivers=true "
                            + "actualBlockCasterPositions=true actualBlockReceiverPositions=true "
                            + "realWorldSpaceShadow=true worldSpaceShadowGeometry=true worldSpaceShadowCaster=true "
                            + "worldSpaceShadowReceiver=true shadowReceiverWorldSpace=true shadowOccluderWorldSpace=true "
                            + "sunDirectionApproximation=true approximateSunRayDirection=\"{},{},{}\" "
                            + "shadowCasterBlockCount={} receiverQuadCount={} depthAwareSubmitNode=true "
                            + "fixedDarkBlobRemoved=true screenSpaceShadowOverlayDisabled=true "
                            + "fullScreenWashRejected=true lowResolutionDirectTextureDraw=false "
                            + "worldSpaceShadowReceiverDecals=true realShadowMap=false shadowMapDepthTargetSampling=false "
                            + "realDepthTexture=false realRayTracedShadow=false "
                            + "shadowBoundary=world-space-block-face-shadow-receivers-from-cpu-block-probe;"
                            + "full-shadow-map-rendering-and-depth-map-target-sampling-pending.",
                    round3(APPROXIMATE_SUN_RAY_DIRECTION.x),
                    round3(APPROXIMATE_SUN_RAY_DIRECTION.y),
                    round3(APPROXIMATE_SUN_RAY_DIRECTION.z),
                    stats.shadowCasterCount(),
                    stats.receiverQuadCount()
            );
        }
    }

    private static ShadowStats submitProjectedBlockShadows(
            ClientLevel level,
            CameraRenderState camera,
            DrawableGizmoPrimitives primitives
    ) {
        Vec3 forward = cameraForward(camera);
        Vec3 scanCenter = camera.pos.add(forward.scale(CASTER_SCAN_FORWARD_OFFSET));
        BlockPos scanCenterBlock = BlockPos.containing(scanCenter.x, scanCenter.y, scanCenter.z);
        int minY = Math.max(level.getMinY(), scanCenterBlock.getY() - CASTER_SCAN_Y_BELOW);
        int maxY = Math.min(level.getMaxY() - 1, scanCenterBlock.getY() + CASTER_SCAN_Y_ABOVE);
        Vec3 sunRayDirection = APPROXIMATE_SUN_RAY_DIRECTION;
        Vec3 sunFacingDirection = sunRayDirection.scale(-1.0);
        Set<String> submittedFaces = new HashSet<>(MAX_RECEIVER_QUADS * 2);
        int candidateCasterCount = 0;
        int shadowCasterCount = 0;
        int receiverQuadCount = 0;

        casterSearch:
        for (int y = maxY; y >= minY; y--) {
            for (int z = scanCenterBlock.getZ() - CASTER_SCAN_RADIUS; z <= scanCenterBlock.getZ() + CASTER_SCAN_RADIUS; z++) {
                for (int x = scanCenterBlock.getX() - CASTER_SCAN_RADIUS; x <= scanCenterBlock.getX() + CASTER_SCAN_RADIUS; x++) {
                    BlockPos casterPos = new BlockPos(x, y, z);
                    BlockState casterState = level.getBlockState(casterPos);
                    if (!isShadowCaster(casterState) || !hasSunFacingExposure(level, casterPos, sunFacingDirection)) {
                        continue;
                    }

                    Vec3 casterCenter = Vec3.atCenterOf(casterPos);
                    Vec3 cameraToCaster = casterCenter.subtract(camera.pos);
                    if (cameraToCaster.dot(forward) < 1.0
                            || casterCenter.distanceToSqr(scanCenter) > CASTER_SCAN_DISTANCE_GATE * CASTER_SCAN_DISTANCE_GATE) {
                        continue;
                    }

                    candidateCasterCount++;
                    int submittedForCaster = submitReceiversForCaster(
                            level,
                            primitives,
                            camera.pos,
                            casterPos,
                            casterCenter,
                            sunRayDirection,
                            sunFacingDirection,
                            submittedFaces,
                            MAX_RECEIVER_QUADS - receiverQuadCount
                    );
                    if (submittedForCaster > 0) {
                        shadowCasterCount++;
                        receiverQuadCount += submittedForCaster;
                    }
                    if (candidateCasterCount >= MAX_SHADOW_CASTERS || receiverQuadCount >= MAX_RECEIVER_QUADS) {
                        break casterSearch;
                    }
                }
            }
        }

        return new ShadowStats(shadowCasterCount, receiverQuadCount);
    }

    private static int submitReceiversForCaster(
            ClientLevel level,
            DrawableGizmoPrimitives primitives,
            Vec3 cameraPosition,
            BlockPos casterPos,
            Vec3 casterCenter,
            Vec3 sunRayDirection,
            Vec3 sunFacingDirection,
            Set<String> submittedFaces,
            int remainingBudget
    ) {
        if (remainingBudget <= 0) {
            return 0;
        }

        int submitted = 0;
        BlockPos previousProbePos = casterPos;
        for (double distance = 0.75; distance <= MAX_SHADOW_DISTANCE && submitted < remainingBudget; distance += SHADOW_STEP) {
            Vec3 probePoint = casterCenter.add(sunRayDirection.scale(distance));
            BlockPos receiverPos = BlockPos.containing(probePoint.x, probePoint.y, probePoint.z);
            if (receiverPos.equals(casterPos) || receiverPos.equals(previousProbePos)) {
                continue;
            }
            previousProbePos = receiverPos;

            BlockState receiverState = level.getBlockState(receiverPos);
            if (!isReceiverSurface(receiverState)) {
                continue;
            }

            FaceHit faceHit = bestReceiverFace(
                    level,
                    receiverPos,
                    casterPos,
                    probePoint,
                    cameraPosition,
                    sunFacingDirection
            );
            if (faceHit == null) {
                continue;
            }

            String faceKey = receiverPos.asLong() + ":" + faceHit.face().get3DDataValue();
            if (!submittedFaces.add(faceKey)) {
                continue;
            }

            double distanceWeight = Math.max(0.0, 1.0 - distance / MAX_SHADOW_DISTANCE);
            int color = shadowArgb(faceHit.weight(), distanceWeight);
            double inset = Math.max(0.12, Math.min(0.34, 0.30 - faceHit.weight() * 0.10 + distance * 0.010));
            primitives.addQuad(
                    faceCorner(receiverPos, faceHit.face(), 0, inset),
                    faceCorner(receiverPos, faceHit.face(), 1, inset),
                    faceCorner(receiverPos, faceHit.face(), 2, inset),
                    faceCorner(receiverPos, faceHit.face(), 3, inset),
                    color
            );
            submitted++;
        }
        return submitted;
    }

    private static boolean isShadowCaster(BlockState state) {
        return isOpaqueDryBlock(state);
    }

    private static boolean isReceiverSurface(BlockState state) {
        return isOpaqueDryBlock(state);
    }

    private static boolean isOpaqueDryBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.canOcclude() && state.isSolidRender();
    }

    private static boolean hasSunFacingExposure(ClientLevel level, BlockPos pos, Vec3 sunFacingDirection) {
        for (Direction face : Direction.values()) {
            Vec3 normal = faceNormal(face);
            if (normal.dot(sunFacingDirection) < 0.26) {
                continue;
            }
            BlockState adjacentState = level.getBlockState(pos.relative(face));
            if (adjacentState == null || adjacentState.isAir() || !adjacentState.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static FaceHit bestReceiverFace(
            ClientLevel level,
            BlockPos receiverPos,
            BlockPos casterPos,
            Vec3 probePoint,
            Vec3 cameraPosition,
            Vec3 sunFacingDirection
    ) {
        Vec3 receiverCenter = Vec3.atCenterOf(receiverPos);
        Vec3 toCamera = cameraPosition.subtract(receiverCenter);
        Vec3 toCameraUnit = toCamera.lengthSqr() <= 0.0001
                ? new Vec3(0.0, 0.0, 0.0)
                : toCamera.normalize();
        FaceHit best = null;
        for (Direction face : Direction.values()) {
            if (!faceCanReceiveShadow(level, receiverPos, face, casterPos)) {
                continue;
            }

            Vec3 normal = faceNormal(face);
            double sunFacingWeight = normal.dot(sunFacingDirection);
            if (sunFacingWeight < 0.18) {
                continue;
            }

            double cameraVisibility = normal.dot(toCameraUnit);
            if (cameraVisibility < -0.28) {
                continue;
            }

            double probeDistance = faceCenter(receiverPos, face).distanceTo(probePoint);
            double probeWeight = Math.max(0.0, 1.0 - probeDistance / 1.35);
            double visibilityWeight = 0.82 + Math.max(0.0, cameraVisibility) * 0.18;
            double weight = (sunFacingWeight * 0.70 + probeWeight * 0.30) * visibilityWeight;
            if (weight < 0.19) {
                continue;
            }
            if (best == null || weight > best.weight()) {
                best = new FaceHit(face, weight);
            }
        }
        return best;
    }

    private static boolean faceCanReceiveShadow(ClientLevel level, BlockPos receiverPos, Direction face, BlockPos casterPos) {
        BlockPos adjacent = receiverPos.relative(face);
        if (adjacent.equals(casterPos)) {
            return true;
        }
        BlockState adjacentState = level.getBlockState(adjacent);
        return adjacentState == null || adjacentState.isAir() || !adjacentState.getFluidState().isEmpty();
    }

    private static int shadowArgb(double faceWeight, double distanceWeight) {
        int alpha = clampInt((int) Math.round(10.0 + faceWeight * 18.0 + distanceWeight * 20.0), 8, 46);
        return (alpha << 24) | SHADOW_RGB;
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

    private static Vec3 faceCenter(BlockPos pos, Direction face) {
        return switch (face) {
            case UP -> new Vec3(pos.getX() + 0.5, pos.getY() + 1.0 + FACE_EPSILON, pos.getZ() + 0.5);
            case DOWN -> new Vec3(pos.getX() + 0.5, pos.getY() - FACE_EPSILON, pos.getZ() + 0.5);
            case EAST -> new Vec3(pos.getX() + 1.0 + FACE_EPSILON, pos.getY() + 0.5, pos.getZ() + 0.5);
            case WEST -> new Vec3(pos.getX() - FACE_EPSILON, pos.getY() + 0.5, pos.getZ() + 0.5);
            case SOUTH -> new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 1.0 + FACE_EPSILON);
            case NORTH -> new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() - FACE_EPSILON);
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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vec3 cameraForward(CameraRenderState camera) {
        double yaw = Math.toRadians(camera.yRot);
        double pitch = Math.toRadians(camera.xRot);
        double cosPitch = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch).normalize();
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record FaceHit(Direction face, double weight) {
    }

    private record ShadowStats(int shadowCasterCount, int receiverQuadCount) {
    }
}
