package net.lucerna.render.frame;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class ReflectiveFrameContextExtractor {
    private static final float TWO_PI = (float) (Math.PI * 2.0D);

    private ReflectiveFrameContextExtractor() {
    }

    static FrameConstantsCaptureRequest extract(
            Object minecraftClient,
            Object renderContext,
            FrameRenderFlags flags,
            long frameIndex,
            float tickDelta,
            FrameJitter explicitJitter
    ) {
        Object client = clientOrInstance(minecraftClient);
        Object gameRenderer = first(readMember(renderContext, "gameRenderer"), readMember(client, "gameRenderer"));
        Object gameRenderState = first(readMember(renderContext, "gameRenderState"), readMember(gameRenderer, "gameRenderState"));
        Object levelRenderState = first(readMember(gameRenderState, "levelRenderState"), readMember(renderContext, "levelRenderState"));
        Object cameraRenderState = first(
                readMember(levelRenderState, "cameraRenderState"),
                readMember(renderContext, "cameraRenderState")
        );

        FrameConstantsCaptureRequest.Builder builder = FrameConstantsCaptureRequest.builder(frameIndex, tickDelta)
                .source(FrameConstantsCaptureRequest.SOURCE_MINECRAFT_REFLECTION)
                .viewport(captureViewport(client, renderContext, gameRenderState))
                .worldState(captureWorldState(client, renderContext, levelRenderState, tickDelta))
                .cameraMatrices(captureCameraMatrices(renderContext, gameRenderState, levelRenderState, cameraRenderState))
                .jitter(captureJitter(renderContext, gameRenderState, levelRenderState, explicitJitter))
                .flags(captureRenderFlags(renderContext, gameRenderState, levelRenderState, flags));

        if (historyResetRequested(renderContext, gameRenderState, levelRenderState)) {
            builder.requestHistoryReset("Render context requested temporal history reset.");
        }

        return builder.build();
    }

    private static FrameViewport captureViewport(Object client, Object renderContext, Object gameRenderState) {
        Object windowState = first(
                readMember(renderContext, "windowRenderState"),
                readMember(gameRenderState, "windowRenderState")
        );
        FrameViewport fromWindowState = viewportFrom(windowState);
        if (fromWindowState.available()) {
            return fromWindowState;
        }

        FrameViewport fromContext = viewportFrom(renderContext);
        if (fromContext.available()) {
            return fromContext;
        }

        Object window = readMember(client, "getWindow", "window");
        FrameViewport fromWindow = viewportFrom(window);
        if (fromWindow.available()) {
            return fromWindow;
        }

        return FrameViewport.UNAVAILABLE;
    }

    private static FrameViewport viewportFrom(Object source) {
        int width = readInt(source, "width", "getWidth", "viewportWidth", "getViewportWidth");
        int height = readInt(source, "height", "getHeight", "viewportHeight", "getViewportHeight");
        return new FrameViewport(width, height);
    }

    private static WorldRenderState captureWorldState(
            Object client,
            Object renderContext,
            Object levelRenderState,
            float tickDelta
    ) {
        Object world = first(
                readMember(renderContext, "world", "level", "clientLevel"),
                readMember(client, "level", "world", "getLevel")
        );
        if (world == null && levelRenderState == null) {
            return WorldRenderState.unavailable();
        }

        String dimensionId = dimensionId(world);
        long timeOfDay = firstLong(
                readLongObject(world, "getOverworldClockTime", "getDefaultClockTime", "getDayTime", "dayTime"),
                readLongObject(readMember(world, "getLevelData", "levelData"), "getGameTime", "gameTime"),
                readLongObject(levelRenderState, "gameTime")
        );
        float celestialAngle = celestialAngle(levelRenderState, timeOfDay);
        float rainIntensity = firstFiniteFloat(
                readFloatObject(world, tickDelta, "getRainLevel", "getRainGradient"),
                readFloatObject(readMember(levelRenderState, "weatherRenderState"), "intensity")
        );
        float thunderIntensity = firstFiniteFloat(
                readFloatObject(world, tickDelta, "getThunderLevel", "getThunderGradient")
        );
        boolean raining = readBoolean(world, "isRaining", "raining") || rainIntensity > 0.001F;
        boolean thundering = readBoolean(world, "isThundering", "thundering") || thunderIntensity > 0.001F;

        return new WorldRenderState(
                dimensionId,
                timeOfDay,
                celestialAngle,
                raining,
                thundering,
                rainIntensity,
                thunderIntensity
        );
    }

    private static String dimensionId(Object world) {
        Object dimension = readMember(world, "dimension", "getDimension");
        Object identifier = first(
                readMember(dimension, "identifier", "location"),
                readMember(readMember(dimension, "value"), "identifier", "location")
        );
        if (identifier != null) {
            return identifier.toString();
        }
        if (dimension != null) {
            String value = dimension.toString();
            int slash = value.lastIndexOf('/');
            int close = value.lastIndexOf(']');
            if (slash >= 0 && close > slash) {
                return value.substring(slash + 1, close).trim();
            }
            return value;
        }
        return WorldRenderState.UNKNOWN_DIMENSION;
    }

    private static float celestialAngle(Object levelRenderState, long timeOfDay) {
        if (timeOfDay >= 0L) {
            long wrappedTime = Math.floorMod(timeOfDay, 24_000L);
            return (float) wrappedTime / 24_000.0F;
        }
        Object skyState = readMember(levelRenderState, "skyRenderState");
        float sunAngle = readFloat(skyState, "sunAngle", "celestialAngle");
        if (!Float.isFinite(sunAngle) || sunAngle < 0.0F) {
            return -1.0F;
        }
        if (sunAngle <= 1.0F) {
            return sunAngle;
        }
        if (sunAngle <= TWO_PI) {
            return sunAngle / TWO_PI;
        }
        return sunAngle % 1.0F;
    }

    private static FrameCameraMatrices captureCameraMatrices(
            Object renderContext,
            Object gameRenderState,
            Object levelRenderState,
            Object cameraRenderState
    ) {
        Object projectionSource = first(
                readMember(renderContext, "projectionMatrix", "getProjectionMatrix"),
                readMember(cameraRenderState, "projectionMatrix"),
                readMember(gameRenderState, "projectionMatrix"),
                readMember(levelRenderState, "projectionMatrix")
        );
        Object viewSource = first(
                readMember(renderContext, "positionMatrix", "viewMatrix", "modelViewMatrix", "getPositionMatrix"),
                readMember(cameraRenderState, "viewMatrix", "viewRotationMatrix"),
                readMember(gameRenderState, "viewMatrix"),
                readMember(levelRenderState, "viewMatrix")
        );

        FrameMatrix4f projection = matrixFrom(projectionSource);
        FrameMatrix4f view = matrixFrom(viewSource);
        if (projection == null || view == null) {
            return FrameCameraMatrices.unavailable();
        }

        FrameMatrix4f viewProjection = multiply(projection, view);
        FrameMatrix4f inverseViewProjection = invert(viewProjection);
        if (inverseViewProjection == null) {
            return FrameCameraMatrices.unavailable();
        }

        return new FrameCameraMatrices(view, projection, viewProjection, inverseViewProjection, true);
    }

    private static FrameJitter captureJitter(
            Object renderContext,
            Object gameRenderState,
            Object levelRenderState,
            FrameJitter explicitJitter
    ) {
        if (explicitJitter != null && explicitJitter.enabled()) {
            return explicitJitter;
        }
        FrameJitter contextJitter = jitterFrom(first(
                readMember(renderContext, "jitter", "frameJitter", "temporalJitter"),
                readMember(gameRenderState, "jitter", "frameJitter", "temporalJitter"),
                readMember(levelRenderState, "jitter", "frameJitter", "temporalJitter")
        ));
        if (contextJitter != null) {
            return contextJitter;
        }
        return explicitJitter == null ? FrameJitter.disabled() : explicitJitter;
    }

    private static FrameRenderFlags captureRenderFlags(
            Object renderContext,
            Object gameRenderState,
            Object levelRenderState,
            FrameRenderFlags explicitFlags
    ) {
        if (explicitFlags != null && explicitFlags.available()) {
            return explicitFlags;
        }
        Object flags = first(
                readMember(renderContext, "lucernaRenderFlags", "frameRenderFlags", "renderFlags"),
                readMember(gameRenderState, "lucernaRenderFlags", "frameRenderFlags", "renderFlags"),
                readMember(levelRenderState, "lucernaRenderFlags", "frameRenderFlags", "renderFlags")
        );
        if (flags instanceof FrameRenderFlags frameRenderFlags) {
            return frameRenderFlags;
        }
        return explicitFlags == null ? FrameRenderFlags.unavailable() : explicitFlags;
    }

    private static FrameJitter jitterFrom(Object source) {
        if (source instanceof FrameJitter jitter) {
            return jitter;
        }
        if (source instanceof float[] values && values.length >= 2) {
            return new FrameJitter(values[0], values[1], 0, 1, true);
        }
        if (source == null) {
            return null;
        }

        float x = readFloat(source, "x", "jitterX");
        float y = readFloat(source, "y", "jitterY");
        int sequenceIndex = readInt(source, "sequenceIndex", "index", "frameIndex");
        int sequenceLength = readInt(source, "sequenceLength", "length");
        boolean enabled = readBoolean(source, "enabled", "isEnabled") || Float.isFinite(x) || Float.isFinite(y);
        if (!enabled) {
            return null;
        }
        return new FrameJitter(x, y, sequenceIndex, Math.max(1, sequenceLength), true);
    }

    private static boolean historyResetRequested(Object renderContext, Object gameRenderState, Object levelRenderState) {
        return readBoolean(renderContext,
                "historyReset",
                "historyResetRequested",
                "isHistoryResetRequested",
                "shouldResetHistory",
                "shouldResetTemporalHistory"
        ) || readBoolean(gameRenderState,
                "historyReset",
                "historyResetRequested",
                "shouldResetHistory",
                "shouldResetTemporalHistory"
        ) || readBoolean(levelRenderState,
                "historyReset",
                "historyResetRequested",
                "shouldResetHistory",
                "shouldResetTemporalHistory"
        );
    }

    private static Object clientOrInstance(Object explicitClient) {
        if (explicitClient != null) {
            return explicitClient;
        }
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            return minecraftClass.getMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException
                 | RuntimeException ignored) {
            return null;
        }
    }

    private static Object readMember(Object target, String... names) {
        Object value = invokeNoArg(target, names);
        if (value != null) {
            return value;
        }
        return readField(target, names);
    }

    private static Object invokeNoArg(Object target, String... names) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Method method = type.getMethod(name);
                return method.invoke(target);
            } catch (IllegalAccessException
                     | InvocationTargetException
                     | NoSuchMethodException
                     | RuntimeException ignored) {
                // Keep capture best-effort; missing or failing probes just fall through.
            }
        }
        return null;
    }

    private static Object invokeFloat(Object target, float value, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                Object argument = floatArgument(value, parameterType);
                if (argument == null) {
                    continue;
                }
                try {
                    return method.invoke(target, argument);
                } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
                    // Keep capture best-effort.
                }
            }
        }
        return null;
    }

    private static Object floatArgument(float value, Class<?> parameterType) {
        if (parameterType == float.class || parameterType == Float.class) {
            return value;
        }
        if (parameterType == double.class || parameterType == Double.class) {
            return (double) value;
        }
        return null;
    }

    private static Object readField(Object target, String... names) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Field field = type.getField(name);
                return field.get(target);
            } catch (IllegalAccessException | NoSuchFieldException | RuntimeException ignored) {
                // Keep capture best-effort.
            }
        }
        return null;
    }

    private static int readInt(Object target, String... names) {
        Object value = readMember(target, names);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        return 0;
    }

    private static Long readLongObject(Object target, String... names) {
        Object value = readMember(target, names);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private static float readFloat(Object target, String... names) {
        Float value = readFloatObject(target, names);
        return value == null ? Float.NaN : value;
    }

    private static Float readFloatObject(Object target, String... names) {
        Object value = readMember(target, names);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return null;
    }

    private static Float readFloatObject(Object target, float argument, String... names) {
        Object value = invokeFloat(target, argument, names);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return null;
    }

    private static boolean readBoolean(Object target, String... names) {
        Object value = readMember(target, names);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return false;
    }

    private static long firstLong(Long... values) {
        for (Long value : values) {
            if (value != null && value >= 0L) {
                return value;
            }
        }
        return -1L;
    }

    private static float firstFiniteFloat(Float... values) {
        for (Float value : values) {
            if (value != null && Float.isFinite(value)) {
                return value;
            }
        }
        return 0.0F;
    }

    private static Object first(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static FrameMatrix4f matrixFrom(Object source) {
        if (source instanceof FrameMatrix4f matrix) {
            return matrix;
        }
        if (source instanceof float[] values && values.length == 16) {
            return FrameMatrix4f.fromRowMajor(values.clone());
        }
        if (source == null) {
            return null;
        }

        Float m00 = readFloatObject(source, "m00");
        Float m01 = readFloatObject(source, "m01");
        Float m02 = readFloatObject(source, "m02");
        Float m03 = readFloatObject(source, "m03");
        Float m10 = readFloatObject(source, "m10");
        Float m11 = readFloatObject(source, "m11");
        Float m12 = readFloatObject(source, "m12");
        Float m13 = readFloatObject(source, "m13");
        Float m20 = readFloatObject(source, "m20");
        Float m21 = readFloatObject(source, "m21");
        Float m22 = readFloatObject(source, "m22");
        Float m23 = readFloatObject(source, "m23");
        Float m30 = readFloatObject(source, "m30");
        Float m31 = readFloatObject(source, "m31");
        Float m32 = readFloatObject(source, "m32");
        Float m33 = readFloatObject(source, "m33");
        if (m00 == null || m01 == null || m02 == null || m03 == null
                || m10 == null || m11 == null || m12 == null || m13 == null
                || m20 == null || m21 == null || m22 == null || m23 == null
                || m30 == null || m31 == null || m32 == null || m33 == null) {
            return null;
        }
        return new FrameMatrix4f(
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33
        );
    }

    private static FrameMatrix4f multiply(FrameMatrix4f left, FrameMatrix4f right) {
        float[] a = left.toRowMajorArray();
        float[] b = right.toRowMajorArray();
        float[] out = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                out[row * 4 + column] = a[row * 4] * b[column]
                        + a[row * 4 + 1] * b[4 + column]
                        + a[row * 4 + 2] * b[8 + column]
                        + a[row * 4 + 3] * b[12 + column];
            }
        }
        return FrameMatrix4f.fromRowMajor(out);
    }

    private static FrameMatrix4f invert(FrameMatrix4f matrix) {
        float[] m = matrix.toRowMajorArray();
        float[] inv = new float[16];

        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
                + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
                - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
                + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
                - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
                - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
                + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
                - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
                + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
                + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
                - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
                + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
                - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
                - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
                + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
                - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
                + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];

        float determinant = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 0.000001F) {
            return null;
        }

        float inverseDeterminant = 1.0F / determinant;
        for (int i = 0; i < inv.length; i++) {
            inv[i] *= inverseDeterminant;
        }
        return FrameMatrix4f.fromRowMajor(inv);
    }
}
