package net.lucerna;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lucerna {
    public static final String MOD_ID = "lucerna";
    public static final String MOD_NAME = "Lucerna";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private Lucerna() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
