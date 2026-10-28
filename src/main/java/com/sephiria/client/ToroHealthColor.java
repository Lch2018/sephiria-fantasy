package com.sephiria.client;

import com.sephiria.weapon.SephiriaWeapon;
import com.sephiria.weapon.WeaponBranch;
import net.minecraft.world.item.ItemStack;

/**
 * 给 ToroHealth 伤害数字用的简单染色：本模组武器命中后 150ms 内的数字按武器分支着色。
 *
 * <p>1.5.1 的粒子只拿得到伤害数值，没有伤害来源；要按元素/暴击精确着色需要整套服务端
 * 伤害元数据包，这里先做「至少能区分是本模组武器打出的数字」的轻量版。
 */
public final class ToroHealthColor {
	private static int color = -1;
	private static long until;

	private ToroHealthColor() {
	}

	public static void mark(ItemStack stack) {
		if (!(stack.getItem() instanceof SephiriaWeapon weapon)) {
			return;
		}

		WeaponBranch branch = weapon.branch();
		color = switch (branch) {
			case SWORD_AND_SHIELD -> 0xFFE8D8A0;
			case GREATSWORD -> 0xFFFF9A3C;
			case DAGGER -> 0xFFB98CFF;
			case CROSSBOW -> 0xFF9CE8FF;
			case KATANA -> 0xFFFF6B6B;
			case STAFF -> 0xFF9CFF9C;
		};
		until = System.currentTimeMillis() + 150L;
	}

	public static int color() {
		return System.currentTimeMillis() <= until ? color : -1;
	}
}
