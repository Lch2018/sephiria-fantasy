package com.sephiria.artifact;

import com.sephiria.Sephiria;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import com.sephiria.weapon.SephiriaWeapon;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * 特别神器「书」（稀有·【唯一】·无连击）：这是一本兼容性很好的书。
 *
 * <p>它身上挂的是**原版附魔组件**（{@code minecraft:enchantments}），所以只认四种附魔书：
 * 火焰附加、抢夺、横扫之刃与效率。铁砧里「书 + 附魔书」把等级**相加**上去——但不超过这些附魔
 * 在原版里的上限（火焰附加 2、抢夺 3、横扫之刃 3、效率 5；到顶之后那一手铁砧不出结果）。见
 * {@code mixin/AnvilMenuMixin}，原版本来不认这种组合。
 *
 * <p>待在赛菲莉娅背包里时，书上的附魔对玩家产生影响：
 * 火焰附加让神器 / 连击 / <b>减益</b>的伤害点燃目标（80 刻 × 等级 = 4 秒 × 级，与原版火焰附加同款；
 * 普攻不算，见 {@link #register()}）；抢夺的等级并进掉落结算（{@code mixin/EnchantmentHelperMixin}
 * 把它加在原版「抢夺」等级之上）。横扫之刃让<b>非赛菲莉娅武器</b>的攻击冷却恢复速度按
 * 「攻击速度超过 100% 的部分 × 附魔等级/3」加快；效率让挖掘速度按「攻击速度超过 100% 的部分
 * × 附魔等级/5」加快。两者都不会在攻击速度低于 100% 时把速度拖慢。
 *
 * <p>无连击：{@link #combos()} 返回空集——神器提示框不显示连击行；许愿池的筛选里归到
 * 「无连击」那一类（见 client/wishpool/WishPoolScreen）。
 */
public class BookItem extends ArtifactItem {
	/** 火焰附加每级点燃的刻数：与原版火焰附加同款，4 秒 × 级。 */
	public static final int FIRE_TICKS_PER_LEVEL = 80;

	/** 铁砧合并一次至少收的经验等级（原版铁砧最低也是 1 级）。 */
	public static final int MIN_MERGE_COST = 1;

	/** 横扫之刃 / 效率的原版最高等级：提示框里的 x/y 用它当分母。 */
	private static final int MAX_SWEEPING_EDGE_LEVEL = 3;
	private static final int MAX_EFFICIENCY_LEVEL = 5;

	/** 非赛菲莉娅武器冷却加成的临时属性修饰符。 */
	private static final Identifier BOOK_SWEEPING_SPEED_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "book_sweeping_speed");

	/** 属性按 4 tick 对一次账：换武器 / 摘书后最多晚 4 tick 生效。 */
	private static final int COOLDOWN_CHECK_INTERVAL = 4;

	/** 词条数字的颜色：绿色（与其它神器一致）。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	private static int cooldownTicker;

	public BookItem(Item.Properties properties) {
		super(properties);
	}

	// ------------------------------------------------------------------ 神器接口

	/**
	 * 这本书不参与任何连击：{@link #combos()} 是空集，所以这个「主连击」没有意义。
	 *
	 * <p>调用方一律走 {@link #combos()}（神器提示框、连击计数、许愿池筛选都是这么读的）。
	 */
	@Override
	public ArtifactCombo combo() {
		return null;
	}

	@Override
	public List<ArtifactCombo> combos() {
		return List.of();
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.book.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		// 这件神器不走「附魔币升级」那套：它的成长就是合附魔书（maxLevel 0 = 附魔面板不给升）
		return 0;
	}

	/** 词条行：静态的机制说明；当前的附魔等级在提示框里由 {@link #enchantmentLines} 现读。 */
	@Override
	public List<Component> affixLines(int level) {
		return List.of(Component.translatable("artifact.sephiria_fantasy.affix.book_merge"));
	}

	// ------------------------------------------------------------------ 读附魔

	/** 单件物品上某个附魔的等级（0 = 没有这本附魔；只认书上认识的四种附魔，别的不读）。 */
	private static int levelOf(ItemStack stack, ResourceKey<Enchantment> wanted) {
		ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

		if (enchantments == null) {
			return 0;
		}

		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			if (entry.getKey().unwrapKey().filter(wanted::equals).isPresent()) {
				// 上限按附魔自己那张表来：万一有旧存档合过了头（当时还没封顶），
				// 读出来也按上限算——「不能超过原版的附魔等级」这条在新老数据上一致
				return Math.min(entry.getIntValue(), entry.getKey().value().getMaxLevel());
			}
		}

		return 0;
	}

	/** 这件物品是不是「书」。 */
	public static boolean isBook(ItemStack stack) {
		return stack.getItem() instanceof BookItem;
	}

	/** 背包里「书」的火焰附加等级（0 = 没有这本书，或没附这个魔）。 */
	public static int fireAspectLevel(ServerPlayer player) {
		return backpackLevel(player, Enchantments.FIRE_ASPECT);
	}

	/** 背包里「书」的抢夺等级（0 = 没有；掉落结算那一步用）。 */
	public static int lootingLevel(ServerPlayer player) {
		return backpackLevel(player, Enchantments.LOOTING);
	}

	/** 背包里「书」的横扫之刃等级（0 = 没有；非赛菲莉娅武器冷却用）。 */
	public static int sweepingEdgeLevel(ServerPlayer player) {
		return backpackLevel(player, Enchantments.SWEEPING_EDGE);
	}

	/** 背包里「书」的效率等级（0 = 没有；挖掘速度用）。 */
	public static int efficiencyLevel(ServerPlayer player) {
		return backpackLevel(player, Enchantments.EFFICIENCY);
	}

	/**
	 * 赛菲莉娅攻速给非赛菲莉娅武器冷却的恢复倍率。
	 *
	 * <p>公式取「攻击速度超过 100% 的部分 × 附魔等级 / 最高等级」，再加回 100%：
	 * 满级横扫之刃且攻速 150% 时恰好是 1.5 倍（例子里的 1.5s → 1s）；低等级按比例拿一部分，
	 * 攻速不到 100% 时倍率夹在 1.0，不会反过来减速。
	 */
	public static float cooldownRecoveryFactor(ServerPlayer player) {
		return attackSpeedScale(player, sweepingEdgeLevel(player), MAX_SWEEPING_EDGE_LEVEL);
	}

	/** 效率给挖掘速度的倍率，口径与 {@link #cooldownRecoveryFactor} 相同（满级效率 1.5s → 1s）。 */
	public static float miningSpeedFactor(ServerPlayer player) {
		return attackSpeedScale(player, efficiencyLevel(player), MAX_EFFICIENCY_LEVEL);
	}

	/** 把「攻速超过 100% 的部分」按附魔进度缩放；没有附魔 / 攻速不足 100% 时返回 1。 */
	private static float attackSpeedScale(ServerPlayer player, int level, int maxLevel) {
		if (level <= 0 || maxLevel <= 0) {
			return 1.0F;
		}

		double extra = Math.max(0.0D, PlayerStats.attackSpeedMultiplier(player) - 1.0D);
		double progress = Math.min(level, maxLevel) / (double) maxLevel;
		return (float) (1.0D + extra * progress);
	}

	/** 取背包里「书」身上某个附魔的最高等级（【唯一】：多本书也只取最高那级）。 */
	private static int backpackLevel(ServerPlayer player, ResourceKey<Enchantment> wanted) {
		int best = 0;
		ArtifactBackpack backpack = ArtifactBackpack.of(player);

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (isBook(stack)) {
				best = Math.max(best, levelOf(stack, wanted));
			}
		}

		return best;
	}

	/** 提示框里的附魔行——等级读的是这一份物品栈（不是等级表）。 */
	public static List<Component> enchantmentLines(ItemStack stack) {
		List<Component> lines = new ArrayList<>();
		int fire = levelOf(stack, Enchantments.FIRE_ASPECT);

		if (fire > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_fire_aspect",
					Component.literal(Numbers.format(fire)).withColor(COLOUR_BONUS),
					Component.literal(Numbers.format(fire * FIRE_TICKS_PER_LEVEL / 20.0D)).withColor(COLOUR_BONUS)));
		}

		int looting = levelOf(stack, Enchantments.LOOTING);

		if (looting > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_looting",
					Component.literal(Numbers.format(looting)).withColor(COLOUR_BONUS)));
		}

		int sweeping = levelOf(stack, Enchantments.SWEEPING_EDGE);

		if (sweeping > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_sweeping_edge",
					Component.literal(Numbers.format(sweeping)).withColor(COLOUR_BONUS),
					Component.literal(sweeping + "/" + MAX_SWEEPING_EDGE_LEVEL).withColor(COLOUR_BONUS)));
		}

		int efficiency = levelOf(stack, Enchantments.EFFICIENCY);

		if (efficiency > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_efficiency",
					Component.literal(Numbers.format(efficiency)).withColor(COLOUR_BONUS),
					Component.literal(efficiency + "/" + MAX_EFFICIENCY_LEVEL).withColor(COLOUR_BONUS)));
		}

		return lines;
	}

	// ------------------------------------------------------------------ 铁砧合并

	/** 一次合并的结果：合好的附魔表 + 这一手要的经验等级。 */
	public record Merge(ItemEnchantments enchantments, int cost) {
	}

	/**
	 * 第二格那本附魔书能不能合上去：至少要有一个附魔，且<b>只带</b>书上认识的四种附魔
	 * （火焰附加 / 抢夺 / 横扫之刃 / 效率）。
	 *
	 * <p>带了别的附魔就不合——这本书不认识那些附魔，合上去会凭空丢掉（原版也走不到这条路，
	 * 所以那种组合在铁砧里保持「出不来结果」）。
	 */
	public static boolean canMerge(ItemEnchantments addition) {
		if (addition.isEmpty()) {
			return false;
		}

		for (Holder<Enchantment> enchantment : addition.keySet()) {
			boolean known = enchantment.unwrapKey()
					.filter(key -> key.equals(Enchantments.FIRE_ASPECT)
							|| key.equals(Enchantments.LOOTING)
							|| key.equals(Enchantments.SWEEPING_EDGE)
							|| key.equals(Enchantments.EFFICIENCY))
					.isPresent();

			if (!known) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 合并后的附魔表：同一种附魔的等级<b>相加</b>，但**不超过这种附魔在原版里的等级上限**
	 * （火焰附加 2、抢夺 3、横扫之刃 3、效率 5——上限从附魔自己身上读，跟着数据包走，
	 * 不是写死的表）。
	 *
	 * <p>经验等级 = 这一手真正加上去的等级数（刚好到顶那一手就是 0）；最低 {@link #MIN_MERGE_COST}。
	 * 一级都加不上去时合并前后完全一样，{@code AnvilMenuMixin} 会看出这一刻不接管、让铁砧出空结果。
	 */
	public static Merge merge(ItemEnchantments current, ItemEnchantments addition) {
		ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(current);
		int cost = 0;

		for (Object2IntMap.Entry<Holder<Enchantment>> entry : addition.entrySet()) {
			Holder<Enchantment> enchantment = entry.getKey();
			int before = merged.getLevel(enchantment);
			int after = Math.min(before + entry.getIntValue(), enchantment.value().getMaxLevel());
			merged.set(enchantment, after);
			cost += after - before;
		}

		return new Merge(merged.toImmutable(), Math.max(MIN_MERGE_COST, cost));
	}

	// ------------------------------------------------------------------ 效果

	/**
	 * 神器 / 连击 / 减益打出的伤害都点燃目标（书上附了火焰附加才生效）。
	 *
	 * <p>口径（2026-10-18 用户定）：<b>减益也算、普攻不算</b>——即除了普攻（{@code Kind.WEAPON}）、
	 * 环境那类「不是玩家造成的」（{@code Kind.OTHER}）与挂在别的伤害上的附加真伤（{@code Kind.TRUE}，
	 * 它要是不排除，带无视防御伤害的普攻就等于间接点火了）之外，剩下的全点：
	 * 神器伤害、太阳剑 / 陨石 / 乌云 / 冰霜武具那几路元素攻击，以及灼伤 / 触电 / 冻伤 / 冰冻
	 * 这些减益自己的结算。
	 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			if (!ignites(source)) {
				return;
			}

			int level = fireAspectLevel(player);

			if (level <= 0) {
				return;
			}

			// 与原版火焰附加同款：4 秒 × 等级
			entity.igniteForTicks(FIRE_TICKS_PER_LEVEL * level);
		});

		// 横扫之刃给非赛菲莉娅武器加快冷却：属性修饰符要跟着“手里拿的是什么”变，
		// 所以每 4 tick 对一次账；摘书 / 换回赛菲莉娅武器时下一次对账就会撤掉。
		ServerTickEvents.END_SERVER_TICK.register(BookItem::tick);
	}

	private static void tick(MinecraftServer server) {
		if (++cooldownTicker % COOLDOWN_CHECK_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyCooldownModifier(player);
		}
	}

	/**
	 * 把「横扫之刃 + 赛菲莉娅攻速」写成 ATTACK_SPEED 的 ADD_MULTIPLIED_TOTAL 修饰符。
	 *
	 * <p>只在手持<b>非赛菲莉娅武器</b>时挂：赛菲莉娅武器本来就走 {@code WeaponStats} 的攻速，
	 * 再乘一次会双份。非赛菲莉娅武器按「有 ATTACK_SPEED 条目的物品」判断，空手 / 方块不触发。
	 */
	private static void applyCooldownModifier(ServerPlayer player) {
		AttributeInstance instance = player.getAttribute(Attributes.ATTACK_SPEED);

		if (instance == null) {
			return;
		}

		double amount = 0.0D;

		if (isNonSephiriaWeapon(player.getMainHandItem())) {
			float factor = cooldownRecoveryFactor(player);

			if (factor > 1.0F) {
				amount = factor - 1.0D;
			}
		}

		AttributeModifier current = instance.getModifier(BOOK_SWEEPING_SPEED_ID);

		if (amount == 0.0D) {
			if (current != null) {
				instance.removeModifier(BOOK_SWEEPING_SPEED_ID);
			}

			return;
		}

		if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(BOOK_SWEEPING_SPEED_ID, amount,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	/** 非赛菲莉娅武器：不是本模组武器，但物品自己的属性里有攻击速度条目。 */
	private static boolean isNonSephiriaWeapon(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() instanceof SephiriaWeapon) {
			return false;
		}

		ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
				ItemAttributeModifiers.EMPTY);

		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (entry.attribute().is(Attributes.ATTACK_SPEED)) {
				return true;
			}
		}

		return false;
	}

	/** 这一发伤害算不算「神器 / 连击 / 减益的伤害」（见 {@link #register()}）。 */
	private static boolean ignites(DamageSource source) {
		SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);

		// 普攻（武器伤害）不算：那一条是原版火焰附加的事，在这里也算就会双份
		if (kind == SephiriaDamage.Kind.WEAPON || kind == SephiriaDamage.Kind.OTHER) {
			return false;
		}

		// 无视防御伤害是挂在一次攻击上的附加一跳（普攻那一下也会带），排除掉才算真的「普攻不算」
		return kind != SephiriaDamage.Kind.TRUE;
	}
}
