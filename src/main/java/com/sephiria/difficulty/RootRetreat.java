package com.sephiria.difficulty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.stats.CombatState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 「根之退」的难度设置：<b>每个玩家一份</b>，存在数据附件里（跟存档走，死亡不清零）。
 *
 * <p>为什么挂在玩家身上而不是方块上：用户的口径是「根之退只会影响自己，不会影响其他玩家」——
 * 而且<b>每个方块存自己的一份方案</b>（{@link RootRetreatBlockEntity}），界面上改的是方块那份草稿，
 * 点「应用」才把它拷到玩家这份上（= 生效的那份）；不点应用就还是原来的方案。
 *
 * <p>这个类同时是效果的<b>唯一</b>结算处：界面只管改等级，怎么生效都在这里，
 * 别的类不需要知道难度选项的存在。
 */
public final class RootRetreat {
	/** 存档用的数据：每项难度选项的等级（按 {@link DifficultyOption#ordinal()} 排列）。 */
	public record Saved(List<Integer> levels) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.listOf().fieldOf("levels").forGetter(Saved::levels)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 难度设置是玩家自己的偏好，死了不该清零
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "root_retreat"));

	private RootRetreat() {
	}

	/**
	 * 显式初始化入口（由 {@link Sephiria#onInitialize()} 调用）。
	 *
	 * <p>附件类型写在静态字段里，加载这个类就会注册；这个方法只是让「初始化」这件事有个明确的
	 * 调用点（与 {@code ArtifactBackpack.register()} 同一套）。
	 */
	public static void register() {
	}

	/** 某一项当前的等级（没设置过 = 0）。 */
	public static int levelOf(ServerPlayer player, DifficultyOption option) {
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			return 0;
		}

		int index = option.ordinal();
		List<Integer> levels = saved.levels();

		if (index >= levels.size()) {
			return 0;
		}

		Integer level = levels.get(index);
		return level == null ? 0 : Mth.clamp(level, 0, option.maxLevel());
	}

	/**
	 * 改某一项的等级（超范围会被夹住，返回夹住之后的实际等级）。
	 *
	 * <p>服务端调用：界面的加号 / 减号只是请求，实际数值以这里为准（客户端的显示是同步过去的）。
	 */
	public static int setLevel(ServerPlayer player, DifficultyOption option, int level) {
		int clamped = Mth.clamp(level, 0, option.maxLevel());
		List<Integer> levels = new ArrayList<>(allLevels(player));

		while (levels.size() <= option.ordinal()) {
			levels.add(0);
		}

		levels.set(option.ordinal(), clamped);
		player.setAttached(SAVED, new Saved(List.copyOf(levels)));
		return clamped;
	}

	/**
	 * 「应用」：把某一套方案（方块那份）整个拷到玩家身上——之后生效的就是它。
	 *
	 * <p>传进来的数组会逐项夹住范围（方块实体那边已经夹过一次，这里再兜一道）。
	 */
	public static void applyPlan(ServerPlayer player, int[] levels) {
		List<Integer> applied = new ArrayList<>(DifficultyOption.values().length);

		for (DifficultyOption option : DifficultyOption.values()) {
			int index = option.ordinal();
			applied.add(index < levels.length ? Mth.clamp(levels[index], 0, option.maxLevel()) : 0);
		}

		player.setAttached(SAVED, new Saved(List.copyOf(applied)));
	}

	/** 所有选项的当前等级（按枚举顺序）。 */
	public static List<Integer> allLevels(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		List<Integer> levels = new ArrayList<>(DifficultyOption.values().length);

		for (int index = 0; index < DifficultyOption.values().length; index++) {
			Integer level = saved == null || index >= saved.levels().size() ? null : saved.levels().get(index);
			levels.add(level == null ? 0 : Mth.clamp(level, 0, DifficultyOption.values()[index].maxLevel()));
		}

		return levels;
	}

	/** 每 1 层阶段提供的祝福：经验获取 +10%（用户口径）。 */
	public static final double XP_PER_STAGE_PERCENT = 10.0D;

	/**
	 * 总阶段：所有选项当前提供的阶段之和（界面里那行「第 x 阶段」）。
	 *
	 * <p>客户端不直接调它——界面读的是菜单同步过来的数据（见 {@code RootRetreatMenu}）。
	 */
	public static int stageOf(ServerPlayer player) {
		int total = 0;

		for (DifficultyOption option : DifficultyOption.values()) {
			total += option.stagesAt(levelOf(player, option));
		}

		return total;
	}

	/**
	 * 祝福：难度换来的经验获取加成（%）= 当前总阶段 × {@link #XP_PER_STAGE_PERCENT}。
	 *
	 * <p>接在 {@code PlayerStats.xpDropPercentTotal} 里，所以它和经验掉落是同一个池子
	 * （属性面板的「经验掉落」那一行会显示加成后的值），实际入账走
	 * {@code mixin/LeafExperienceMixin}——和黄金枫叶那条完全同路。
	 */
	public static double xpGainPercent(ServerPlayer player) {
		return stageOf(player) * XP_PER_STAGE_PERCENT;
	}

	// ------------------------------------------------------------------ 效果

	/**
	 * 「凶猛的爪子」：敌人打到你身上的伤害乘这个倍率（1.0 = 没有这项）。
	 *
	 * <p>三条限定（都写在用户口径里）：
	 * <ol>
	 *   <li><b>只看被打的人自己的等级</b>——「只会影响自己，不会影响其他玩家」，
	 *       所以这里判的是 {@code victim} 而不是攻击者；</li>
	 *   <li><b>只算敌人打的</b>：来源实体得是个生物、而且不是玩家（玩家之间 PVP 不吃这条，
	 *       环境伤害（摔落 / 岩浆 / 虚空）本来就没有来源实体，也不算）；</li>
	 *   <li>非玩家目标（生物之间互殴）一律不放大。</li>
	 * </ol>
	 *
	 * <p>调用点在 {@code DamageReductionMixin}——服务端伤害入口，那里才拿得到「这一下打多少」。
	 */
	public static float incomingDamageMultiplier(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof ServerPlayer player)) {
			return 1.0F;
		}

		Entity attacker = source.getEntity();

		if (!(attacker instanceof LivingEntity) || attacker instanceof Player) {
			return 1.0F;
		}

		int level = levelOf(player, DifficultyOption.FIERCE_CLAWS);

		if (level <= 0) {
			return 1.0F;
		}

		return 1.0F + (float) (DifficultyOption.FIERCE_CLAWS.effectAt(level) / 100.0D);
	}

	/**
	 * 「顽强之躯」+「防护铠甲」：<b>自己打出去的伤害</b>被削多少（1.0 = 没有这两项）。
	 *
	 * <p>两层的口径不同（都是用户给的）：
	 * <ul>
	 *   <li><b>顽强之躯</b>是「最底层的减免」——作用在伤害的最后一步，所以
	 *       <b>连真实伤害也照减</b>；</li>
	 *   <li><b>防护铠甲</b>是「敌人的防御力」——按 k = R / (100 + R) 折算成减伤，
	 *       而真实伤害绕开的正是防御，所以<b>真实伤害不吃这一层</b>。</li>
	 * </ul>
	 *
	 * <p>「敌人」= <b>非玩家的生物</b>：玩家打的（PVP）不吃这两项，生物互殴也不吃
	 * （它们是「谁的难度」都不算）。触发者掉线（{@code source} 里没有玩家）时也不削。
	 *
	 * @param trueDamage 这一下是不是「无视防御伤害」（真实伤害）：决定要不要算防护铠甲那层
	 */
	public static float outgoingDamageMultiplier(LivingEntity victim, DamageSource source, boolean trueDamage) {
		if (victim instanceof Player || !(source.getEntity() instanceof ServerPlayer attacker)) {
			return 1.0F;
		}

		double tenacious = 1.0D - DifficultyOption.TENACIOUS_BODY.effectAt(
				levelOf(attacker, DifficultyOption.TENACIOUS_BODY)) / 100.0D;
		double armor = 1.0D;

		if (!trueDamage) {
			double defence = DifficultyOption.PROTECTIVE_ARMOR.effectAt(
					levelOf(attacker, DifficultyOption.PROTECTIVE_ARMOR));
			armor = 1.0D - defence / (100.0D + defence);
		}

		return (float) (tenacious * armor);
	}

	// ------------------------------------------------------------------ 新增选项与祝福（2026-10-21）

	/** 「轻盈」祝福的激活阶段。 */
	public static final int LIGHTNESS_STAGE = 20;
	/** 「力量」祝福的激活阶段。 */
	public static final int STRENGTH_STAGE = 30;
	/** 「勇气」祝福的激活阶段。 */
	public static final int COURAGE_STAGE = 40;

	/**
	 * 「衰退」+「紧张」：玩家这次治疗实际拿到的比例（1.0 = 全额，0 = 完全不回）。
	 *
	 * <p>「衰退」按等级削 5/10/20/30/40/50%；「紧张」在战斗状态直接把比例压到 0。
	 * 结算挂在 {@code LivingEntity#heal} 上，自然回复 / 再生药水 / 瞬间治疗 / 金苹果都走这条。
	 */
	public static float healingMultiplier(ServerPlayer player) {
		double decay = DifficultyOption.DECAY.effectAt(levelOf(player, DifficultyOption.DECAY));
		double multiplier = 1.0D - decay / 100.0D;

		if (levelOf(player, DifficultyOption.TENSION) > 0 && CombatState.isInCombat(player)) {
			multiplier = 0.0D;
		}

		return (float) Math.max(0.0D, multiplier);
	}

	/**
	 * 「心之重担」：死亡时按等级概率让物品消失（主背包 36 格 + 赛菲莉娅背包）。
	 *
	 * <p>由 {@code ServerPlayer#die} 的 mixin 在 <b>HEAD</b> 调用——也就是在原版死亡掉落之前，
	 * 所以消失的东西不会掉到地上。不区分 keepInventory：即使开了死亡不掉落，这一部分也照样消失
	 * （用户 2026-10-21 的口径）。
	 *
	 * <p>概率按「每一摞物品」独立掷：20% 就是每摞各有 20% 的概率消失，期望是两成。
	 */
	public static void applyDeathBurden(ServerPlayer player) {
		double percent = DifficultyOption.HEART_BURDEN.effectAt(
				levelOf(player, DifficultyOption.HEART_BURDEN));

		if (percent <= 0.0D || player.isCreative() || player.isSpectator()) {
			return;
		}

		RandomSource random = player.getRandom();
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			if (!inventory.getItem(slot).isEmpty() && random.nextDouble() * 100.0D < percent) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(player);

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			if (!backpack.getItem(slot).isEmpty() && random.nextDouble() * 100.0D < percent) {
				backpack.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	/**
	 * 「轻盈」：最终摔落伤害乘 0.8（没激活就是 1.0）。
	 *
	 * <p>挂在 {@code LivingEntity#causeFallDamage} 的第一个参数（下落距离）上，等比缩小 20%。
	 */
	public static double lightnessFallMultiplier(ServerPlayer player) {
		return stageOf(player) >= LIGHTNESS_STAGE ? 0.8D : 1.0D;
	}
}
