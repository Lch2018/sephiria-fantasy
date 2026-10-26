package com.sephiria.compat.torohealth.mixin;

import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import com.sephiria.compat.torohealth.ToroHealthCritAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 给 ToroHealth 的伤害数字粒子挂上本模组的显示信息。
 *
 * <p>文本会被加上一段零宽标签（暴击、副色、税前数字），由
 * {@code TextParticleRendererMixin} 剥掉并自己按上下半颜色 / 大小 / 亮度绘制。
 *
 * <p>目标类来自可选 mod，所以整体走独立、{@code required:false} 的 mixin 配置。
 */
@Mixin(targets = "net.kairost.torohealth.client.particle.HealthChangeParticle")
public abstract class HealthChangeParticleMixin implements ToroHealthCritAccess {
	@Unique
	private boolean sephiria$custom;
	@Unique
	private boolean sephiria$crit;
	@Unique
	private int sephiria$secondaryColor = -1;
	@Unique
	private int sephiria$displayValue = -1;

	@Override
	public void sephiria$setCustom(boolean custom) {
		this.sephiria$custom = custom;
	}

	@Override
	public boolean sephiria$isCustom() {
		return this.sephiria$custom;
	}

	@Override
	public void sephiria$setCrit(boolean crit) {
		this.sephiria$crit = crit;
	}

	@Override
	public boolean sephiria$isCrit() {
		return this.sephiria$crit;
	}

	@Override
	public void sephiria$setSecondaryColor(int color) {
		this.sephiria$secondaryColor = color;
	}

	@Override
	public int sephiria$secondaryColor() {
		return this.sephiria$secondaryColor;
	}

	@Override
	public void sephiria$setDisplayValue(int value) {
		this.sephiria$displayValue = value;
	}

	@Override
	public int sephiria$displayValue() {
		return this.sephiria$displayValue;
	}

	/**
	 * {@code String text = Integer.toString(this.value);} 是方法里唯一的 String 局部变量，
	 * 所以 ordinal 0 就是它。
	 */
	@ModifyVariable(method = "extract", at = @At("STORE"), ordinal = 0)
	private String sephiria$mark(String text) {
		if (!this.sephiria$custom) {
			return text;
		}

		String plain = this.sephiria$displayValue >= 0
				? Integer.toString(this.sephiria$displayValue)
				: text;
		return ToroHealthCompatClient.tag(this.sephiria$secondaryColor, this.sephiria$crit) + plain;
	}
}
