// skip.renderer.client.mixin.net.carbonmc.graphene.FramebufferMixin.java
package net.carbonmc.graphene.mixin.client.renderer.skip;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.carbonmc.graphene.config.CoolConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class FramebufferMixin {

    @Inject(
            method = "copyDepthFrom(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipDepthCopy(RenderTarget source, CallbackInfo ci) {
        if (CoolConfig.skipFramebufferCopy.get()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "_blitToScreen(IIZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipBlitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {
        if (CoolConfig.skipFramebufferCopy.get()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "_bindWrite(Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipBindWrite(boolean updateViewport, CallbackInfo ci) {
        if (CoolConfig.skipFramebufferCopy.get()) {
            ci.cancel();
        }
    }
}