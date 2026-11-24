package com.cosmicsky.mixin;

import com.cosmicsky.CosmicSky;
import net.minecraft.client.render.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
    /** Tints the cloud color (keeps vanilla's day/night/sunset shading) or hides clouds via alpha 0. */
    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int cosmicsky$tintClouds(int color) {
        var cfg = CosmicSky.CONFIG;
        if (!cfg.cloudsEnabled) return color & 0x00FFFFFF;
        int a = (color >>> 24) & 255;
        int r = ((color >> 16) & 255) * ((cfg.cloudColor >> 16) & 255) / 255;
        int g = ((color >> 8) & 255) * ((cfg.cloudColor >> 8) & 255) / 255;
        int b = (color & 255) * (cfg.cloudColor & 255) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
