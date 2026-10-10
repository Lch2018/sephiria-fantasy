package com.sephiria.client.artifact;

import com.sephiria.artifact.ArtifactItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.util.Numbers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器的提示框：名称（原版自己画）+ 连招 + 【唯一】+ 词条 + 稀有度 + 等级 + 背景描述。
 *
 * <p>词条数值按「实际等级」显示：物品自身等级 + 所处格子的格子等级。格子等级只有站在背包界面里
 * 才看得见，所以由界面在渲染前塞进 {@link #setSlotLevelHint}，鼠标移开时清掉。
 */
public final class ArtifactTooltip {
	/** 【唯一】标签的颜色（橙）。 */
	private static final int COLOUR_TAG = 0xFFFFAA00;
	/** 鼠标下的格子等级；-1 表示不在背包界面里（按 0 算）。 */
	private static int slotLevelHint = -1;

	private ArtifactTooltip() {
	}

	/** 背包界面渲染前设置：鼠标悬停格子的格子等级。 */
	public static void setSlotLevelHint(int slotLevel) {
		slotLevelHint = slotLevel;
	}

	public static void clearSlotLevelHint() {
		slotLevelHint = -1;
	}

	/** 神器 / 石板的提示行（调用方负责在它前面插空行）。 */
	public static List<Component> lines(ItemStack stack) {
		if (stack.getItem() instanceof com.sephiria.slate.SlateItem slate) {
			return slateLines(stack, slate);
		}

		if (stack.getItem() instanceof com.sephiria.potion.SephiriaPotionItem potion) {
			return potionLines(potion);
		}

		if (!(stack.getItem() instanceof SephiriaArtifact artifact)) {
			return List.of();
		}

		int level = Math.min(ArtifactItem.levelOf(stack) + Math.max(0, slotLevelHint), artifact.maxLevel());
		List<Component> lines = new ArrayList<>();

		// 连招（坚固）
		// 双连击神器（风车）两个连击都列出来
		for (com.sephiria.artifact.ArtifactCombo combo : artifact.combos()) {
			lines.add(Component.translatable(combo.translationKey()).withStyle(ChatFormatting.GREEN));
		}

		if (artifact.unique()) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.tag.unique").withColor(COLOUR_TAG));
		}

		lines.addAll(artifact.affixLines(level));

		// 桑德耳环的闪电攻击按「当前电元素强度」现算：客户端用同步过来的面板值补一行当前伤害
		if (artifact instanceof com.sephiria.artifact.SandeEarringsItem) {
			double current = com.sephiria.client.ClientStats.lightning()
					* com.sephiria.artifact.SandeEarringsItem.ATTACK_PERCENT / 100.0D;
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.sande_current",
					Component.literal(Numbers.format(current)).withColor(0xFF55FF55)));
		}

		lines.add(Component.translatable("artifact.sephiria_fantasy.rarity_line",
						Component.translatable(artifact.rarity().translationKey()).withColor(artifact.rarity().color()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("artifact.sephiria_fantasy.level_line", Numbers.format(level))
				.withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Component.translatable(artifact.flavorKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

		return lines;
	}

	/** 石板的提示行：效果 + 品质 + 当前朝向 + 背景描述。 */
	private static List<Component> slateLines(ItemStack stack, com.sephiria.slate.SlateItem slate) {
		List<Component> lines = new ArrayList<>();
		lines.addAll(slate.effectLines());
		lines.add(Component.translatable("artifact.sephiria_fantasy.slate.rarity_line",
						Component.translatable(slate.rarity().translationKey()).withColor(slate.rarity().color()))
				.withStyle(ChatFormatting.GRAY));

		// 不可旋转的石板没有朝向可言（永远是 0），这一行就不写了
		if (slate.rotatable()) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.slate.rotation",
							Numbers.format(com.sephiria.slate.SlateItem.rotationOf(stack)))
					.withStyle(ChatFormatting.DARK_GRAY));
		}

		lines.add(Component.translatable(slate.flavorKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		return lines;
	}

	/** 药水的提示行：效果 + 品质（药水不写风味，效果本身已经够说明问题）。 */
	private static List<Component> potionLines(com.sephiria.potion.SephiriaPotionItem potion) {
		List<Component> lines = new ArrayList<>();
		lines.add(potion.effect().describe());
		lines.add(Component.translatable("artifact.sephiria_fantasy.rarity_line",
						Component.translatable(potion.rarity().translationKey()).withColor(potion.rarity().color()))
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 供神器物品类复用的橙色标签构造。 */
	public static MutableComponent tag(String key) {
		return Component.translatable(key).withColor(COLOUR_TAG);
	}
}
