package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

public interface ModOptionsBuilder {
    ModOptionsBuilder setName(String name);

    ModOptionsBuilder setVersion(String version);

    ModOptionsBuilder setColorTheme(ColorThemeBuilder colorTheme);

    ModOptionsBuilder setIcon(Identifier texture);

    ModOptionsBuilder setNonTintedIcon(Identifier texture);

    ModOptionsBuilder addPage(PageBuilder page);
}
