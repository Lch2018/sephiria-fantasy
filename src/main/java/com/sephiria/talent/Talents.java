package com.sephiria.talent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.network.TalentSyncPayload;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.Projectile;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天赋系统：天赋点、两条天赋页的加点、以及全部加成数值。
 *
 * <p>天赋点跟「四叶草」一样是<b>数值不是物品</b>：达成一个会弹 toast 的成就 +1、击杀 BOSS +1，
 * 另外<b>按等级发点</b>——玩家达到过的最高等级每 1 级给 1 点，这条来源最多 30 点
 * （见 {@link #claimLevelPoints}）。加点只改变这里存的两条天赋页点数，剩余点数 = 已获得 − 已加点。
 * 全部存在玩家附件里，{@code copyOnDeath()} 让死亡不会抹掉天赋（与叶子、属性同一套口径）。
 *
 * <p>天赋加成分两种，读取方式也分两种：
 * <ul>
 *   <li><b>战斗数值</b>（暴击、攻速、闪避、武器伤害、冲刺……）——直接由各系统在结算时现查，
 *       与神器词条走同一套「不写回存档、每 tick 对账」的路线；</li>
 *   <li><b>阶段能力</b>（BOSS 伤害、防御穿透、冲刺无敌）——只在阶段达标时生效，
 *       由 {@link #stage} 判定。</li>
 * </ul>
 *
 * <p>关于「一种天赋最多加点到 10 点」：用户 2026-10-21 已确认是笔误，**每页上限 20 点**
 * （与三阶段的 20 点门槛一致）。
 */
public final class Talents {
	/** 每条天赋页最多能加几点（用户 2026-10-21 确认：20）。 */
	public static final int MAX_POINTS = 20;
	/** 三个阶段的解锁点数：5 / 10 / 20。 */
	public static final int[] STAGE_POINTS = { 5, 10, 20 };
	/** 「升级获得」这条来源最多给几点（用户 2026-10-21 确认：30）。 */
	public static final int LEVEL_POINT_CAP = 30;

	/** 愤怒：每 1 点加多少暴击几率（%）。 */
	public static final double RAGE_CRIT_CHANCE_PER_POINT = 1.0D;
	/** 愤怒一阶段：暴击伤害 +20%。 */
	public static final double RAGE_STAGE1_CRIT_DAMAGE = 20.0D;
	/** 愤怒二阶段：对 BOSS 造成的伤害 +10%。 */
	public static final double RAGE_STAGE2_BOSS_DAMAGE = 10.0D;
	/** 愤怒三阶段：防御穿透 +20%（无视敌人 20% 的防御力）。 */
	public static final double RAGE_STAGE3_DEFENSE_PENETRATION = 20.0D;

	/** 迅捷：每 1 点加多少攻击速度（%）。 */
	public static final double SWIFT_ATTACK_SPEED_PER_POINT = 1.0D;
	/** 迅捷一阶段：闪避 +8 点。 */
	public static final double SWIFT_STAGE1_DODGE = 8.0D;
	/** 迅捷一阶段：冲刺次数 +1。 */
	public static final int SWIFT_STAGE1_DASH_CHARGES = 1;
	/** 迅捷二阶段：冲刺恢复速度 +20%。 */
	public static final double SWIFT_STAGE2_DASH_REGEN = 20.0D;
	/** 迅捷二阶段：武器伤害 +8%。 */
	public static final double SWIFT_STAGE2_WEAPON_DAMAGE = 8.0D;
	/** 迅捷三阶段：冲刺中的无敌时间 +0.1 秒（2 刻，20 TPS）。 */
	public static final int SWIFT_STAGE3_INVULNERABLE_TICKS = 2;
	/** 迅捷三阶段：武器伤害 +15%（与二阶段叠加）。 */
	public static final double SWIFT_STAGE3_WEAPON_DAMAGE = 15.0D;

	/** 意志：每 1 点加多少幸运值（点）。 */
	public static final double WILL_LUCK_PER_POINT = 1.0D;
	/** 意志一阶段：许愿池锚定格 +1。 */
	public static final int WILL_STAGE1_ANCHOR_BONUS = 1;
	/** 意志二阶段：最大 HP +4。 */
	public static final int WILL_STAGE2_MAX_HP = 4;
	/** 意志二阶段：最大 MP +25。 */
	public static final int WILL_STAGE2_MAX_MP = 25;
	/** 意志三阶段：许愿池锚定格再 +3（与一阶段叠加，默认 1 格最多到 5 格）。 */
	public static final int WILL_STAGE3_ANCHOR_BONUS = 3;

	/** 急智：每 1 点加多少最大 HP。 */
	public static final double QUICK_WIT_MAX_HP_PER_POINT = 0.3D;
	/** 急智：每 1 点加多少闪避（点）。 */
	public static final double QUICK_WIT_DODGE_PER_POINT = 0.5D;
	/** 急智一阶段：背包 +2 格。 */
	public static final int QUICK_WIT_STAGE1_BACKPACK_SLOTS = 2;
	/** 急智二阶段：宝箱选择 +1 个（5 选 1 → 6 选 1）。 */
	public static final int QUICK_WIT_STAGE2_CHEST_CHOICES = 1;
	/** 急智三阶段：每种激活的连击类型使所有伤害放大 +7%。 */
	public static final double QUICK_WIT_STAGE3_COMBO_DAMAGE = 7.0D;

	/** 生存：每点 +0.6 最大 HP。 */
	public static final double SURVIVAL_MAX_HP_PER_POINT = 0.6D;
	/** 生存二阶段：+3 格（三阶段不再扣格，用户 2026-10-24 去掉了原来的 -12）。 */
	public static final int SURVIVAL_STAGE2_BACKPACK_SLOTS = 3;

	/** 耐心：每点 +1 防御力。 */
	public static final double PATIENCE_DEFENSE_PER_POINT = 1.0D;
	/** 耐心二阶段：血量低于 7 时每秒回 0.5（用户 2026-10-23 从 1 点下调）。 */
	public static final double PATIENCE_LOW_HP_THRESHOLD = 7.0D;
	public static final double PATIENCE_LOW_HP_REGEN = 0.5D;
	/** 耐心三阶段：60% 生命复活，10 个游戏日一次（1 个游戏日 = 24000 刻）。 */
	public static final double PATIENCE_REVIVE_HEALTH_PERCENT = 60.0D;
	public static final long PATIENCE_REVIVE_COOLDOWN_TICKS = 10L * 24000L;

	/** 智慧一阶段：MP 吸收 +5、最大 MP +10；每点 +1 战斗 MP 再生（每 10 秒回 x 点）。 */
	public static final double WISDOM_STAGE1_MP_ABSORB = 5.0D;
	public static final double WISDOM_STAGE1_MAX_MP = 10.0D;
	public static final double WISDOM_MP_REGEN_PER_POINT = 1.0D;
	public static final int WISDOM_MP_REGEN_INTERVAL_TICKS = 200;
	/** 智慧二阶段：击杀 BOSS 给 1 个骰子。 */
	public static final int WISDOM_STAGE2_BOSS_DICE = 1;
	/** 智慧三阶段：经验掉落 +20%。 */
	public static final double WISDOM_STAGE3_XP_DROP = 20.0D;

	/**
	 * 已获得的天赋点 + 各天赋页的加点数 + 已经兑换成天赋点的最高等级。
	 *
	 * <p>{@code level} 记的是「最高等级里已经发过点的部分」（0..{@value #LEVEL_POINT_CAP}）：
	 * 掉级不会把它减回去，所以再升级时只补超出历史最高的那几级，不会重复发；
	 * 「重置」只清各天赋页，不动它。
	 *
	 * <p>2026-10-21 新增生存 / 耐心 / 智慧三页：三个字段都是 optional，旧存档读出来是 0；
	 * 字段名按 {@link Talent#id()} 写，以后再加页也照这个顺序往后排。
	 */
	public record Saved(int earned, int rage, int swift, int will, int quickWit,
			int survival, int patience, int wisdom, int level) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("earned", 0).forGetter(Saved::earned),
				Codec.INT.optionalFieldOf("rage", 0).forGetter(Saved::rage),
				Codec.INT.optionalFieldOf("swift", 0).forGetter(Saved::swift),
				Codec.INT.optionalFieldOf("will", 0).forGetter(Saved::will),
				Codec.INT.optionalFieldOf("quick_wit", 0).forGetter(Saved::quickWit),
				Codec.INT.optionalFieldOf("survival", 0).forGetter(Saved::survival),
				Codec.INT.optionalFieldOf("patience", 0).forGetter(Saved::patience),
				Codec.INT.optionalFieldOf("wisdom", 0).forGetter(Saved::wisdom),
				Codec.INT.optionalFieldOf("level", 0).forGetter(Saved::level)
		).apply(instance, Saved::new));

		static final Saved EMPTY = new Saved(0, 0, 0, 0, 0, 0, 0, 0, 0);

		public int points(Talent talent) {
			return switch (talent) {
				case RAGE -> this.rage;
				case SWIFT -> this.swift;
				case WILL -> this.will;
				case QUICK_WIT -> this.quickWit;
				case SURVIVAL -> this.survival;
				case PATIENCE -> this.patience;
				case WISDOM -> this.wisdom;
			};
		}

		Saved withPoints(Talent talent, int value) {
			return switch (talent) {
				case RAGE -> new Saved(this.earned, value, this.swift, this.will, this.quickWit,
						this.survival, this.patience, this.wisdom, this.level);
				case SWIFT -> new Saved(this.earned, this.rage, value, this.will, this.quickWit,
						this.survival, this.patience, this.wisdom, this.level);
				case WILL -> new Saved(this.earned, this.rage, this.swift, value, this.quickWit,
						this.survival, this.patience, this.wisdom, this.level);
				case QUICK_WIT -> new Saved(this.earned, this.rage, this.swift, this.will, value,
						this.survival, this.patience, this.wisdom, this.level);
				case SURVIVAL -> new Saved(this.earned, this.rage, this.swift, this.will, this.quickWit,
						value, this.patience, this.wisdom, this.level);
				case PATIENCE -> new Saved(this.earned, this.rage, this.swift, this.will, this.quickWit,
						this.survival, value, this.wisdom, this.level);
				case WISDOM -> new Saved(this.earned, this.rage, this.swift, this.will, this.quickWit,
						this.survival, this.patience, value, this.level);
			};
		}

		Saved withEarned(int value) {
			return new Saved(value, this.rage, this.swift, this.will, this.quickWit,
					this.survival, this.patience, this.wisdom, this.level);
		}

		Saved withLevel(int value) {
			return new Saved(this.earned, this.rage, this.swift, this.will, this.quickWit,
					this.survival, this.patience, this.wisdom, value);
		}
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 天赋点是长期成长，死一次不该丢（与叶子、属性一样）
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "talents"));

	/**
	 * 实体类 → 是否自带一条服务端 BOSS 血条。
	 *
	 * <p>用来认「其它模组的 BOSS」：只要它的类里声明了 {@link BossEvent} 字段
	 * （原版凋灵就是这种写法），就按玩家在屏幕最上方看到的血条算。这是类型级别的判断，
	 * 每个类只反射一次；原版末影龙的血条在 {@code EnderDragonFight} 里、不在实体类上，
	 * 所以那一类靠 {@code c:bosses} 标签兜住。
	 */
	private static final Map<Class<?>, Boolean> BOSS_CLASSES = new ConcurrentHashMap<>();

	private Talents() {
	}

	/** 注册天赋点来源（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		// 击杀 BOSS +1 天赋点。只认「有 BOSS 血条」的目标，且只记到真正补上最后一刀的那名玩家头上。
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!isBoss(entity)) {
				return;
			}

			ServerPlayer killer = attackerOf(source);

			if (killer != null) {
				addPoints(killer, 1);

				// 智慧二阶段：击败 BOSS 额外给 1 个骰子（与天赋点分开结算）
				int dice = bossDice(killer);

				if (dice > 0) {
					com.sephiria.util.PlayerItems.give(killer,
							new net.minecraft.world.item.ItemStack(com.sephiria.registry.ModItems.DICE, dice));
				}
			}
		});

		// 升级发点：每 tick 对一次「当前等级 > 已兑换的最高等级」，超出的部分补成天赋点。
		// 每 tick 都查是为了把「升级后立刻花经验掉级」的窗口压到最小；没变化时只是一次整数比较。
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				claimLevelPoints(player);
			}
		});
	}

	/**
	 * 把「当前等级超过历史最高等级」的那几级换成天赋点。
	 *
	 * <p>用户 2026-10-21 的口径：升到 10 级给 10 点；花经验掉到 3 级再升到 14 级，只补
	 * 10 → 14 的 4 点，3 → 10 那段不重复发。所以这里记的是<b>已经发过点的最高等级</b>
	 * （{@link Saved#level()}），掉级不减它，只有当前等级更高时才发差额。
	 *
	 * <p>这条来源总共最多 {@value #LEVEL_POINT_CAP} 点：等级先夹到 30 再比较。首次检查时
	 * 按玩家当前等级一次性补满（老存档 / 已经 30 级的玩家也能拿到该得的点）。
	 */
	public static void claimLevelPoints(ServerPlayer player) {
		int target = Math.min(LEVEL_POINT_CAP, Math.max(0, player.experienceLevel));
		Saved saved = of(player);

		if (target <= saved.level()) {
			return;
		}

		int gained = target - saved.level();
		set(player, saved.withLevel(target).withEarned(saved.earned() + gained));
	}

	/** 玩家当前存档（没加过点时是一份全零的）。 */
	public static Saved of(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? Saved.EMPTY : saved;
	}

	public static int earned(ServerPlayer player) {
		return of(player).earned();
	}

	public static int points(ServerPlayer player, Talent talent) {
		return of(player).points(talent);
	}

	/** 已经花掉的天赋点。 */
	public static int spent(ServerPlayer player) {
		Saved saved = of(player);
		return saved.rage() + saved.swift() + saved.will() + saved.quickWit()
				+ saved.survival() + saved.patience() + saved.wisdom();
	}

	/** 还没花掉的天赋点（界面上「剩余天赋点数 m」）。 */
	public static int remaining(ServerPlayer player) {
		return Math.max(0, earned(player) - spent(player));
	}

	/** 某条天赋页当前点亮到第几阶段（0..3）。 */
	public static int stage(ServerPlayer player, Talent talent) {
		return talent.stage(points(player, talent));
	}

	/** 获得天赋点（成就 / BOSS 击杀）。 */
	public static void addPoints(ServerPlayer player, int amount) {
		if (amount <= 0) {
			return;
		}

		Saved saved = of(player);
		set(player, saved.withEarned(saved.earned() + amount));
	}

	/** 天赋只能在出生点 100 格内修改（用户 2026-10-21）。 */
	public static final double SPAWN_RANGE = 100.0D;

	/**
	 * 玩家是否在出生点附近：当前维度的<b>世界出生点</b>，或玩家自己设置的出生点
	 * （床 / 重生锚 / {@code /spawnpoint}），任意一个在 100 格内都算。
	 *
	 * <p>用户 2026-10-22 报的 bug：之前只认世界生成时的出生点，玩家在床上设的重生点附近
	 * 反而不满足条件。个人出生点只在服务端有（{@code ServerPlayer#getRespawnConfig}），
	 * 所以客户端不能自己算——同步包里的 {@code nearSpawn} 由这里算好后推过去；
	 * 服务端也用它做最终判据，改包绕不过去。
	 */
	public static boolean nearSpawn(ServerPlayer player) {
		return nearSpawn(player, player.blockPosition());
	}

	/** 某个位置是否在玩家可用的出生点附近（世界出生点或个人出生点，且个人出生点要在同一维度）。 */
	public static boolean nearSpawn(ServerPlayer player, BlockPos pos) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}

		if (isNear(pos, level.getRespawnData().pos())) {
			return true;
		}

		ServerPlayer.RespawnConfig config = player.getRespawnConfig();

		if (config == null) {
			return false;
		}

		LevelData.RespawnData respawn = config.respawnData();
		// 床 / 重生锚在别的维度时不算：根之退与天赋都是「当前维度的出生点」这个口径
		return respawn.dimension().equals(level.dimension()) && isNear(pos, respawn.pos());
	}

	private static boolean isNear(BlockPos pos, BlockPos spawn) {
		return pos.distSqr(spawn) <= SPAWN_RANGE * SPAWN_RANGE;
	}

	/**
	 * 加点/退点，正数加、负数退。
	 *
	 * <p>两端都夹住：加点不超过「剩余点数」与每页上限，退点不超过这一页已加的点数——
	 * 所以 {@code -5} / {@code -1} 永远只返还真的存在的点，不会出现负加点或吞点。
	 */
	public static void adjust(ServerPlayer player, Talent talent, int delta) {
		if (!nearSpawn(player) || delta == 0) {
			return;
		}

		Saved saved = of(player);
		int points = saved.points(talent);

		if (delta > 0) {
			int allowed = Math.min(delta, Math.min(remaining(player), MAX_POINTS - points));

			if (allowed <= 0) {
				return;
			}

			set(player, saved.withPoints(talent, points + allowed));
		} else {
			int refund = Math.min(-delta, points);

			if (refund <= 0) {
				return;
			}

			set(player, saved.withPoints(talent, points - refund));
		}
	}

	/** 重置全部天赋：所有天赋页都归零，点数全部回到「剩余」里。 */
	public static void reset(ServerPlayer player) {
		if (!nearSpawn(player)) {
			return;
		}

		Saved saved = of(player);

		if (spent(player) == 0) {
			sync(player);
			return;
		}

		set(player, saved.withPoints(Talent.RAGE, 0)
				.withPoints(Talent.SWIFT, 0)
				.withPoints(Talent.WILL, 0)
				.withPoints(Talent.QUICK_WIT, 0)
				.withPoints(Talent.SURVIVAL, 0)
				.withPoints(Talent.PATIENCE, 0)
				.withPoints(Talent.WISDOM, 0));
	}

	private static void set(ServerPlayer player, Saved saved) {
		Saved before = of(player);
		player.setAttached(SAVED, saved);
		sync(player);
		afterChange(player, before, saved);
		// 意志二阶段会改最大 MP、迅捷会改冲刺上限：立刻重推属性包，
		// 不然 HUD 的蓝条上限要等下一轮「总值指纹」对账（最多 1 秒）才跟上
		com.sephiria.stats.PlayerStats.sync(player);
		// 迅捷的「冲刺次数 +1」改的是 SkillStorage 的上限，而它平时只在数值变化时推包；
		// 这里主动重推一次，HUD 的冲刺次数不会等到下一次消耗/回复才更新
		com.sephiria.ability.SkillStorage.syncAll(player);
	}

	/**
	 * 加点变化后的副作用：重算背包格子等级缓存。
	 *
	 * <p>三条新天赋页的能力都不再需要「阶段熄灭」时的清理（2026-10-24 用户定的）：
	 * 石板刻印是永久的、神器合并直接写在物品栈上、耐心一阶段的第一行等级是现算的——
	 * 所以这里只剩缓存失效（背包格数会随急智 +2 / 生存 +3 变化）。
	 */
	private static void afterChange(ServerPlayer player, Saved before, Saved after) {
		// 背包格数可能会变（急智 +2、生存 +3），格子等级缓存与 HUD 都要跟着重算
		com.sephiria.backpack.ArtifactBackpack.of(player).invalidateLevels();
	}

	/** 把天赋点与「能否修改」推给客户端（进服、每次变更与打开界面时各一次）。 */
	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, new TalentSyncPayload(earned(player),
				points(player, Talent.RAGE), points(player, Talent.SWIFT),
				points(player, Talent.WILL), points(player, Talent.QUICK_WIT),
				points(player, Talent.SURVIVAL), points(player, Talent.PATIENCE),
				points(player, Talent.WISDOM), nearSpawn(player)));
	}

	// ------------------------------------------------------------------
	// 数值：全部由各系统在结算时现查
	// ------------------------------------------------------------------

	/** 愤怒每点 +1% 暴击几率。 */
	public static double critChancePercent(ServerPlayer player) {
		return points(player, Talent.RAGE) * RAGE_CRIT_CHANCE_PER_POINT;
	}

	/** 愤怒阶段给的暴击伤害（%）：只有一阶段 +20。 */
	public static double critDamagePercent(ServerPlayer player) {
		return stage(player, Talent.RAGE) >= 1 ? RAGE_STAGE1_CRIT_DAMAGE : 0.0D;
	}

	/** 愤怒三阶段的防御穿透（%）：0 = 不穿透。 */
	public static double defensePenetrationPercent(ServerPlayer player) {
		return stage(player, Talent.RAGE) >= 3 ? RAGE_STAGE3_DEFENSE_PENETRATION : 0.0D;
	}

	/** 对 BOSS 的伤害倍率：愤怒二阶段起 ×1.10，其余 ×1.0。 */
	public static float bossDamageMultiplier(ServerPlayer player, LivingEntity target) {
		return stage(player, Talent.RAGE) >= 2 && isBoss(target)
				? (float) (1.0D + RAGE_STAGE2_BOSS_DAMAGE / 100.0D)
				: 1.0F;
	}

	/** 迅捷每点 +1% 攻击速度。 */
	public static double attackSpeedPercent(ServerPlayer player) {
		return points(player, Talent.SWIFT) * SWIFT_ATTACK_SPEED_PER_POINT;
	}

	/** 意志每点 +1 幸运值。 */
	public static double luckPoints(ServerPlayer player) {
		return points(player, Talent.WILL) * WILL_LUCK_PER_POINT;
	}

	/** 意志阶段给的许愿池锚定格加成（格）：一阶段 +1、三阶段再 +3。 */
	public static int anchorSlotBonus(ServerPlayer player) {
		int stage = stage(player, Talent.WILL);
		int bonus = 0;

		if (stage >= 1) {
			bonus += WILL_STAGE1_ANCHOR_BONUS;
		}

		if (stage >= 3) {
			bonus += WILL_STAGE3_ANCHOR_BONUS;
		}

		return bonus;
	}

	/** 最大 HP 的固定加成：意志二阶段 +4，急智每点 +0.3，生存每点 +0.6。 */
	public static double maxHpFlat(ServerPlayer player) {
		double total = points(player, Talent.QUICK_WIT) * QUICK_WIT_MAX_HP_PER_POINT
				+ points(player, Talent.SURVIVAL) * SURVIVAL_MAX_HP_PER_POINT;

		if (stage(player, Talent.WILL) >= 2) {
			total += WILL_STAGE2_MAX_HP;
		}

		return total;
	}

	/** 最大 MP 的固定加成：意志二阶段 +25，智慧一阶段 +10。 */
	public static double maxMpFlat(ServerPlayer player) {
		double total = stage(player, Talent.WILL) >= 2 ? WILL_STAGE2_MAX_MP : 0.0D;

		if (stage(player, Talent.WISDOM) >= 1) {
			total += WISDOM_STAGE1_MAX_MP;
		}

		return total;
	}

	/**
	 * 背包格子数的天赋加成：急智一阶段 +2、生存二阶段 +3、生存三阶段 -12。
	 *
	 * <p>生存三阶段的 -12 是在二阶段 +3 之上再扣（「减少了 12 格」相对二阶段说的），
	 * 净变化是 -9。
	 */
	public static int backpackSlots(ServerPlayer player) {
		int bonus = 0;

		if (stage(player, Talent.QUICK_WIT) >= 1) {
			bonus += QUICK_WIT_STAGE1_BACKPACK_SLOTS;
		}

		if (stage(player, Talent.SURVIVAL) >= 2) {
			bonus += SURVIVAL_STAGE2_BACKPACK_SLOTS;
		}

		return bonus;
	}

	/** 耐心每点 +1 防御力。 */
	public static double defensePoints(ServerPlayer player) {
		return points(player, Talent.PATIENCE) * PATIENCE_DEFENSE_PER_POINT;
	}

	/** 智慧每点 +1 战斗 MP 再生（每 10 秒回 x 点）。 */
	public static double wisdomMpRegenPoints(ServerPlayer player) {
		return points(player, Talent.WISDOM) * WISDOM_MP_REGEN_PER_POINT;
	}

	/** 智慧一阶段：MP 吸收 +5。 */
	public static double mpAbsorbBonus(ServerPlayer player) {
		return stage(player, Talent.WISDOM) >= 1 ? WISDOM_STAGE1_MP_ABSORB : 0.0D;
	}

	/** 智慧二阶段：所有神器强制按【独特】结算。 */
	public static boolean forceArtifactUnique(ServerPlayer player) {
		return stage(player, Talent.WISDOM) >= 2;
	}

	/** 智慧二阶段：击杀 BOSS 给的骰子数。 */
	public static int bossDice(ServerPlayer player) {
		return stage(player, Talent.WISDOM) >= 2 ? WISDOM_STAGE2_BOSS_DICE : 0;
	}

	/** 智慧三阶段：经验掉落加成（%）。 */
	public static double wisdomXpDropPercent(ServerPlayer player) {
		return stage(player, Talent.WISDOM) >= 3 ? WISDOM_STAGE3_XP_DROP : 0.0D;
	}

	/** 急智二阶段：宝箱选择 +1 个。 */
	public static int chestChoices(ServerPlayer player) {
		return stage(player, Talent.QUICK_WIT) >= 2 ? QUICK_WIT_STAGE2_CHEST_CHOICES : 0;
	}

	/**
	 * 急智三阶段：每种激活的连击类型使所有伤害放大 +7%。
	 *
	 * <p>「激活」= 该连击当前等级 &gt; 0（背包里的神器 + 药水「索莱拉的偶然」两者相加）；
	 * 只数<b>类型</b>，不数件数。连击面板里那些 0 级的条目不算。
	 */
	public static double comboDamagePercent(ServerPlayer player) {
		if (stage(player, Talent.QUICK_WIT) < 3) {
			return 0.0D;
		}

		int active = 0;

		for (int level : com.sephiria.artifact.ArtifactEffects.comboLevels(player).values()) {
			if (level > 0) {
				active++;
			}
		}

		return active * QUICK_WIT_STAGE3_COMBO_DAMAGE;
	}

	/** 急智三阶段的伤害倍率（1.0 = 不加成）。 */
	public static float comboDamageMultiplier(ServerPlayer player) {
		return 1.0F + (float) (comboDamagePercent(player) / 100.0D);
	}

	/** 迅捷一阶段：闪避 +8 点；急智每点再 +0.5 点。 */
	public static double dodgePoints(ServerPlayer player) {
		double total = points(player, Talent.QUICK_WIT) * QUICK_WIT_DODGE_PER_POINT;

		if (stage(player, Talent.SWIFT) >= 1) {
			total += SWIFT_STAGE1_DODGE;
		}

		return total;
	}

	/** 迅捷一阶段：冲刺次数 +1。 */
	public static int dashCharges(ServerPlayer player) {
		return stage(player, Talent.SWIFT) >= 1 ? SWIFT_STAGE1_DASH_CHARGES : 0;
	}

	/** 迅捷二阶段：冲刺恢复速度 +20%。 */
	public static double dashRegenPercent(ServerPlayer player) {
		return stage(player, Talent.SWIFT) >= 2 ? SWIFT_STAGE2_DASH_REGEN : 0.0D;
	}

	/** 迅捷阶段给的武器伤害（%）：二阶段 +8，三阶段再 +15。 */
	public static double weaponDamagePercent(ServerPlayer player) {
		int stage = stage(player, Talent.SWIFT);
		double total = 0.0D;

		if (stage >= 2) {
			total += SWIFT_STAGE2_WEAPON_DAMAGE;
		}

		if (stage >= 3) {
			total += SWIFT_STAGE3_WEAPON_DAMAGE;
		}

		return total;
	}

	/** 迅捷三阶段：冲刺无敌额外时长（刻）。 */
	public static int dashInvulnerableTicks(ServerPlayer player) {
		return stage(player, Talent.SWIFT) >= 3 ? SWIFT_STAGE3_INVULNERABLE_TICKS : 0;
	}

	// ------------------------------------------------------------------
	// BOSS 判定：c:bosses 标签（原版末影龙 / 凋灵与遵守约定的模组 BOSS）+ 自带血条字段
	// ------------------------------------------------------------------

	/**
	 * 这个生物算不算 BOSS。
	 *
	 * <p>口径是「屏幕最上方有没有它自己的血条」：
	 * <ul>
	 *   <li>原版末影龙的血条由 {@code EnderDragonFight} 管，实体类上没有事件字段，
	 *       所以显式认这一类；末影龙与凋灵也都在 Fabric 的 {@code c:bosses} 标签里；</li>
	 *   <li>其它模组的 BOSS 如果把自己加进 {@code c:bosses} 标签，直接算；</li>
	 *   <li>没打标签、但类里自己声明了 {@link BossEvent} 字段的（原版凋灵这种写法），
	 *       也按有血条算——这是「兼容其它模组血条」的兜底。</li>
	 * </ul>
	 */
	public static boolean isBoss(LivingEntity entity) {
		if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
			return true;
		}

		if (entity.getType().builtInRegistryHolder().is(ConventionalEntityTypeTags.BOSSES)) {
			return true;
		}

		return BOSS_CLASSES.computeIfAbsent(entity.getClass(), Talents::hasBossBarField);
	}

	/** 类（含父类）里有没有 {@link BossEvent} 字段——有就说明它会给自己开一条 BOSS 血条。 */
	private static boolean hasBossBarField(Class<?> type) {
		try {
			for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
				for (Field field : current.getDeclaredFields()) {
					if (BossEvent.class.isAssignableFrom(field.getType())) {
						return true;
					}
				}
			}
		} catch (Throwable ignored) {
			// 反射失败时按「不是 BOSS」处理，至少不影响正常击杀
		}

		return false;
	}

	/** 伤害的真正主人：直接命中者不是玩家时，再看投掷物的主人。 */
	public static ServerPlayer attackerOf(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player) {
			return player;
		}

		if (source.getEntity() instanceof Projectile projectile
				&& projectile.getOwner() instanceof ServerPlayer player) {
			return player;
		}

		Entity direct = source.getDirectEntity();

		if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
			return player;
		}

		return null;
	}
}
