package com.sephiria.artifact;

import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import com.sephiria.weapon.SephiriaWeapon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * 特别神器「书」（稀有·【唯一】·无连击）：这是一本兼容性很好的书。
 *
 * <p>它身上挂的是**原版附魔组件**（{@code minecraft:enchantments}），只认四种附魔书：
 * 火焰附加、抢夺、横扫之刃与效率，铁砧里「书 + 附魔书」把等级**相加**上去——但不超过这些附魔
 * 在原版里的上限（火焰附加 2、抢夺 3、横扫之刃 3、效率 5；到顶之后那一手铁砧不出结果）。见
 * {@code mixin/AnvilMenuMixin}，原版本来不认这种组合。
 *
 * <p>待在赛菲莉娅背包里时，书上的附魔对玩家产生影响：
 * 火焰附加让神器 / 连击 / <b>减益</b>的伤害点燃目标（80 刻 × 等级 = 4 秒 × 级，与原版火焰附加同款；
 * 普攻不算，见 {@link #register()}）；抢夺的等级并进掉落结算（{@code mixin/EnchantmentHelperMixin}
 * 把它加在原版「抢夺等级」之上——原版所有按抢夺结算的掉落物都自动跟着涨）。
 * 横扫之刃让<b>非赛菲莉娅武器</b>的攻击冷却恢复速度按「攻击速度超过 100% 的部分 × 附魔等级/3」
 * 加快；效率让挖掘速度按「攻击速度超过 100% 的部分 × 附魔等级/5」加快。两者都不会在攻击速度
 * 低于 100% 时把速度拖慢。
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

	/** 非赛菲莉娅武器冷却加成的临时属性修饰符（1.20.1 的属性修饰符按 UUID 认）。 */
	private static final UUID BOOK_SWEEPING_SPEED_ID =
			UUID.fromString("8f2f6c48-6b1e-4a3e-9f0a-3d5d1b7c9e21");

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

	/** 单件物品上某个附魔的等级（0 = 没有这本附魔；只认火焰附加 / 抢夺，别的不读）。 */
	private static int levelOf(ItemStack stack, Enchantment wanted) {
		int level = EnchantmentHelper.getEnchantments(stack).getOrDefault(wanted, 0);
		// 上限按附魔自己那张表来：万一有旧存档合过了头（当时还没封顶），
		// 读出来也按上限算——「不能超过原版的附魔等级」这条在新老数据上一致
		return Math.min(level, wanted.getMaxLevel());
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
		return backpackLevel(player, Enchantments.MOB_LOOTING);
	}

	/** 背包里「书」的横扫之刃等级（0 = 没有；非赛菲莉娅武器冷却用）。 */
	public static int sweepingEdgeLevel(ServerPlayer player) {
		return backpackLevel(player, Enchantments.SWEEPING_EDGE);
	}

	/** 背包里「书」的效率等级（0 = 没有；挖掘速度用）。 */
	public static int efficiencyLevel(ServerPlayer player) {
		return backpackLevel(player, Enchantments.BLOCK_EFFICIENCY);
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

	/** 一次合并的结果：合好的附魔表 + 这一手要的经验等级。 */
	public record Merge(Map<Enchantment, Integer> enchantments, int cost) {
	}

	/** 第二格那本附魔书能不能合上去：只带书上认识的四种附魔。 */
	public static boolean canMerge(Map<Enchantment, Integer> addition) {
		if (addition == null || addition.isEmpty()) {
			return false;
		}

		for (Enchantment enchantment : addition.keySet()) {
			if (enchantment != Enchantments.FIRE_ASPECT && enchantment != Enchantments.MOB_LOOTING
					&& enchantment != Enchantments.SWEEPING_EDGE && enchantment != Enchantments.BLOCK_EFFICIENCY) {
				return false;
			}
		}

		return true;
	}

	/** 合并后的附魔表：同种附魔等级相加，但不超过原版上限；经验 = 真正加上的等级数（最低 1）。 */
	public static Merge merge(Map<Enchantment, Integer> current, Map<Enchantment, Integer> addition) {
		Map<Enchantment, Integer> merged = new HashMap<>(current);
		int cost = 0;

		for (Map.Entry<Enchantment, Integer> entry : addition.entrySet()) {
			Enchantment enchantment = entry.getKey();
			int before = merged.getOrDefault(enchantment, 0);
			int after = Math.min(before + entry.getValue(), enchantment.getMaxLevel());
			merged.put(enchantment, after);
			cost += after - before;
		}

		return new Merge(Map.copyOf(merged), Math.max(MIN_MERGE_COST, cost));
	}

	/** 取背包里「书」身上某个附魔的最高等级（【唯一】：多本书也只取最高那级）。 */
	private static int backpackLevel(ServerPlayer player, Enchantment wanted) {
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

	/** 提示框里那两行「火焰附加 III / 抢夺 II」——等级读的是这一份物品栈（不是等级表）。 */
	public static List<Component> enchantmentLines(ItemStack stack) {
		List<Component> lines = new ArrayList<>();
		int fire = levelOf(stack, Enchantments.FIRE_ASPECT);

		if (fire > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_fire_aspect",
					Component.literal(Numbers.format(fire)).withStyle(style -> style.withColor(COLOUR_BONUS)),
					Component.literal(Numbers.format(fire * FIRE_TICKS_PER_LEVEL / 20.0D)).withStyle(style -> style.withColor(COLOUR_BONUS))));
		}

		int looting = levelOf(stack, Enchantments.MOB_LOOTING);

		if (looting > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_looting",
					Component.literal(Numbers.format(looting)).withStyle(style -> style.withColor(COLOUR_BONUS))));
		}

		int sweeping = levelOf(stack, Enchantments.SWEEPING_EDGE);

		if (sweeping > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_sweeping_edge",
					Component.literal(Numbers.format(sweeping)).withStyle(style -> style.withColor(COLOUR_BONUS)),
					Component.literal(sweeping + "/" + MAX_SWEEPING_EDGE_LEVEL)
							.withStyle(style -> style.withColor(COLOUR_BONUS))));
		}

		int efficiency = levelOf(stack, Enchantments.BLOCK_EFFICIENCY);

		if (efficiency > 0) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.affix.book_efficiency",
					Component.literal(Numbers.format(efficiency)).withStyle(style -> style.withColor(COLOUR_BONUS)),
					Component.literal(efficiency + "/" + MAX_EFFICIENCY_LEVEL)
							.withStyle(style -> style.withColor(COLOUR_BONUS))));
		}

		return lines;
	}

	// ------------------------------------------------------------------ 铁砧合并

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
		com.sephiria.compat.DamageEvents.registerAfterDamage((entity, source, blockedDamage, damageTaken, blocked) -> {
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
			entity.setSecondsOnFire(FIRE_TICKS_PER_LEVEL * level / 20);
		});

		// 横扫之刃给非赛菲莉娅武器加快冷却：属性修饰符要跟着「手里拿的是什么」变，
		// 所以每 4 tick 对一次账；摘书 / 换回赛菲莉娅武器时下一次对账就会撤掉。
		ServerTickEvents.END_SERVER_TICK.register(BookItem::tick);
	}

	private static void tick(net.minecraft.server.MinecraftServer server) {
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

		// 1.20.1 没有 addOrUpdateTransientModifier：数值变了就删掉重加
		if (current == null || current.getAmount() != amount) {
			if (current != null) {
				instance.removeModifier(BOOK_SWEEPING_SPEED_ID);
			}

			instance.addTransientModifier(new AttributeModifier(BOOK_SWEEPING_SPEED_ID,
					"sephiria_book_sweeping_speed", amount,
					AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	/** 非赛菲莉娅武器：不是本模组武器，但物品自己的属性里有攻击速度条目。 */
	private static boolean isNonSephiriaWeapon(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() instanceof SephiriaWeapon) {
			return false;
		}

		// 1.20.1 没有数据组件：直接问物品在当前槽位下会不会给出攻击速度修饰符
		return !stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
				.get(Attributes.ATTACK_SPEED).isEmpty();
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
