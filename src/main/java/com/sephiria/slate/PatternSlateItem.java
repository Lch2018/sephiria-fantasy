package com.sephiria.slate;

import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用石板：影响范围就是一张「相对坐标 → 增量」的表，所以所有石板都是它的一个实例
 * （数值集中在 {@link Slates}，改平衡只动那一处）。
 *
 * <p>可旋转的石板按 R 会把整张表一起转；「入口」这类写着「不可旋转」的固定石板不参与旋转
 * （见 {@link SlateItem#rotatable()}）。
 */
public class PatternSlateItem extends SlateItem {
	/** 一格效果：相对石板的偏移（dx 右为正、dy 下为正）与增量（可以是负数）。 */
	public record Cell(int dx, int dy, int delta) {
	}

	/** 增量文字的颜色：加绿、减红。 */
	private static final int COLOUR_POSITIVE = 0xFF55FF55;
	private static final int COLOUR_NEGATIVE = 0xFFFF5555;

	private final ArtifactRarity rarity;
	private final String flavorKey;
	private final List<Cell> pattern;
	private final boolean rotatable;

	public PatternSlateItem(Properties properties, ArtifactRarity rarity, String flavorKey, List<Cell> pattern,
			boolean rotatable) {
		super(properties);
		this.rarity = rarity;
		this.flavorKey = flavorKey;
		this.pattern = pattern;
		this.rotatable = rotatable;
	}

	@Override
	public ArtifactRarity rarity() {
		return this.rarity;
	}

	@Override
	public String flavorKey() {
		return this.flavorKey;
	}

	@Override
	public boolean rotatable() {
		return this.rotatable;
	}

	/**
	 * 影响说明：把同类增量合并成一行（{@code +1（4 格）}），从大到小排。
	 *
	 * <p>末尾一行说明能不能旋转——不能转的（入口）直接写「不可旋转」，省得玩家按 R 按不出反应。
	 */
	@Override
	public List<Component> effectLines() {
		List<Component> lines = new ArrayList<>();
		Map<Integer, Integer> counts = new LinkedHashMap<>();

		for (Cell cell : this.pattern) {
			counts.merge(cell.delta(), 1, Integer::sum);
		}

		List<Integer> deltas = new ArrayList<>(counts.keySet());
		deltas.sort(java.util.Comparator.reverseOrder());

		for (int delta : deltas) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.slate.effect.cells",
					Component.literal((delta > 0 ? "+" : "") + Numbers.format(delta))
							.withColor(delta > 0 ? COLOUR_POSITIVE : COLOUR_NEGATIVE),
					Component.literal(String.valueOf(counts.get(delta))).withColor(COLOUR_POSITIVE)));
		}

		lines.add(this.rotatable
				? Component.translatable("artifact.sephiria_fantasy.slate.effect.rotate")
				: Component.translatable("artifact.sephiria_fantasy.slate.effect.fixed").withColor(COLOUR_NEGATIVE));

		return lines;
	}

	@Override
	public void apply(int[] levels, int slot, int width, int rotation) {
		int x = slot % width;
		int y = slot / width;

		for (Cell cell : this.pattern) {
			// 不可旋转的石板朝向固定是 0，这里统一走旋转函数也不会有变化
			int[] turned = rotateOffset(cell.dx(), cell.dy(), rotation);
			int targetX = x + turned[0];
			int targetY = y + turned[1];

			// 只挡左右越界：向上越界（targetY < 0）在下面的下标检查里自然会被算掉
			if (targetX < 0 || targetX >= width || targetY < 0) {
				continue;
			}

			int target = targetX + targetY * width;

			if (target >= 0 && target < levels.length) {
				levels[target] += cell.delta();
			}
		}
	}
}
