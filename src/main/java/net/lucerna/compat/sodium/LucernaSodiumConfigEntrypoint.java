package net.lucerna.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.lucerna.Lucerna;
import net.lucerna.gui.LucernaSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LucernaSodiumConfigEntrypoint implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .setName(Lucerna.MOD_NAME)
                .setVersion("0.1.0")
                .addPage(builder.createExternalPage()
                        .setName(Component.translatable("lucerna.options.title"))
                        .setScreenConsumer(parent -> Minecraft.getInstance().gui.setScreen(new LucernaSettingsScreen(parent))));
    }
}
