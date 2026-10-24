package com.sephiria.slate;

import com.sephiria.artifact.ArtifactRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 「天国」：极其特殊、极其稀有的石板（用户 2026-10-21 定）。
 *
 * <p>效果是全局的：背包里所有格子的等级 +9，不按图案、也不参与旋转。
 *
 * <p>表面品质是永恒（粉），但它<b>不进任何掉落池 / 商店 / 许愿池</b>——只能烧炼远古残骸时以
 * 0.01% 的概率从熔炉里弹出（见 {@code FurnaceHeavenSlateMixin}）。创造页里留了一件，方便测试。
 */
public class HeavenSlateItem extends SlateItem {
	/** 全局加成：所有格子等级 +9（用户 2026-10-21 定的数值）。 */
	public static final int BONUS = 9;

	private static final int COLOUR_POSITIVE = 0xFF55FF55;
	private static final int COLOUR_NEGATIVE = 0xFFFF5555;

	public HeavenSlateItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ETERNAL;
	}

	@Override
	public String flavorKey() {
		// 用户没给风味文案：对应的 lang 值是空字符串，提示框就不会画那一行
		return "artifact.sephiria_fantasy.slate_of_heaven.flavor";
	}

	@Override
	public boolean rotatable(ItemStack stack) {
		return false;
	}

	@Override
	public List<Component> effectLines(ItemStack stack) {
		return List.of(
				Component.translatable("artifact.sephiria_fantasy.slate.effect.all",
						Component.literal("+" + BONUS).withColor(COLOUR_POSITIVE)),
				Component.translatable("artifact.sephiria_fantasy.slate.tag.boost").withColor(COLOUR_POSITIVE),
				Component.translatable("artifact.sephiria_fantasy.slate.effect.fixed").withColor(COLOUR_NEGATIVE));
	}

	@Override
	public void apply(int[] levels, int slot, int width, int rotation, ItemStack stack) {
		for (int i = 0; i < levels.length; i++) {
			levels[i] += BONUS;
		}
	}
}
