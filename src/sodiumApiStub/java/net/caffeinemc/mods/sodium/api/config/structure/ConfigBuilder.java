package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

public interface ConfigBuilder {
    ModOptionsBuilder registerModOptions(String configId, String name, String version);

    ModOptionsBuilder registerModOptions(String configId);

    ModOptionsBuilder registerOwnModOptions();

    ColorThemeBuilder createColorTheme();

    ExternalPageBuilder createExternalPage();

    BooleanOptionBuilder createBooleanOption(Identifier id);
}
