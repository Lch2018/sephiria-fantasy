package com.sephiria.slate;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.BackpackStorable;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 石板的基类：放进赛菲利亚背包后，影响<b>周围格子</b>的格子等级（可以加也可以减）。
 *
 * <p>每种石板有自己的影响范围（相对坐标表）与数值；石板之间互相影响，最终格子等级 = 基础等级 +
 * 所有石板效果之和，可以为负——神器放在负数等级的格子上就会失效。
 *
 * <p>鼠标放在石板上按 R 可以旋转（默认逆时针，每次 90°），影响范围会跟着转。
 * 朝向存在物品的数据组件里。
 */
public abstract class SlateItem extends Item implements BackpackStorable {
	public static final DataComponentType<Integer> ROTATION = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slate_rotation"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** 四个朝向（0 = 默认，每次 +1 是逆时针 90°）。 */
	public static final int ROTATIONS = 4;

	protected SlateItem(Properties properties) {
		super(properties);
	}

	public static int rotationOf(ItemStack stack) {
		return Mth.clamp(stack.getOrDefault(ROTATION, 0), 0, ROTATIONS - 1);
	}

	/** 逆时针转 90°；不可旋转的石板（入口）什么都不做。 */
	public static void rotate(ItemStack stack) {
		if (stack.getItem() instanceof SlateItem slate && !slate.rotatable()) {
			return;
		}

		stack.set(ROTATION, (rotationOf(stack) + 1) % ROTATIONS);
	}

	public abstract ArtifactRarity rarity();

	public abstract String flavorKey();

	/** 能不能按 R 旋转；固定石板（入口）返回 false。 */
	public boolean rotatable() {
		return true;
	}

	/** 这个朝向下的影响范围说明（提示框用）。 */
	public abstract java.util.List<Component> effectLines();

	/**
	 * 把这块石板的效果加到 {@code levels} 上。
	 *
	 * @param levels   各格子的等级（会被就地修改）
	 * @param slot     石板所在的格子编号
	 * @param width    背包宽度（固定 6）
	 * @param rotation 朝向
	 */
	public abstract void apply(int[] levels, int slot, int width, int rotation);

	/** 名字按品质上色。 */
	@Override
	public Component getName(ItemStack stack) {
		return this.rarity().style(Component.translatable(this.getDescriptionId()).getString());
	}

	/** 把相对坐标按朝向旋转：每 +1 是逆时针 90°。 */
	protected static int[] rotateOffset(int dx, int dy, int rotation) {
		int x = dx;
		int y = dy;

		for (int i = 0; i < rotation; i++) {
			int nextX = y;
			int nextY = -x;
			x = nextX;
			y = nextY;
		}

		return new int[] { x, y };
	}
}
