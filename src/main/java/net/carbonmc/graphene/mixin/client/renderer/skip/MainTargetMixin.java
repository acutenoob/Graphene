// skip.renderer.client.mixin.net.carbonmc.graphene.MainTargetMixin.java
package net.carbonmc.graphene.mixin.client.renderer.skip;

import com.mojang.blaze3d.pipeline.MainTarget;
import net.carbonmc.graphene.config.CoolConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainTarget.class)
public abstract class MainTargetMixin {
    @Inject(
            method = "createFrameBuffer",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCreateFramebuffer(int width, int height, CallbackInfo ci) {
        if (CoolConfig.skipFramebufferCopy.get()) {
            ci.cancel();
        }
    }
}