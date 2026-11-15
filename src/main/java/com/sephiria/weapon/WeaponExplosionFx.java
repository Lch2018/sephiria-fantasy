package com.sephiria.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

/**
 * 武器技能的 TNT 爆炸观感：只放原版 TNT 爆炸的<b>部分粒子</b>，不造成爆炸伤害、不破坏方块、不放爆炸音效。
 *
 * <p>用户 2026-11-15 要求：特效只用 TNT 爆炸的一部分，不要整段大爆炸动画；范围要铺满整个伤害盒；
 * 浓度降到原来的 30% 左右。所以这里：
 * <ul>
 *   <li>不再发 {@code EXPLOSION_EMITTER}（那一个粒子就是整段 TNT 爆炸动画）；</li>
 *   <li>每个爆炸点只留 1 个 {@code EXPLOSION} 白爆点 + 1 个 {@code SMOKE} 烟
 *       （旧版是 1 个白爆点 + 4 个烟，总粒子数约为原来的三分之一）；</li>
 *   <li>把伤害盒切成网格做分层抖动采样，每个爆炸点落在自己的小格里，
 *       从一端到另一端都有粒子，而不是全挤在中心。</li>
 * </ul>
 */
public final class WeaponExplosionFx {
	/** 一次技能最多铺几个爆炸点，避免长路径刷屏。 */
	private static final int MAX_BLOOMS = 8;
	/** 旧版每 12 平方格一个爆炸点；保留这个密度决定“铺点位置”的数量。 */
	private static final double BLOOM_AREA = 12.0D;

	private WeaponExplosionFx() {
	}

	/** 在一整块伤害盒里分层铺爆炸：调用方传进来的就是真正参与伤害判定的 AABB。 */
	public static void box(ServerLevel level, AABB area) {
		RandomSource random = level.getRandom();
		int blooms = Mth.clamp((int) Math.ceil(
				(area.maxX - area.minX) * (area.maxZ - area.minZ) / BLOOM_AREA), 2, MAX_BLOOMS);
		int columns = Mth.ceil(Math.sqrt(blooms));
		int rows = Mth.ceil((double) blooms / columns);

		for (int index = 0; index < blooms; index++) {
			int column = index % columns;
			int row = index / columns;
			// 分层抖动：x/z 按网格分格、y 按爆炸点序号分层，每点落在自己的格子里。
			// 这样粒子会从伤害盒的一端铺到另一端，不会随机挤成一团。
			double x = Mth.lerp((column + random.nextDouble()) / columns, area.minX, area.maxX);
			double z = Mth.lerp((row + random.nextDouble()) / rows, area.minZ, area.maxZ);
			double y = Mth.lerp((index + random.nextDouble()) / blooms, area.minY, area.maxY);

			bloom(level, x, y, z);
		}
	}

	/** 一朵“部分 TNT 爆炸”：白爆点 + 烟。没有大爆炸贴图、没有音效。 */
	private static void bloom(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.25D, 0.25D, 0.25D, 0.01D);
	}
}
