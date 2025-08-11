package net.carbonmc.graphene.mixin.MOFRP;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void graphene$subStepMove(net.minecraft.world.entity.MoverType type, Vec3 delta, CallbackInfo ci) {
        if (!CoolConfig.ENABLE_SUBSTEP.get()) return;

        double len = delta.length();
        if (len < 0.25) return;
        int steps = (int) Math.ceil(len * 4);
        Vec3 step = delta.scale(1.0 / steps);
        Entity self = (Entity) (Object) this;
        for (int i = 0; i < steps; i++) {
            self.move(type, step);
        }
        ci.cancel();
    }
}