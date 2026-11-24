package com.cosmicsky.mixin;

import com.cosmicsky.SkyDrawer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    /**
     * Replaces vanilla sun/moon/stars with ours. NOTE: this parameter list is my best guess for 1.21.11
     * (Mojang changes it often). If the log says "Invalid descriptor", the log prints the real one.
     */
    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true, require = 0)
    private void cosmicsky$render(MatrixStack matrices, VertexConsumerProvider.Immediate consumers,
                                  float skyAngle, @Coerce Object moonPhase, float rainGradient,
                                  float starBrightness, @Coerce Object fog, CallbackInfo ci) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null || world.getRegistryKey() != World.OVERWORLD) return;
        SkyDrawer.draw(matrices, consumers, world);
        ci.cancel();
    }
}
