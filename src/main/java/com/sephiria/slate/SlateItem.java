package com.sephiria.slate;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.BackpackStorable;
import com.sephiria.artifact.Quality;
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
 * 石板的基类：放进赛菲莉娅背包后，影响<b>周围格子</b>的格子等级（可以加也可以减）。
 *
 * <p>每种石板有自己的影响范围（相对坐标表）与数值；石板之间互相影响，最终格子等级 = 基础等级 +
 * 所有石板效果之和，可以为负——神器放在负数等级的格子上就会失效。
 *
 * <p>鼠标放在石板上按 R 可以旋转（默认逆时针，每次 90°），影响范围会跟着转。
 * 朝向存在物品的数据组件里。
 */
public abstract class SlateItem extends Item implements BackpackStorable, Quality {
	public static final DataComponentType<Integer> ROTATION = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slate_rotation"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** 四个朝向（0 = 默认，每次 +1 是逆时针 90°）。 */
	public static final int ROTATIONS = 4;

	/**
	 * 刻印进度（0..100）：只在客户端长按时临时写在物品副本上，靠原版耐久条画出来。
	 *
	 * <p>不参与服务端结算（服务端只认 {@code SlateImprintPayload} 与格子里的真物品），
	 * 所以同步组件只是为了让同一份 ItemStack 在客户端各处画法一致。
	 */
	public static final DataComponentType<Integer> IMPRINT = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slate_imprint_progress"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** 刻印进度条的颜色：天赋「生存」的颜色 #D49ED8。 */
	private static final int IMPRINT_COLOUR = 0xFFD49ED8;

	protected SlateItem(Properties properties) {
		super(properties);
	}

	/** 长按刻印时显示耐久条；平时不显示。 */
	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.getOrDefault(IMPRINT, 0) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int progress = Mth.clamp(stack.getOrDefault(IMPRINT, 0), 0, 100);
		return Math.round(13.0F * progress / 100.0F);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return IMPRINT_COLOUR;
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
