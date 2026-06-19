package net.lucerna.compat.iris;

import net.fabricmc.loader.api.FabricLoader;
import net.lucerna.Lucerna;
import net.lucerna.compat.iris.IrisCompatStatus.ShaderPackState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class IrisCompat {
    private boolean disableAttempted;
    private boolean lastDisableSucceeded;
    private boolean disableFailureLogged;
    private boolean disableSuccessLogged;
    private boolean shaderStateFailureLogged;
    private String lastMessage = "Iris is not installed.";

    public boolean isIrisInstalled() {
        return FabricLoader.getInstance().isModLoaded("iris");
    }

    public void disableIrisShadersForLucerna() {
        if (!this.isIrisInstalled()) {
            this.disableAttempted = false;
            this.lastDisableSucceeded = false;
            this.disableFailureLogged = false;
            this.disableSuccessLogged = false;
            this.shaderStateFailureLogged = false;
            this.lastMessage = "Iris is not installed.";
            return;
        }

        ShaderPackState shaderPackState = this.readShaderPackState();
        if (shaderPackState == ShaderPackState.DISABLED) {
            this.disableAttempted = true;
            this.lastDisableSucceeded = true;
            this.disableFailureLogged = false;
            this.lastMessage = "Iris is installed; no shader pack is active while Lucerna is rendering.";
            if (!this.disableSuccessLogged) {
                Lucerna.LOGGER.info(this.lastMessage);
                this.disableSuccessLogged = true;
            }
            return;
        }

        this.disableAttempted = true;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            Object config = apiClass.getMethod("getConfig").invoke(api);
            Method disable = config.getClass().getMethod("setShadersEnabledAndApply", boolean.class);
            disable.invoke(config, false);
            this.lastDisableSucceeded = true;
            this.disableFailureLogged = false;
            this.lastMessage = "Iris is installed; shader packs are disabled by Lucerna.";
            if (!this.disableSuccessLogged) {
                Lucerna.LOGGER.info(this.lastMessage);
                this.disableSuccessLogged = true;
            }
        } catch (Throwable throwable) {
            if (this.isIrisConfigUnavailable(throwable)) {
                this.disableAttempted = false;
                this.lastDisableSucceeded = false;
                this.lastMessage = "Iris is installed; waiting for Iris config before disabling shader packs.";
                Lucerna.LOGGER.debug(this.lastMessage, throwable);
                return;
            }

            this.lastDisableSucceeded = false;
            this.lastMessage = "Iris is installed, but Lucerna could not disable Iris shader packs through the public API.";
            if (!this.disableFailureLogged) {
                Lucerna.LOGGER.warn(this.lastMessage, throwable);
                this.disableFailureLogged = true;
            } else {
                Lucerna.LOGGER.debug(this.lastMessage, throwable);
            }
        }
    }

    public boolean isShaderPackInUse() {
        return this.readShaderPackState() == ShaderPackState.ENABLED;
    }

    public IrisCompatStatus status() {
        if (!this.isIrisInstalled()) {
            return IrisCompatStatus.notInstalled();
        }

        ShaderPackState shaderPackState = this.readShaderPackState();
        if (!this.disableAttempted) {
            return IrisCompatStatus.installedPendingDisable(shaderPackState);
        }

        if (this.lastDisableSucceeded && shaderPackState == ShaderPackState.ENABLED) {
            return IrisCompatStatus.needsDisableReapply(shaderPackState);
        }

        if (this.lastDisableSucceeded) {
            return IrisCompatStatus.disabledForLucerna(shaderPackState);
        }

        return IrisCompatStatus.disableFailed(shaderPackState, this.lastMessage);
    }

    public String statusMessage() {
        return this.status().userMessage();
    }

    private ShaderPackState readShaderPackState() {
        if (!this.isIrisInstalled()) {
            return ShaderPackState.NOT_INSTALLED;
        }

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isShaderPackInUse").invoke(api);
            this.shaderStateFailureLogged = false;
            return Boolean.TRUE.equals(result) ? ShaderPackState.ENABLED : ShaderPackState.DISABLED;
        } catch (Throwable throwable) {
            if (!this.shaderStateFailureLogged) {
                Lucerna.LOGGER.debug("Could not query Iris shader-pack state.", throwable);
                this.shaderStateFailureLogged = true;
            }
            return ShaderPackState.UNKNOWN;
        }
    }

    private boolean isIrisConfigUnavailable(Throwable throwable) {
        Throwable cause = throwable;
        if (cause instanceof InvocationTargetException invocationTargetException && invocationTargetException.getCause() != null) {
            cause = invocationTargetException.getCause();
        }

        String message = cause.getMessage();
        return cause instanceof NullPointerException
                && message != null
                && message.contains("config");
    }
}
