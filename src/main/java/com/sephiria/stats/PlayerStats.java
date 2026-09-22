package com.sephiria.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.network.StatsSyncPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家属性：生命值之外的那些数值。
 *
 * <p>生命值不在这里——它就是原版的玩家血量，直接读 {@code LivingEntity} 即可，不需要另存一份
 * （否则两处数据还得互相同步）。这里只管自定义的那几项：
 * 蓝量、蓝量再生、物理强度、火/冰/电元素强度、防御力，以及攻击速度 / 近战范围 / 武器伤害这类
 * 换算用的百分比。
 *
 * <p>数值<b>跟着存档走</b>：每个玩家一份存在 Fabric 数据附件里（{@code player_stats}），
 * 变化时由 {@link #sync} 写回，所以药水给的永久加成（物理伤害 +2、HP 偷取 +1）、树叶与升级宝箱
 * 的进度都不会因为重登或重启而丢。附件只在服务端，客户端那边由 {@link StatsSyncPayload} 推。
 */
public final class PlayerStats {
	/** 默认值：MP 50、MP 再生 1、三种元素与物理强度各 20、防御 0。 */
	public static final double DEFAULT_MP = 50.0D;
	public static final double DEFAULT_MP_REGEN = 1.0D;
	public static final double DEFAULT_STRENGTH = 20.0D;
	public static final double DEFAULT_DEFENSE = 0.0D;
	/** 攻击速度（百分比）：武器面板攻速 = 武器基础攻速 × 它。 */
	public static final double DEFAULT_ATTACK_SPEED = 100.0D;
	/** 近战攻击范围（百分比）：武器的攻击范围与技能伤害范围都乘它。 */
	public static final double DEFAULT_MELEE_RANGE = 100.0D;
	/** 武器伤害（百分比）：赛菲莉亚武器的全部伤害（普攻 + 技能）都乘它；神器伤害不吃。 */
	public static final double DEFAULT_WEAPON_DAMAGE = 100.0D;
	/** 特殊攻击伤害（百分比）：只加成武器技能的伤害。 */
	public static final double DEFAULT_SPECIAL_ATTACK = 100.0D;
	/** 普通攻击伤害（百分比）：只加成普通攻击（含横扫与弩矢），技能不吃。 */
	public static final double DEFAULT_NORMAL_ATTACK_DAMAGE = 100.0D;
	/** 暴击几率（百分比）：默认 0，也就是不会暴击。 */
	public static final double DEFAULT_CRIT_CHANCE = 0.0D;
	/** 暴击伤害（百分比）：暴击时伤害乘它，默认 150 = 1.5 倍。 */
	public static final double DEFAULT_CRIT_DAMAGE = 150.0D;
	/** 移动速度（百分比）：玩家移速乘它。 */
	public static final double DEFAULT_MOVE_SPEED = 100.0D;
	/** 闪避率曲线的上限（80%）与尺度常数（43.28 点）。 */
	public static final double DODGE_CAP = 0.8D;
	public static final double DODGE_SCALE = 43.28D;
	/** 多少点经验换一个升级宝箱。 */
	public static final double EXPERIENCE_PER_CHEST = 1000.0D;

	/**
	 * 后加的那几项战斗属性单独成一组。
	 *
	 * <p>理由很实际：{@code RecordCodecBuilder} 一条记录最多 16 个字段，主记录已经占了 15 个，
	 * 所以新属性打包成一个可选的子记录——旧存档没有这一组时整组取默认值。
	 */
	private record Combat(double normalAttackDamage, double critChance, double critDamage, double ignoreDefense,
			double moveSpeed, double dodge) {
		static final Combat DEFAULT = new Combat(DEFAULT_NORMAL_ATTACK_DAMAGE, DEFAULT_CRIT_CHANCE,
				DEFAULT_CRIT_DAMAGE, 0.0D, DEFAULT_MOVE_SPEED, 0.0D);

		static final Codec<Combat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.DOUBLE.optionalFieldOf("normal_attack_damage", DEFAULT_NORMAL_ATTACK_DAMAGE)
						.forGetter(Combat::normalAttackDamage),
				Codec.DOUBLE.optionalFieldOf("crit_chance", DEFAULT_CRIT_CHANCE).forGetter(Combat::critChance),
				Codec.DOUBLE.optionalFieldOf("crit_damage", DEFAULT_CRIT_DAMAGE).forGetter(Combat::critDamage),
				Codec.DOUBLE.optionalFieldOf("ignore_defense", 0.0D).forGetter(Combat::ignoreDefense),
				Codec.DOUBLE.optionalFieldOf("move_speed", DEFAULT_MOVE_SPEED).forGetter(Combat::moveSpeed),
				Codec.DOUBLE.optionalFieldOf("dodge", 0.0D).forGetter(Combat::dodge)
		).apply(instance, Combat::new));
	}

	/**
	 * 存档用的那一份数值。
	 *
	 * <p>字段全部写成 optional：以后往 {@link Values} 里加新项时，旧存档读出来是默认值，
	 * 不会因为缺一个字段就整份读失败（读失败等于玩家的加成凭空消失）。
	 */
	private record Saved(double mp, double mpRegen, double physical, double potionPhysical, double fire, double ice,
			double lightning, double defense, double lifesteal, double leaves, double experienceTowardsChest,
			double attackSpeed, double meleeRange, double weaponDamage, double specialAttack, Combat combat) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.DOUBLE.optionalFieldOf("mp", DEFAULT_MP).forGetter(Saved::mp),
				Codec.DOUBLE.optionalFieldOf("mp_regen", DEFAULT_MP_REGEN).forGetter(Saved::mpRegen),
				Codec.DOUBLE.optionalFieldOf("physical", DEFAULT_STRENGTH).forGetter(Saved::physical),
				Codec.DOUBLE.optionalFieldOf("potion_physical", 0.0D).forGetter(Saved::potionPhysical),
				Codec.DOUBLE.optionalFieldOf("fire", DEFAULT_STRENGTH).forGetter(Saved::fire),
				Codec.DOUBLE.optionalFieldOf("ice", DEFAULT_STRENGTH).forGetter(Saved::ice),
				Codec.DOUBLE.optionalFieldOf("lightning", DEFAULT_STRENGTH).forGetter(Saved::lightning),
				Codec.DOUBLE.optionalFieldOf("defense", DEFAULT_DEFENSE).forGetter(Saved::defense),
				Codec.DOUBLE.optionalFieldOf("lifesteal", 0.0D).forGetter(Saved::lifesteal),
				Codec.DOUBLE.optionalFieldOf("leaves", 0.0D).forGetter(Saved::leaves),
				Codec.DOUBLE.optionalFieldOf("experience_towards_chest", 0.0D).forGetter(Saved::experienceTowardsChest),
				Codec.DOUBLE.optionalFieldOf("attack_speed", DEFAULT_ATTACK_SPEED).forGetter(Saved::attackSpeed),
				Codec.DOUBLE.optionalFieldOf("melee_range", DEFAULT_MELEE_RANGE).forGetter(Saved::meleeRange),
				Codec.DOUBLE.optionalFieldOf("weapon_damage", DEFAULT_WEAPON_DAMAGE).forGetter(Saved::weaponDamage),
				Codec.DOUBLE.optionalFieldOf("special_attack", DEFAULT_SPECIAL_ATTACK).forGetter(Saved::specialAttack),
				Combat.CODEC.optionalFieldOf("combat", Combat.DEFAULT).forGetter(Saved::combat)
		).apply(instance, Saved::new));

		/** 把当前数值打包成存档格式。 */
		static Saved of(Values values) {
			return new Saved(values.mp, values.mpRegen, values.physical, values.potionPhysical, values.fire,
					values.ice, values.lightning, values.defense, values.lifesteal, values.leaves,
					values.experienceTowardsChest, values.attackSpeed, values.meleeRange, values.weaponDamage,
					values.specialAttack, new Combat(values.normalAttackDamage, values.critChance, values.critDamage,
							values.ignoreDefense, values.moveSpeed, values.dodge));
		}

		/** 读档：把存档里的数值填进一份新的 {@link Values}。 */
		Values values() {
			Values values = new Values();
			values.mp = this.mp;
			values.mpRegen = this.mpRegen;
			values.physical = this.physical;
			values.potionPhysical = this.potionPhysical;
			values.fire = this.fire;
			values.ice = this.ice;
			values.lightning = this.lightning;
			values.defense = this.defense;
			values.lifesteal = this.lifesteal;
			values.leaves = this.leaves;
			values.experienceTowardsChest = this.experienceTowardsChest;
			values.attackSpeed = this.attackSpeed;
			values.meleeRange = this.meleeRange;
			values.weaponDamage = this.weaponDamage;
			values.specialAttack = this.specialAttack;
			values.normalAttackDamage = this.combat.normalAttackDamage;
			values.critChance = this.combat.critChance;
			values.critDamage = this.combat.critDamage;
			values.ignoreDefense = this.combat.ignoreDefense;
			values.moveSpeed = this.combat.moveSpeed;
			values.dodge = this.combat.dodge;
			return values;
		}
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 永久加成不该因为死一次就没了（树叶同理：它是商店的货币）
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "player_stats"));

	private static final Map<UUID, Values> STATS = new HashMap<>();
	/** 上一次推给客户端的「总值指纹」，用来发现神器换装这类不经过 sync 的变化。 */
	private static final Map<UUID, String> SNAPSHOTS = new HashMap<>();

	private PlayerStats() {
	}

	/**
	 * 显式初始化入口（由 {@link Sephiria#onInitialize()} 调用）。
	 *
	 * <p>附件类型写在静态字段里，加载这个类就会注册；这个方法只是让「初始化」这件事有个明确的
	 * 调用点，免得将来有人把静态字段挪走之后忘了注册——附件注册晚了，玩家存档里那份就读不出来。
	 */
	public static void register() {
		// 面板上那些「总值」有一大半来自神器（近战范围、武器伤害、暴击、闪避…），而神器的增减
		// 不会经过任何 sync 调用点，所以这里每秒对一次账：对不上就重推一次面板。
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					String signature = snapshot(player);

					if (!signature.equals(SNAPSHOTS.put(player.getUUID(), signature))) {
						sync(player);
					}
				}
			}

			if (server.getTickCount() % 20 != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Values values = of(player);

				if (values.mp >= DEFAULT_MP || values.mpRegen <= 0.0D) {
					continue;
				}

				// 蓝量回复：每秒加 mpRegen，回到默认上限为止（还没有「最大蓝量」这条属性）
				values.mp = Math.min(DEFAULT_MP, values.mp + values.mpRegen);
				sync(player);
			}
		});
	}

	/** 面板上那些实际值的指纹（神器一变它就会变）。 */
	private static String snapshot(ServerPlayer player) {
		return meleeRangeTotal(player) + "/" + attackSpeedTotal(player) + "/" + weaponDamageTotal(player)
				+ "/" + specialAttackTotal(player) + "/" + normalAttackDamageTotal(player) + "/"
				+ critChanceTotal(player) + "/" + critDamageTotal(player) + "/" + ignoreDefenseTotal(player)
				+ "/" + moveSpeedPercentTotal(player) + "/" + dodgeTotal(player) + "/" + dodgeRatePercent(player)
				+ "/" + physicalTotal(player);
	}

	/** 玩家退出时清缓存（附件里已经是最新状态，下次进来重新读）。 */
	public static void forget(ServerPlayer player) {
		STATS.remove(player.getUUID());
		SNAPSHOTS.remove(player.getUUID());
	}

	/** 一名玩家的全部自定义属性。 */
	public static final class Values {
		public double mp = DEFAULT_MP;
		public double mpRegen = DEFAULT_MP_REGEN;
		public double physical = DEFAULT_STRENGTH;
		/** 药水给的物理强度（面板算式里「药水」那一份固定值）。 */
		public double potionPhysical = 0.0D;
		public double fire = DEFAULT_STRENGTH;
		public double ice = DEFAULT_STRENGTH;
		public double lightning = DEFAULT_STRENGTH;
		public double defense = DEFAULT_DEFENSE;
		/** HP 偷取（俗称吸血）：每点 = 造成伤害的 0.1% 回血，见 {@link #lifestealTotal}。 */
		public double lifesteal = 0.0D;
		/** 树叶（货币）：每获得 1 点原版经验 +1。 */
		public double leaves = 0.0D;
		/** 累计获得的经验：每满 1000 发一个升级宝箱（计数器会扣掉已兑换的部分）。 */
		public double experienceTowardsChest = 0.0D;
		public double attackSpeed = DEFAULT_ATTACK_SPEED;
		public double meleeRange = DEFAULT_MELEE_RANGE;
		public double weaponDamage = DEFAULT_WEAPON_DAMAGE;
		public double specialAttack = DEFAULT_SPECIAL_ATTACK;
		/** 普通攻击伤害（%）：只加成普通攻击，技能不吃。 */
		public double normalAttackDamage = DEFAULT_NORMAL_ATTACK_DAMAGE;
		/** 暴击几率（%）：0 = 不会暴击。 */
		public double critChance = DEFAULT_CRIT_CHANCE;
		/** 暴击伤害（%）：暴击时伤害乘它，默认 150。 */
		public double critDamage = DEFAULT_CRIT_DAMAGE;
		/** 无视防御伤害（点）：每次造成伤害时额外打一次真实伤害。 */
		public double ignoreDefense = 0.0D;
		/** 移动速度（%）。 */
		public double moveSpeed = DEFAULT_MOVE_SPEED;
		/** 闪避（点）：按 {@link #dodgeRatePercent} 折算成闪避率。 */
		public double dodge = 0.0D;
	}

	/**
	 * 伤害倍率：所有 SEPHIRIA 伤害都是物理伤害，所以这里 = 物理强度倍率 ×（1 + 物理伤害增幅）。
	 *
	 * <p>物理伤害增幅是独立的一条乘算项（默认 0 = 没有增幅），不是加在物理强度上的。
	 */
	public static double damageMultiplier(ServerPlayer player) {
		return physicalTotal(player) / DEFAULT_STRENGTH
				* (1.0D + physicalAmpPercent(player) / 100.0D)
				* (weaponDamageTotal(player) / DEFAULT_WEAPON_DAMAGE);
	}

	/**
	 * 技能伤害倍率：在武器伤害的基础上再乘「特殊攻击伤害」。
	 *
	 * <p>只有武器技能用它——普通攻击不吃特殊攻击伤害。
	 */
	public static double skillDamageMultiplier(ServerPlayer player) {
		return damageMultiplier(player) * (specialAttackTotal(player) / DEFAULT_SPECIAL_ATTACK);
	}

	/** 武器伤害的实际值（%%）：面板值 × (1 + 神器与连击的百分比加成)。 */
	public static double weaponDamageTotal(ServerPlayer player) {
		return of(player).weaponDamage * (1.0D + ArtifactEffects.weaponDamagePercent(player) / 100.0D);
	}

	/** 特殊攻击伤害的实际值（%%）：面板值 + 神器固定加成。 */
	public static double specialAttackTotal(ServerPlayer player) {
		return of(player).specialAttack + ArtifactEffects.specialAttackBonus(player);
	}

	/** 普通攻击伤害的实际值（%）：面板值 × (1 + 神器与连击的百分比加成)。 */
	public static double normalAttackDamageTotal(ServerPlayer player) {
		return of(player).normalAttackDamage * (1.0D + ArtifactEffects.normalAttackDamagePercent(player) / 100.0D);
	}

	/**
	 * 普通攻击的最终倍率：在伤害倍率之上再乘「普通攻击伤害」。
	 *
	 * <p>普通攻击（含横扫与弩矢）用它；技能不用——技能走 {@link #skillDamageMultiplier}。
	 */
	public static double normalAttackMultiplier(ServerPlayer player) {
		return damageMultiplier(player) * (normalAttackDamageTotal(player) / DEFAULT_NORMAL_ATTACK_DAMAGE);
	}

	/** 暴击几率（%）：面板值 + 神器 + 精密连击。 */
	public static double critChanceTotal(ServerPlayer player) {
		return of(player).critChance + ArtifactEffects.critChanceBonus(player)
				+ ArtifactEffects.comboCritChancePercent(player);
	}

	/**
	 * 武器攻击的暴击几率（%）：在暴击几率之上再加「武器攻击的暴击几率」。
	 *
	 * <p>只有武器打出的伤害用它（普通攻击、横扫、弩矢、武器技能）；神器自己造成的伤害只用
	 * {@link #critChanceTotal}。
	 */
	public static double weaponCritChanceTotal(ServerPlayer player) {
		return critChanceTotal(player) + ArtifactEffects.weaponCritChanceBonus(player);
	}

	/** 暴击伤害（%）：面板值 + 神器 + 精密连击，默认 150。 */
	public static double critDamageTotal(ServerPlayer player) {
		return of(player).critDamage + ArtifactEffects.critDamageBonus(player)
				+ ArtifactEffects.comboCritDamagePercent(player);
	}

	/** 无视防御伤害（点）：面板值 + 神器，默认 0。 */
	public static double ignoreDefenseTotal(ServerPlayer player) {
		return of(player).ignoreDefense + ArtifactEffects.ignoreDefenseBonus(player);
	}

	/**
	 * 闪避率（%）：{@code 0.8 × (1 − e^(−闪避 / 43.28))}。
	 *
	 * <p>收益递减：闪避点越多，每点的边际收益越小，理论上限 80%。
	 */
	public static double dodgeRatePercent(ServerPlayer player) {
		return DODGE_CAP * (1.0D - Math.exp(-dodgeTotal(player) / DODGE_SCALE)) * 100.0D;
	}

	/** 闪避（点）：面板值 + 神器 + 影子连击。 */
	public static double dodgeTotal(ServerPlayer player) {
		return of(player).dodge + ArtifactEffects.dodgeBonus(player) + ArtifactEffects.comboDodgeBonus(player);
	}

	/**
	 * 近战攻击范围的实际值（%）：面板值 × (1 + 神器与连击的百分比加成)。
	 *
	 * <p>面板要显示这个总值——「乐谱·风」这类神器加的就是它，只发 values.meleeRange 的话
	 * 面板会一直显示 100%，看着像属性没生效。
	 */
	public static double meleeRangeTotal(ServerPlayer player) {
		return of(player).meleeRange * (1.0D + ArtifactEffects.meleeRangePercent(player) / 100.0D);
	}

	/** 移动速度的实际值（%）：面板值 × (1 + 神器加成)。 */
	public static double moveSpeedPercentTotal(ServerPlayer player) {
		return of(player).moveSpeed * (1.0D + ArtifactEffects.moveSpeedPercent(player) / 100.0D);
	}

	/** 移动速度倍率：1.0 = 默认值 100%。 */
	public static double moveSpeedMultiplier(ServerPlayer player) {
		return moveSpeedPercentTotal(player) / DEFAULT_MOVE_SPEED;
	}

	/**
	 * 物理强度的实际值：{@code (基础值 + 固定加成) × (1 + 百分比加成)}。
	 *
	 * <p>「物理伤害 +N」「物理强度 +N」这类固定值先加在基础值上；「物理强度 +x%」这类百分比
	 * 加成先把多个来源加起来，再整体乘上去（不是各自乘一次）。
	 */
	public static double physicalTotal(ServerPlayer player) {
		return physicalBreakdown(player).total();
	}

	/**
	 * 物理伤害增幅（%）：作为独立的一条乘算项，直接作用在最终物理伤害上
	 * （不是加在物理强度上）。默认 0，也就是没有增幅。
	 */
	public static double physicalAmpPercent(ServerPlayer player) {
		return ArtifactEffects.physicalAmpPercent(player);
	}

	/**
	 * 物理强度的来源明细，供面板按来源分色显示，也供结算复用。
	 *
	 * <p>目前只有神器一个来源；药水、树根祝福等来源接进来时在这里各加一项即可。
	 *
	 * <p>「最高元素伤害」如果加在物理强度上，也算神器那一份（同样是神器给的固定值）。
	 */
	public static PhysicalBreakdown physicalBreakdown(ServerPlayer player) {
		double artifactFlat = ArtifactEffects.physicalBonus(player);

		if (highestElement(player) == Element.PHYSICAL) {
			artifactFlat += ArtifactEffects.highestElementBonus(player);
		}

		return new PhysicalBreakdown(of(player).physical, artifactFlat, of(player).potionPhysical,
				ArtifactEffects.physicalPercentBonus(player), 0.0D);
	}

	/**
	 * HP 偷取（俗称吸血）：造成的伤害按 {@code 数值 / 1000} 回血，默认 0。
	 *
	 * <p>5 点 = 造成 100 点伤害回 0.5 点生命。目前只有药水会给，以后神器也能给。
	 */
	public static double lifestealTotal(ServerPlayer player) {
		return of(player).lifesteal;
	}

	/** 药水永久 +HP 偷取。 */
	public static void addPotionLifesteal(ServerPlayer player, double amount) {
		of(player).lifesteal += amount;
		sync(player);
	}

	/** 药水永久 +物理强度（算「药水」那一份来源，面板里红字）。 */
	public static void addPotionPhysical(ServerPlayer player, double amount) {
		of(player).potionPhysical += amount;
		sync(player);
	}

	/** 四项强度：物理强度与三种元素强度，「最高元素伤害」只在它们之间挑一个。 */
	public enum Element {
		PHYSICAL, FIRE, ICE, LIGHTNING
	}

	/**
	 * 四项强度里数值最高的一项——「最高元素伤害 +N」只加给它。
	 *
	 * <p>比较用的是<b>不含该加成</b>的数值：加成如果参与比较，自己就会改变谁最高，
	 * 会出现「谁加上去谁就最高」的不稳定结果。
	 *
	 * <p>并列时按 物理 → 火 → 冰 → 电 的顺序取靠前的那一项。
	 */
	public static Element highestElement(ServerPlayer player) {
		Values values = of(player);
		Element best = Element.PHYSICAL;
		double highest = physicalBreakdownBase(player);

		if (values.fire > highest) {
			best = Element.FIRE;
			highest = values.fire;
		}

		if (values.ice > highest) {
			best = Element.ICE;
			highest = values.ice;
		}

		if (values.lightning > highest) {
			best = Element.LIGHTNING;
			highest = values.lightning;
		}

		return best;
	}

	/** 不含「最高元素伤害」的物理强度，只给 {@link #highestElement} 比较用（避免自引用）。 */
	private static double physicalBreakdownBase(ServerPlayer player) {
		double flat = of(player).physical + ArtifactEffects.physicalBonus(player);
		return flat * (1.0D + ArtifactEffects.physicalPercentBonus(player) / 100.0D);
	}

	/** 某项强度的实际值（面板显示用）：最高的那一项带上「最高元素伤害」。 */
	public static double elementTotal(ServerPlayer player, Element element) {
		double base = switch (element) {
			case PHYSICAL -> physicalTotal(player);
			case FIRE -> of(player).fire;
			case ICE -> of(player).ice;
			case LIGHTNING -> of(player).lightning;
		};

		// 物理强度那条链在 physicalBreakdown 里已经把加成算进去了，这里不能重复加。
		if (element == Element.PHYSICAL || highestElement(player) != element) {
			return base;
		}

		return base + ArtifactEffects.highestElementBonus(player);
	}

	/**
	 * 攻击速度的来源明细，结构与物理强度那边一样。
	 *
	 * <p>攻击速度的「+N%%」在数值上就是 +N 点（基准 100 = 100%%），所以神器的加成直接进固定值那一段。
	 */
	public static PhysicalBreakdown attackSpeedBreakdown(ServerPlayer player) {
		return new PhysicalBreakdown(of(player).attackSpeed, ArtifactEffects.attackSpeedBonus(player), 0.0D,
				0.0D, 0.0D);
	}

	/** 物理强度的构成：基础 + 各来源固定值，再乘各来源百分比。 */
	public record PhysicalBreakdown(double base, double artifactFlat, double potionFlat,
			double artifactPercent, double potionPercent) {
		/** 括号里那一段（各固定值之和）。 */
		public double flat() {
			return this.base + this.artifactFlat + this.potionFlat;
		}

		/** 百分比那段（各来源百分比之和，单位 %）。 */
		public double percent() {
			return this.artifactPercent + this.potionPercent;
		}

		public double total() {
			return flat() * (1.0D + percent() / 100.0D);
		}
	}

	/** 近战攻击范围倍率：1.0 = 默认值 100%。 */
	public static double rangeMultiplier(ServerPlayer player) {
		return of(player).meleeRange * (1.0D + ArtifactEffects.meleeRangePercent(player) / 100.0D)
				/ DEFAULT_MELEE_RANGE;
	}

	/** 攻击速度倍率：1.0 = 默认值 100%。 */
	public static double attackSpeedMultiplier(ServerPlayer player) {
		return attackSpeedTotal(player) / DEFAULT_ATTACK_SPEED;
	}

	/** 攻击速度的实际值（%）：面板值 + 神器加成。 */
	public static double attackSpeedTotal(ServerPlayer player) {
		return of(player).attackSpeed + ArtifactEffects.attackSpeedBonus(player)
				+ ArtifactEffects.comboAttackSpeedPercent(player);
	}

	/** 当前蓝量（上限就是默认值 50——还没有「最大蓝量」这条属性）。 */
	public static double mpTotal(ServerPlayer player) {
		return of(player).mp;
	}

	/** 花蓝；不够返回 false（技能释放前先问一次，不够就不放、也不扣）。 */
	public static boolean spendMp(ServerPlayer player, double amount) {
		Values values = of(player);

		if (amount > 0.0D && values.mp < amount) {
			return false;
		}

		values.mp -= amount;
		sync(player);
		return true;
	}

	/** 树叶持有量。 */
	public static double leaves(ServerPlayer player) {
		return of(player).leaves;
	}

	/**
	 * 直接给树叶（商店出售这类非经验来源）。
	 *
	 * <p>和 {@link #addLeaves} 的区别：这里不推进「每 1000 经验一个升级宝箱」的计数——
	 * 卖东西不是赚经验。
	 */
	public static void grantLeaves(ServerPlayer player, double amount) {
		if (amount <= 0.0D) {
			return;
		}

		of(player).leaves += amount;
		sync(player);
	}

	/** 花掉树叶；不够则返回 false。 */
	public static boolean spendLeaves(ServerPlayer player, double amount) {
		Values values = of(player);

		if (values.leaves < amount) {
			return false;
		}

		values.leaves -= amount;
		sync(player);
		return true;
	}

	/**
	 * 获得经验：同时加树叶，并按每 1000 点发一个升级宝箱。
	 *
	 * <p>宝箱由这里直接塞进玩家背包（背包满了就掉在脚下），所以调用方不用管。
	 */
	public static void addLeaves(ServerPlayer player, int experience) {
		Values values = of(player);
		values.leaves += experience;
		values.experienceTowardsChest += experience;

		while (values.experienceTowardsChest >= EXPERIENCE_PER_CHEST) {
			values.experienceTowardsChest -= EXPERIENCE_PER_CHEST;
			ItemStack chest = new ItemStack(com.sephiria.registry.ModItems.UPGRADE_CHEST);

			if (!player.getInventory().add(chest)) {
				player.drop(chest, false, Prediction.SERVER_ONLY);
			}
		}

		sync(player);
	}

	/** 取属性：首次访问时从存档里读出来，读不到就是一份默认值。 */
	public static Values of(ServerPlayer player) {
		return STATS.computeIfAbsent(player.getUUID(), uuid -> {
			Saved saved = player.getAttached(SAVED);
			return saved == null ? new Values() : saved.values();
		});
	}

	/**
	 * 改完属性后调用：写回存档、并把最新数值推给客户端
	 * （物理强度与攻速都用含神器加成的总值）。
	 */
	public static void sync(ServerPlayer player) {
		// 所有改动都会走到这里，所以存档只在这一个地方写——属性存在附件里，重登/重启都不丢
		player.setAttached(SAVED, Saved.of(of(player)));
		ServerPlayNetworking.send(player, StatsSyncPayload.of(of(player), physicalTotal(player),
				elementTotal(player, Element.FIRE), elementTotal(player, Element.ICE),
				elementTotal(player, Element.LIGHTNING),
				// 面板要显示当前实际值：把限时加成（急速/旗帜）加回去；机械结算那边不加，免得重复
				attackSpeedTotal(player) + TimedAttributes.attackSpeedPercent(player), physicalAmpPercent(player),
				physicalBreakdown(player), attackSpeedBreakdown(player), weaponDamageTotal(player),
				specialAttackTotal(player), normalAttackDamageTotal(player), critChanceTotal(player),
				critDamageTotal(player), ignoreDefenseTotal(player),
				moveSpeedPercentTotal(player) + TimedAttributes.moveSpeedPercent(player),
				dodgeTotal(player), dodgeRatePercent(player), meleeRangeTotal(player)));
	}

	/** 进服时推一次，面板才有初始值（顺带把读出来的那份写回附件）。 */
	public static void syncOnJoin(ServerPlayer player) {
		sync(player);
	}
}
