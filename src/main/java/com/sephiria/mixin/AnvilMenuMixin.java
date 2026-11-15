package com.sephiria.mixin;

import com.sephiria.artifact.BookItem;
import com.sephiria.weapon.SephiriaWeapon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧上的「书 + 附魔书」：原版不认这种组合（这本「书」不在任何附魔的可用物品标签里，
 * 所以原版连火焰附加都合不上去），这里在 {@code createResult} 的 HEAD 接管：
 * 只带火焰附加 / 抢夺 / 横扫之刃 / 效率的附魔书能合上去、等级相加。
 *
 * <p>没命中的组合（别的物品、带别的附魔的书）整个方法照原版走，一点不受影响。
 * 代价照「这一手加进去的等级数」收（最低 1 级），40 级封顶那段也照抄原版——
 * {@code createResult} 结尾那条「成本 ≥ 40 且不是创造模式就不给结果」在下面复刻，
 * 免得出现一个拿不走的幽灵结果。
 *
 * <p>原版在 createResult 结尾还会 {@code broadcastChanges()} 一下，这里不跟：
 * 服务端每刻都会替玩家开着的菜单广播变更（{@code ServerPlayer} 的 tick 里），
 * 结果与成本最多晚一刻同步到客户端，肉眼看不出来；而且那个方法是**继承来的**，
 * 在这个 mixin 里 @Shadow 不到（Mixin 只在目标类自己身上找成员）。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	/** 与原版同款的经验等级封顶：到 40 级（非创造模式）原版直接不出结果。 */
	private static final int MAX_COST = 40;

	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private boolean onlyRenaming;

	@Shadow
	private int repairItemCountCost;

	/**
	 * 武器不能再附魔原版附魔书（用户 2026-11-15）：任意一格是赛菲莉娅武器、另一格是原版附魔书时，
	 * 结果直接清空、费用归零——两格摆放本身还允许（不会吞物品），只是合不出东西。
	 *
	 * <p>放在 HEAD 且不区分创造模式：这是玩法限制，不是经验成本问题。
	 * 本模组自己的「书」（{@link BookItem}）不是原版附魔书，走下面那条合并逻辑，不受影响。
	 */
	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void sephiria$blockVanillaEnchantedBook(CallbackInfo info) {
		ItemCombinerMenuAccessor access = (ItemCombinerMenuAccessor) (Object) this;
		ItemStack left = access.sephiria$inputSlots().getItem(0);
		ItemStack right = access.sephiria$inputSlots().getItem(1);

		boolean weapon = left.getItem() instanceof SephiriaWeapon || right.getItem() instanceof SephiriaWeapon;
		boolean book = left.is(Items.ENCHANTED_BOOK) || right.is(Items.ENCHANTED_BOOK);

		if (!weapon || !book) {
			return;
		}

		// 和原版「无结果」的状态保持一致：不改名、不消耗第二格、费用清零
		this.onlyRenaming = false;
		this.repairItemCountCost = 0;
		this.cost.set(0);
		access.sephiria$resultSlots().setItem(0, ItemStack.EMPTY);
		info.cancel();
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void sephiria$mergeBookEnchantments(CallbackInfo info) {
		// 两个容器与开菜单的玩家都声明在 ItemCombinerMenu 上（这里 @Shadow 不到），走访问器
		ItemCombinerMenuAccessor access = (ItemCombinerMenuAccessor) (Object) this;
		ItemStack book = access.sephiria$inputSlots().getItem(0);

		if (!BookItem.isBook(book)) {
			return;
		}

		// 附魔书的附魔存在 stored_enchantments 里（书上那份在 enchantments 里，两回事）
		ItemEnchantments addition = access.sephiria$inputSlots().getItem(1)
				.get(DataComponents.STORED_ENCHANTMENTS);

		if (addition == null || !BookItem.canMerge(addition)) {
			return;
		}

		BookItem.Merge merge = BookItem.merge(book.getEnchantments(), addition);

		// 已经到原版上限了（这一手一级也加不上去）：不接管，让原版走——铁砧出空结果，
		// 玩家看到的是「这一手没意义」，而不是白扣经验
		if (merge.enchantments().equals(book.getEnchantments())) {
			return;
		}

		ItemStack result = book.copy();
		EnchantmentHelper.setEnchantments(result, merge.enchantments());

		// 这是真合并：第二格那本书会被拿走（onlyRenaming 是「只改名、不动第二格」那条原版规则）
		this.onlyRenaming = false;
		this.repairItemCountCost = 0;
		this.cost.set(merge.cost());
		access.sephiria$resultSlots().setItem(0,
				merge.cost() >= MAX_COST && !access.sephiria$player().hasInfiniteMaterials()
						? ItemStack.EMPTY
						: result);
		info.cancel();
	}
}
