package com.sephiria.slate;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 石板合成的规则（服务端结算与客户端预览共用同一份，免得两边算出来不一样）。
 *
 * <p>规则（用户 2026-10-24 的口径）：
 * <ul>
 *   <li>两块输入石板各自的朝向先应用（按 R 逆时针转过的角度），得到它们在「以自己为中心」的
 *       相对坐标下的效果；</li>
 *   <li>同一相对格子的增量<b>相加</b>（+1 与 +1 变 +2，+1 与 -1 抵消），抵消成 0 的格子直接丢掉；</li>
 *   <li>结果是新石板的图案，存进 {@link FusedSlateItem#PATTERN}，之后放进背包就按这张表生效。</li>
 * </ul>
 *
 * <p>只接受<b>普通石板</b>：天国是全局效果、石板基础是天赋给的保护道具，两者都不参与合成；
 * 已经合成过的「合成石板」也不能再当输入（用户 2026-10-24 定）。朝向另外有一条：
 * 两块输入里只要有一块不可旋转，结果也不可旋转（见 {@link #resultRotatable}）。
 */
public final class SlateFusion {
	/** 方块偏移；拿来当 Map 的键（坐标都很小，用 record 比拼 long 清楚）。 */
	private record Offset(int dx, int dy) {
	}

	private SlateFusion() {
	}

	/**
	 * 这块物品能不能放进合成输入格。
	 *
	 * <p>{@link FusedSlateItem} 也是 {@link PatternSlateItem}，必须显式排除——合成石板不能再参与合成。
	 */
	public static boolean accepts(ItemStack stack) {
		return stack.getItem() instanceof PatternSlateItem
				&& !(stack.getItem() instanceof SlateBaseItem)
				&& !(stack.getItem() instanceof FusedSlateItem);
	}

	/** 合成结果能不能按 R 旋转：两块输入都可旋转才为 true。 */
	public static boolean resultRotatable(ItemStack first, ItemStack second) {
		return first.getItem() instanceof SlateItem a && a.rotatable(first)
				&& second.getItem() instanceof SlateItem b && b.rotatable(second);
	}

	/** 已按石板当前朝向转好的图案；不是可合成的石板时返回空表。 */
	public static List<PatternSlateItem.Cell> orientedPattern(ItemStack stack) {
		if (!accepts(stack)) {
			return List.of();
		}

		PatternSlateItem slate = (PatternSlateItem) stack.getItem();
		List<PatternSlateItem.Cell> base = slate.pattern(stack);
		int rotation = SlateItem.rotationOf(stack);

		if (rotation == 0) {
			return base;
		}

		List<PatternSlateItem.Cell> turned = new ArrayList<>(base.size());

		for (PatternSlateItem.Cell cell : base) {
			int[] offset = SlateItem.rotateOffset(cell.dx(), cell.dy(), rotation);
			turned.add(new PatternSlateItem.Cell(offset[0], offset[1], cell.delta()));
		}

		return turned;
	}

	/** 两块物品（按它们现在的朝向）合并后的图案；不在输入格里的物品按空表算。 */
	public static List<PatternSlateItem.Cell> combine(ItemStack first, ItemStack second) {
		return combine(orientedPattern(first), orientedPattern(second));
	}

	/** 两张已定向的图案相加；顺序只影响返回列表的稳定排序，不影响效果。 */
	public static List<PatternSlateItem.Cell> combine(List<PatternSlateItem.Cell> first,
			List<PatternSlateItem.Cell> second) {
		Map<Offset, Integer> merged = new LinkedHashMap<>();

		for (PatternSlateItem.Cell cell : first) {
			merged.merge(new Offset(cell.dx(), cell.dy()), cell.delta(), Integer::sum);
		}

		for (PatternSlateItem.Cell cell : second) {
			merged.merge(new Offset(cell.dx(), cell.dy()), cell.delta(), Integer::sum);
		}

		List<PatternSlateItem.Cell> result = new ArrayList<>();

		for (Map.Entry<Offset, Integer> entry : merged.entrySet()) {
			if (entry.getValue() != 0) {
				result.add(new PatternSlateItem.Cell(entry.getKey().dx(), entry.getKey().dy(), entry.getValue()));
			}
		}

		// 固定顺序：组件要参与网络同步与同物品比较，同样的输入必须得到同一个列表
		result.sort(Comparator.comparingInt(PatternSlateItem.Cell::dy)
				.thenComparingInt(PatternSlateItem.Cell::dx));
		return List.copyOf(result);
	}
}
