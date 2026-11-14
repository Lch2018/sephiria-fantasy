package com.sephiria.client.particle;

import com.sephiria.registry.ModParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

/**
 * 太阳剑的剑痕：一道<b>笔直</b>的发光划痕，行为照原版「横扫之刃」的剑痕
 * （{@code AttackSweepParticle}）那一套写——同一张朝向摄像机的方片、同样全程满亮、
 * 同样的短寿命，区别只在贴图：原版是弧形，这里是直形（textures/particle/sun_slash_0.png）。
 *
 * <p>与原版的一处不同是渲染层：原版用不透明层（没有透明混合，贴图只有「画/不画」
 * 两态，所以它一帧到位、直接消失）；剑痕要淡出，所以走半透明层，
 * 由 {@link #tick()} 每刻改 alpha。贴图也因此带一圈柔和的外发光。
 *
 * <p>长度（方片边长）与<b>倾角</b>由服务端随粒子包给（{@link ModParticleTypes.SunSlashOptions}）：
 * 火红的夕阳的「增大」那一发更长，倾角是那一刀的三个方向之一（3→9 点、3→7 点、5→11 点，见 SunSword）。
 * 1.20.1 的 {@code Particle.roll} 与 26.x 同义：屏幕面内旋转（正数 = 右端抬起 = 逆时针）。
 */
public class SunSlashParticle extends TextureSheetParticle {
	/** 寿命（刻）：0.3 秒——比原版剑痕（4 刻）长一点，够看清那道划痕。 */
	private static final int LIFETIME = 6;
	/**
	 * 从第几刻开始淡出：第一刻全亮（那道剑痕要「瞬间出现」），之后每刻掉 0.2，
	 * 最后一刻剩 0.2 —— 粒子没有「死后的那一帧」，淡不到 0 是正常的（够不显眼）。
	 */
	private static final int FADE_START = 1;

	public SunSlashParticle(ClientLevel level, double x, double y, double z,
			float size, float roll, SpriteSet sprites) {
		super(level, x, y, z);
		this.setSprite(sprites.get(0, 1));
		this.setLifetime(LIFETIME);
		this.quadSize = size;
		this.hasPhysics = false;
		this.setAlpha(1.0F);
		// 倾角全程不变：roll 与 oRoll 都设成同一个值，插值出来的就是固定角度
		this.roll = roll;
		this.oRoll = roll;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	/** 剑痕是「光」：全程满亮，不跟着环境光变暗（原版剑痕同样写死满亮）。 */
	@Override
	protected int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	public void tick() {
		// 钉在被划中的那一处：不吃速度、不吃物理，只推寿命与淡出
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ >= this.lifetime) {
			this.remove();
			return;
		}

		float progress = (float) (this.age - FADE_START) / (float) (this.lifetime - FADE_START);
		this.setAlpha(1.0F - Mth.clamp(progress, 0.0F, 1.0F));
	}

	/** 太阳剑剑痕的工厂：贴图与参数形状和冰蓝版（ice_slash）一样。 */
	public static ParticleProvider<ModParticleTypes.SunSlashOptions> sunProvider(SpriteSet sprites) {
		return (options, level, x, y, z, xd, yd, zd) ->
				new SunSlashParticle(level, x, y, z, options.size(), options.roll(), sprites);
	}

	/** 冰蓝剑痕的工厂：与太阳剑共用渲染，只有粒子类型决定的贴图不同。 */
	public static ParticleProvider<ModParticleTypes.IceSlashOptions> iceProvider(SpriteSet sprites) {
		return (options, level, x, y, z, xd, yd, zd) ->
				new SunSlashParticle(level, x, y, z, options.size(), options.roll(), sprites);
	}
}
