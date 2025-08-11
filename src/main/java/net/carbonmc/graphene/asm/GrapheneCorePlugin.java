package net.carbonmc.graphene.asm;

import net.minecraftforge.coremod.api.ASMAPI;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.function.Function;

/**
 * Core-Mod 入口：在启动期对 Projectile.class 字节码做 ASM Patch。
 * Forge 通过 META-INF/coremods.json 加载此类。
 */
public class GrapheneCorePlugin implements Function<ClassNode, ClassNode>, Opcodes {

    @Override
    public ClassNode apply(ClassNode node) {
        // 只处理 net/minecraft/world/entity/projectile/Projectile
        if (!"net/minecraft/world/entity/projectile/Projectile".equals(node.name)) {
            return node;
        }

        for (MethodNode mn : node.methods) {
            // 1) 修复 tick() 里对 lastRenderX/Y/Z 的读写
            if ("tick".equals(mn.name) && "()V".equals(mn.desc)) {
                patchTick(mn);
            }
            // 2) 修复 lerpMotion() 里的插值字段
            if ("lerpMotion".equals(mn.name) && "(DDD)V".equals(mn.desc)) {
                patchLerpMotion(mn);
            }
        }
        return node;
    }

    /* ---------- 内部工具 ---------- */

    private void patchTick(MethodNode mn) {
        for (AbstractInsnNode ain : mn.instructions) {
            // 找到 GETFIELD lastRenderX
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && fin.name.equals("lastRenderX")) {
                fin.name = "lastTrackedX";
                fin.desc = "D";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == PUTFIELD
                    && fin.name.equals("lastRenderX")) {
                fin.name = "lastTrackedX";
                fin.desc = "D";
            }
            // 同理 Y、Z
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && fin.name.equals("lastRenderY")) {
                fin.name = "lastTrackedY";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == PUTFIELD
                    && fin.name.equals("lastRenderY")) {
                fin.name = "lastTrackedY";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && fin.name.equals("lastRenderZ")) {
                fin.name = "lastTrackedZ";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == PUTFIELD
                    && fin.name.equals("lastRenderZ")) {
                fin.name = "lastTrackedZ";
            }
        }
    }

    private void patchLerpMotion(MethodNode mn) {
        for (AbstractInsnNode ain : mn.instructions) {
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && (fin.name.equals("lastRenderX")
                    || fin.name.equals("lastRenderY")
                    || fin.name.equals("lastRenderZ"))) {
                fin.name = fin.name.replace("lastRender", "lastTracked");
            }
        }
    }
}