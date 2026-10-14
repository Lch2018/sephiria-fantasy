package com.sephiria.client.particle;

import com.sephiria.registry.ModParticleTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * 冰霜武具（驱邪剑鞘）的剑气：一道冰蓝色的<b>弧</b>。
 *
 * <p>渲染机制整套沿用太阳剑的剑痕（见 {@link SunSlashParticle}）：朝摄像机的一张方片、
 * 全程满亮、走 TRANSLUCENT 层好淡出，贴图是弧线（textures/particle/frost_qi_0.png，
 * 由 {@code tools/gen-frost-qi-texture.ps1} 画的白芯 → 淡蓝 → 深蓝月牙），寿命 8 刻。
 *
 * <p><b>唯一的区别是朝向</b>（2026-10-13 用户改的口径：「现在是朝向上方，应该朝向前方」）：
 * 方片不再是「整张朝摄像机」，而是摆成一面<b>垂直于飞行方向</b>的刀墙——法线 = 飞行方向、
 * 贴图的「上」取世界竖直方向在该面内的投影（所以横着飞出去时，这张弧是竖着的墙，
 * 从正后方看和以前一样是一道横弧，从侧面/第三人称看不再是一张「朝上的板子」）。
 * 做法是覆盖 {@link #extract}、自己算一个世界空间的四元数：
 * {@code rotateY(yaw).rotateX(-pitch)} 正好把局部 +Z 转到飞行方向、局部 +Y 留在「尽量朝上」
 * （这两步都是局部旋转，yaw = atan2(dir.x, dir.z)、pitch = atan2(dir.y, 水平分量)）。
 * 面内的倾角照旧走 {@code roll}（父类在 extract 里做 rotateZ），同一发的多道因此不会完全重合。
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
	 * 「这个特效不是一直朝向玩家的」，之前那两版（整张朝摄像机 / 垂直于飞行方向）从正后方看
	 * 观感都一样、像是永远朝着玩家，所以现在改成<b>面里包含飞行方向</b>：
	 *
	 * <pre>
	 *     局部 +Y（贴图的「上」= 弧的鼓包）→ 飞行方向
	 *     局部 +X（贴图右）                → 飞行方向 × 世界竖直（水平法线）
	 *     局部 +Z（面法线）                → 朝上
	 * </pre>
	 *
	 * 于是这张面是<b>水平</b>的：从上往下看，弧的鼓包正好指着飞出去的方向、两只角朝后张开，
	 * 转个角度就看不见了（正后方直视时会收成一条线——任何「包含飞行方向」的面都这样，
	 * 这正是「不是一直朝向玩家」的代价，用户要的就是这个）。
	 *
	 * <p>面法线取<b>朝上</b>那一侧：26.3 的粒子管线背面剔除，法线朝下的话从上方（第三人称）
	 * 看过去就是背面、整道弧会被剔掉。
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

	/** 自己摆朝向（父类这里固定用「朝摄像机」的那套，我们换成按飞行方向算好的四元数）。 */
	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		Quaternionf quaternion = new Quaternionf(this.orientation);

		if (this.roll != 0.0F) {
			// roll 作用在局部 Z（= 面法线 = 飞行方向）上：那张弧自己在面内转，不改变这张面的朝向
			quaternion.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
		}

		this.extractRotatedQuad(state, camera, quaternion, partialTick);
	}

	/**
	 * 粒子工厂：由 {@code ParticleResourcesMixin} 在注册时挂到 sephiria_fantasy:frost_qi 与
	 * sephiria_fantasy:flame_qi 上（冰蓝与焰黄两版的参数形状一样，见
	 * {@link ModParticleTypes.QiOptions}，所以共用这一个工厂——颜色画在各自的贴图里）。
	 */
	public record Provider(SpriteSet sprites) implements ParticleProvider<ModParticleTypes.QiOptions> {
		@Override
		public Particle createParticle(ModParticleTypes.QiOptions options, ClientLevel level,
				double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
			return new FrostQiParticle(level, x, y, z, options.size(), options.roll(),
					new Vec3(options.dirX(), options.dirY(), options.dirZ()), this.sprites);
		}
	}
}
