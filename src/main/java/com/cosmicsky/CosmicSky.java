package com.cosmicsky;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class CosmicSky implements ClientModInitializer {
    public static final String MOD_ID = "cosmicsky";
    public static CosmicConfig CONFIG = new CosmicConfig();
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        CONFIG = CosmicConfig.load();

        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.cosmicsky.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.setScreen(new CosmicSkyScreen(null));
                }
            }
        });
    }
}
