package com.sephiria.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sephiria.registry.ModParticleTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 冰霜武具（驱邪剑鞘）的剑气：一道冰蓝色的<b>弧</b>。
 *
 * <p>渲染机制沿用太阳剑的剑痕（见 {@link SunSlashParticle}）：一张方片、全程满亮、
 * 走半透明层好淡出，贴图是弧线（textures/particle/frost_qi_0.png，
 * 由 {@code tools/gen-frost-qi-texture.ps1} 画的白芯 → 淡蓝 → 深蓝月牙），寿命 8 刻。
 *
 * <p><b>唯一的区别是朝向</b>（2026-10-13 用户改的口径：「现在是朝向上方，应该朝向前方」）：
 * 方片不再是「整张朝摄像机」，而是摆成一面<b>包含飞行方向</b>的面——局部 +Y（贴图的「上」=
 * 弧的鼓包）对齐飞行方向、局部 +Z（面法线）朝上。于是从上往下看，弧的鼓包指着飞出去的方向、
 * 两只角朝后张开。
 *
 * <p>1.20.1 的粒子可以自己写 {@link #render}，所以这里按 26.x 的 {@code orientationOf} 算好
 * 世界空间四元数，再把四个角手动旋转、平移后发给 {@link VertexConsumer}——
 * 顶点与 UV 的对应关系照抄原版 {@code SingleQuadParticle.render}，贴图不会翻。
 */
public class FrostQiParticle extends SunSlashParticle {
	/** 寿命（刻）：0.4 秒——比太阳剑那道直痕（6 刻）长一点。 */
	private static final int LIFETIME = 8;

	/** 这张方片的朝向（世界空间，构造时按飞行方向算好，粒子自己不动所以不用每刻更新）。 */
	private final Quaternionf orientation;

	public FrostQiParticle(ClientLevel level, double x, double y, double z,
			float size, float roll, Vec3 direction, SpriteSet sprites) {
		super(level, x, y, z, size, roll, sprites);
		this.setLifetime(LIFETIME);
		this.orientation = orientationOf(direction);
	}

	/**
	 * 把这张方片摆成「<b>弧的鼓包朝飞行方向</b>」的竖切面——2026-10-13 用户的口径：
	 * 「这个特效不是一直朝向玩家的」：
	 *
	 * <pre>
	 *     局部 +Y（贴图的「上」= 弧的鼓包）→ 飞行方向
	 *     局部 +X（贴图右）                → 飞行方向 × 世界竖直（水平法线）
	 *     局部 +Z（面法线）                → 朝上
	 * </pre>
	 *
	 * <p>飞行方向是竖直的（抬头/低头直着挥）时「飞行方向 × 向上」退化：横轴退回世界 X，
	 * 面照样水平、鼓包照样朝上/下飞的方向。
	 */
	private static Quaternionf orientationOf(Vec3 direction) {
		Vec3 dir = direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
		Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));

		if (right.lengthSqr() < 1.0E-6D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}

		right = right.normalize();
		Vec3 normal = right.cross(dir).normalize();
		// Matrix3f 的九个参数是「列主序」：前三参 = 第 0 列 = 局部 +X 的像（已用 JOML 验算过）
		Matrix3f basis = new Matrix3f(
				(float) right.x, (float) right.y, (float) right.z,
				(float) dir.x, (float) dir.y, (float) dir.z,
				(float) normal.x, (float) normal.y, (float) normal.z);
		return new Quaternionf().setFromNormalized(basis);
	}

	/** 自己摆朝向（父类用原版的「朝摄像机」，这里换成按飞行方向算好的四元数）。 */
	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
		Vec3 cameraPos = camera.getPosition();
		float px = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
		float py = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
		float pz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());

		Quaternionf rotation = new Quaternionf(this.orientation);

		if (this.roll != 0.0F) {
			// roll 作用在局部 Z（= 面法线 = 飞行方向）上：那张弧自己在面内转，不改变这张面的朝向
			rotation.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
		}

		float half = this.getQuadSize(partialTick) * 0.5F;
		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();
		int light = this.getLightColor(partialTick);

		// 顶点顺序与 UV 对应关系照抄原版 SingleQuadParticle.render
		emit(buffer, rotation, -1.0F, -1.0F, half, px, py, pz, u1, v1, light);
		emit(buffer, rotation, -1.0F, 1.0F, half, px, py, pz, u1, v0, light);
		emit(buffer, rotation, 1.0F, 1.0F, half, px, py, pz, u0, v0, light);
		emit(buffer, rotation, 1.0F, -1.0F, half, px, py, pz, u0, v1, light);
	}

	private void emit(VertexConsumer buffer, Quaternionf rotation, float cornerX, float cornerY, float half,
			float px, float py, float pz, float u, float v, int light) {
		Vector3f corner = new Vector3f(cornerX, cornerY, 0.0F).rotate(rotation).mul(half);
		buffer.vertex(px + corner.x(), py + corner.y(), pz + corner.z())
				.uv(u, v)
				.color(this.rCol, this.gCol, this.bCol, this.alpha)
				.uv2(light)
				.endVertex();
	}

	/** 冰蓝剑气的工厂：挂到 sephiria_fantasy:frost_qi 上。 */
	public static ParticleProvider<ModParticleTypes.FrostQiOptions> frostProvider(SpriteSet sprites) {
		return (options, level, x, y, z, xd, yd, zd) ->
				new FrostQiParticle(level, x, y, z, options.size(), options.roll(),
						new Vec3(options.dirX(), options.dirY(), options.dirZ()), sprites);
	}

	/** 焰黄剑气的工厂：渲染与冰蓝版完全共用，颜色画在各自的贴图里。 */
	public static ParticleProvider<ModParticleTypes.FlameQiOptions> flameProvider(SpriteSet sprites) {
		return (options, level, x, y, z, xd, yd, zd) ->
				new FrostQiParticle(level, x, y, z, options.size(), options.roll(),
						new Vec3(options.dirX(), options.dirY(), options.dirZ()), sprites);
	}
}
