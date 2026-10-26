package com.sephiria.compat.torohealth.mixin;

import com.sephiria.client.damage.DamageDisplayConfig;
import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import com.sephiria.compat.torohealth.ToroHealthTextRenderer;
import net.kairost.torohealth.ToroHealth;
import net.kairost.torohealth.data.BarState;
import net.kairost.torohealth.data.BarStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ToroHealth 左上角 HUD 的血量变化文字也按本模组的元素色绘制。
 *
 * <p>浮动数字走 {@code TextParticleRendererMixin}；HUD 文字是另一条绘制路径，直接由
 * {@code ToroHealthHud} 用 {@code GuiGraphicsExtractor#text} 画出来。这里在方法开头截住：
 * 用 {@link ToroHealthCompatClient#lastDamage} 找该目标最近一次本模组伤害，按主元素色画；
 * 双属性也统一用面板值较高那一侧的颜色。
 *
 * <p>治疗文字、非本模组伤害、以及取不到 ToroHealth 内部状态时直接放行原方法。
 */
@Mixin(targets = "net.kairost.torohealth.client.gui.ToroHealthHud")
public abstract class ToroHealthHudMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("SephiriaToroHealth");
	private static int logs;

	@Shadow
	private boolean at_left;
	@Shadow
	private boolean at_top;

	@Inject(method = "renderHealthChangeText(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/LivingEntity;II)V",
			at = @At("HEAD"), cancellable = true)
	private void sephiria$renderHealthChangeText(GuiGraphicsExtractor extractor, LivingEntity entity,
			int x, int y, CallbackInfo callback) {
		int healthChange = healthChange(entity);
		ToroHealthCompatClient.HudDamage info = ToroHealthCompatClient.lastDamage(entity.getId());

		if (healthChange != 0 && logs < 5) {
			logs++;
			LOGGER.info("[SEPHIRIA] hud entity={}, healthChange={}, info={}", entity.getId(), healthChange, info);
		}

		// 只接管伤害（HUD 里伤害是负数）；治疗继续用 ToroHealth 自己的绿色。
		if (healthChange >= 0 || info == null) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		String text = Integer.toString(Math.abs(healthChange));
		int color = ToroHealthTextRenderer.brighten(
				0xFF000000 | info.displayColor(), DamageDisplayConfig.brightness());
		int textX = x + (this.at_left ? -font.width(text) : 2);
		int textY = this.at_top ? y : y - 8;

		extractor.text(font, text, textX, textY, color, true);
		callback.cancel();
	}

	/** 按 ToroHealth 当前 HUD 配置取「最后一次 / 累计」血量变化。 */
	private static int healthChange(LivingEntity entity) {
		BarState state = ((BarStateAccessor) entity).torohealth$getBarState();

		if (state == null) {
			return 0;
		}

		return switch (ToroHealth.getConfig().hudOptions.healthChangeType) {
			case LAST -> state.healthChangeLast;
			case CUMULATIVE -> state.healthChangeCumulate;
			default -> 0;
		};
	}
}
