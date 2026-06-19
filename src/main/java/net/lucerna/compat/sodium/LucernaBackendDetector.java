package net.lucerna.compat.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.lucerna.Lucerna;
import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class LucernaBackendDetector {
    private static final String SODIUM_MOD_ID = "sodium";
    private static final String UNKNOWN_BACKEND = "unknown";

    public BackendStatus detect() {
        SodiumState sodium = this.detectSodium();
        BackendProbe backend = this.readBackendName();
        String backendName = backend.backendName();
        String normalizedBackend = normalize(backendName);

        if (!sodium.present()) {
            if (isVulkanBackend(normalizedBackend)) {
                return BackendStatus.disabled(
                        BackendKind.MOJANG_VULKAN_EXPERIMENTAL,
                        backendName,
                        "Vulkan backend detected without Sodium; Lucerna requires Sodium Vulkan."
                );
            }

            return BackendStatus.disabled(BackendKind.NO_SODIUM_DISABLED, backendName, "Sodium is not installed; Lucerna is disabled.");
        }

        if (!backend.available()) {
            return BackendStatus.disabled(
                    BackendKind.UNKNOWN,
                    backendName,
                    sodium.displayName() + " is installed, but Minecraft has not exposed a renderer backend yet."
            );
        }

        if (isVulkanBackend(normalizedBackend)) {
            return BackendStatus.active(BackendKind.SODIUM_VULKAN, backendName, "Sodium Vulkan backend detected.");
        }

        if (isOpenGlBackend(normalizedBackend)) {
            return BackendStatus.disabled(BackendKind.OPENGL_DISABLED, backendName, "OpenGL backend detected; Lucerna requires Sodium Vulkan.");
        }

        return BackendStatus.disabled(BackendKind.UNSUPPORTED_DEVICE, backendName, "Unsupported renderer backend for Lucerna: " + backendName);
    }

    private SodiumState detectSodium() {
        FabricLoader loader = FabricLoader.getInstance();
        Optional<ModContainer> container = loader.getModContainer(SODIUM_MOD_ID);
        if (container.isPresent()) {
            String version = container.get().getMetadata().getVersion().getFriendlyString();
            return new SodiumState(true, version);
        }

        return new SodiumState(loader.isModLoaded(SODIUM_MOD_ID), "");
    }

    private BackendProbe readBackendName() {
        try {
            Object device = RenderSystem.class.getMethod("getDevice").invoke(null);
            if (device == null) {
                return BackendProbe.unavailable();
            }

            Object deviceInfo = invokeNoArg(device, "getDeviceInfo");
            String backendName = null;
            if (deviceInfo != null) {
                backendName = readStringMethod(deviceInfo, "backendName", "getBackendName", "backend");
            }

            if (backendName == null) {
                backendName = readStringMethod(device, "backendName", "getBackendName", "backend");
            }

            if (backendName == null || backendName.isBlank() || UNKNOWN_BACKEND.equalsIgnoreCase(backendName.trim())) {
                return BackendProbe.unavailable();
            }

            return BackendProbe.available(backendName);
        } catch (Throwable throwable) {
            Lucerna.LOGGER.debug("Could not read Minecraft renderer backend.", throwable);
            return BackendProbe.unavailable();
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String readStringMethod(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeNoArg(target, methodName);
            if (value instanceof String string) {
                return string;
            }

            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    private static boolean isVulkanBackend(String normalizedBackend) {
        return normalizedBackend.contains("vulkan");
    }

    private static boolean isOpenGlBackend(String normalizedBackend) {
        return normalizedBackend.contains("opengl")
                || normalizedBackend.contains("open gl")
                || normalizedBackend.equals("gl")
                || normalizedBackend.startsWith("gl ")
                || normalizedBackend.endsWith(" gl")
                || normalizedBackend.contains(" gl ");
    }

    private static String normalize(String backendName) {
        if (backendName == null) {
            return "";
        }

        return backendName.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');
    }

    private record BackendProbe(boolean available, String backendName) {
        static BackendProbe available(String backendName) {
            return new BackendProbe(true, backendName.trim());
        }

        static BackendProbe unavailable() {
            return new BackendProbe(false, UNKNOWN_BACKEND);
        }
    }

    private record SodiumState(boolean present, String version) {
        String displayName() {
            if (this.version == null || this.version.isBlank()) {
                return "Sodium";
            }

            return "Sodium " + this.version.trim();
        }
    }
}
