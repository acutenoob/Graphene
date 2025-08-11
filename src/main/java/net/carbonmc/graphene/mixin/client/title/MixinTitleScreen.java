package net.carbonmc.graphene.mixin.client.title;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mixin(SplashManager.class)
public class MixinTitleScreen {
    // 自定义闪烁标语列表（简体中文）
    private static final List<String> CUSTOM_SPLASHES_ZH_CN = Arrays.asList(
            "诬汉大学！",
            "这论文...",
            "羊！",
            "诬告好玩嘛？",
            "杜甫能动",
            "你好中国！",
            "umm",
            "Ahh",
            "Bot!",
            "我是彩蛋！",
            "MC百科",
            "回来吧！MCBBS！",
            "大型纪录片之诬告王",
            "多多益膳",
            "自己吓自己",
            "错字受！"
    );

    @Inject(
            method = "getSplash",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetSplash(CallbackInfoReturnable<SplashRenderer> cir) {
        // 创建自己的 Random 实例
        Random random = new Random();

        // 获取当前游戏语言
        String language = Minecraft.getInstance().options.languageCode;

        // 仅当语言为简体中文时生效
        if ("zh_cn".equals(language)) {
            // 30%几率使用自定义标语
            if (random.nextFloat() < 0.35114f) {
                String splashText = CUSTOM_SPLASHES_ZH_CN.get(random.nextInt(CUSTOM_SPLASHES_ZH_CN.size()));
                cir.setReturnValue(new SplashRenderer(splashText));
            }
        }
    }
}