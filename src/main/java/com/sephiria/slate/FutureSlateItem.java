package com.sephiria.slate;

import com.sephiria.artifact.ArtifactRarity;
import net.minecraft.network.chat.Component;

/**
 * 石板「未来」（稀有）：让周围的格子等级 +1（图 6 的形状——所在格正上方三格 + 左侧一格），
 * 按 R 旋转会把这四格一起转，所以它对不同的邻格生效。
 */
public class FutureSlateItem extends SlateItem {
	/** 影响范围（相对石板的 dx, dy；y 向下为正）：正上方三格 + 左侧一格。 */
	private static final int[][] PATTERN = { { 0, -1 }, { -1, -1 }, { 1, -1 }, { -1, 0 } };
	/** 每格的增量。 */
	private static final int DELTA = 1;

	public FutureSlateItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.slate_of_future.flavor";
	}

	@Override
	public java.util.List<Component> effectLines() {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.slate.effect.level",
						Component.literal("+" + DELTA).withColor(0xFF55FF55)),
				Component.translatable("artifact.sephiria.slate.effect.rotate"));
	}

	@Override
	public void apply(int[] levels, int slot, int width, int rotation) {
		int x = slot % width;
		int y = slot / width;

		for (int[] offset : PATTERN) {
			int[] turned = rotateOffset(offset[0], offset[1], rotation);
			int targetX = x + turned[0];
			int targetY = y + turned[1];

			if (targetX < 0 || targetX >= width || targetY < 0) {
				continue;
			}

			int target = targetX + targetY * width;

			if (target >= 0 && target < levels.length) {
				levels[target] += DELTA;
			}
		}
	}
}
