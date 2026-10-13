package com.sephiria.frost;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.BlizzardHammerItem;
import com.sephiria.artifact.ExorcismScabbardItem;
import com.sephiria.artifact.IceBowItem;
import com.sephiria.artifact.VolspaItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.network.FrostSyncPayload;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 「冰霜武具」连击的四件充能 / 资源神器：<b>驱邪剑鞘</b>（剑气）、<b>暴风雪之锤</b>（锤子）、
 * <b>沃尔斯帕</b>（冰长矛）与<b>冰弓</b>（弹药 + 发动型技能）。
 *
 * <p>前三件各自攒一份充能，攒满后等一个触发口：剑鞘等<b>左键挥击</b>（{@link #onSwing}，
 * 打中敌人与挥空都算，由客户端的 {@code MinecraftAttackMixin} 发 {@code FrostQiPayload} 通知），
 * 锤子等<b>冲刺</b>（{@link #onDash}），长矛是<b>自动</b>的（攒满后、战斗中周围有敌人就飞）。
 * 冰弓的资源是<b>弹药</b>：每 0.8 秒自动装填 1 发（最多 7），发动技能时一次打空
 * （{@link #castIceBow}，技能本体在 {@code artifact.skill.IceBowSkill}）。
 *
 * <p><b>充能时间</b> = 基础刻数 ÷ (1 + 连击的「充能速度」+ 冰封的心脏那几件加起来)；
 * <b>伤害</b> = 固定值 + 冰元素强度 × 倍率，再乘 (1 + 连击与神器的「冰霜武具的伤害」)。
 * <b>额外触发</b>（连击 10 档 + 冰之翼）= 每次触发多打一轮：剑鞘多发一道剑气、锤子多扔一把、
 * 长矛多飞几根（冰弓不受它影响——那件的产出量由弹药管着）。
 *
 * <p><b>剑气是一道飞出去的弧</b>（2026-10-13 用户改的口径）：从玩家眼睛出发、沿<b>准星方向</b>
 * （完整三维方向，不是水平投影）飞 {@link #QI_DISTANCE} 格（与雷之裁决同距离），
 * 沿途对周围一圈（{@link #QI_DAMAGE_RADIUS}，比剑气贴图大一圈）的敌人各打一次，
 * 撞到方块就停。它<b>不需要瞄准敌人</b>——充能满了随手一挥就飞出去。
 *
 * <p><b>冰长矛</b>（沃尔斯帕）从玩家 12 格内的随机位置出现、冲向敌人：飞 {@link #SPEAR_DISTANCE} 格、
 * 速度比剑气快 30%、<b>贯穿敌人</b>、<b>可以穿墙</b>，伤害范围比剑气小 30%（都是用户定的口径）。
 *
 * <p>锤子在冲刺时朝最近的敌人扔一把（额外触发再多一把给下一个最近的），
 * 落地时周围 {@link #SMASH_RADIUS} 格一圈都吃伤害。
 *
 * <p><b>充能只在带上那件神器时走表</b>（【唯一】，取等级最高的那一份，与红蛇之眼同一套），
 * 并且<b>不受战斗 / 脱战状态影响</b>：三件的表一直在走（2026-10-13 用户口径，原先那条
 * 「脱战立即满充能」的机制已删掉）。唯一还看战斗状态的是<b>长矛的自动开打</b>——脱战不会自动攻击敌人。
 * 攒满时给一次雪片 + 一声轻响，HUD 那一行同时亮成冰蓝（见 {@link #sync}）。
 *
 * <p>伤害类型复用 {@code sephiria:ice_attack}（冰属性攻击伤害）：吃通用暴击、减伤与黄金之手，
 * 也照常触发冰川的霜之触（与普罗修姆戒指的附加冰伤同一条路）。
 * 伤害都只打「有威胁的生物」（{@code SephiriaDamage.strikeable}，不打玩家、不误伤和平生物）。
 */
public final class FrostArmament {
	/** 驱邪剑鞘的充能时间（tick）：6 秒。 */
	public static final int SCABBARD_CHARGE_TICKS = 120;
	/** 暴风雪之锤的充能时间（tick）：7 秒（一直走，不看战斗状态）。 */
	public static final int HAMMER_CHARGE_TICKS = 140;
	/** 沃尔斯帕的充能时间（tick）：6 秒（一直走，不看战斗状态；自动开打才要战斗）。 */
	public static final int SPEAR_CHARGE_TICKS = 120;

	/** 冰弓的弹药上限（发）：与神器等级无关。 */
	private static final int BOW_MAX_AMMO = 7;
	/** 冰弓每装填一发的间隔（tick）：0.8 秒。 */
	private static final int BOW_RELOAD_TICKS = 16;

	/** 剑气飞出去的距离（格）：2026-10-13 用户从 30 收到 18（同时把伤害半径放大 20%）。 */
	private static final double QI_DISTANCE = 18.0D;
	/** 剑气每刻飞多少格：慢比快好看一点，1.5 格/刻 → 18 格飞 12 刻。 */
	private static final double QI_SPEED = 1.5D;
	/** 剑气飞完要几刻：距离 ÷ 速度。 */
	private static final int QI_FLIGHT_TICKS = (int) Math.round(QI_DISTANCE / QI_SPEED);
	/** 剑气出手点离眼睛多远（格）：别在脑袋里生成，也免得贴脸的方块立刻把这一发吃掉。 */
	private static final double QI_START_OFFSET = 0.6D;
	/**
	 * 剑气<b>特效</b>相对飞行轨迹下移多少格（2026-10-13 用户要求 0.3）。
	 *
	 * <p>剑气的面是水平的、又沿准星方向飞，第一人称下那张面和视线正好共面——等于看不见。
	 * 往下挪 0.3 格之后它落在视线下方一点，扫一眼就能看到那道弧。
	 * <b>只挪特效</b>：伤害判定（{@link #strikeAlong}）仍然打在准星那条轨迹上，两者差 0.3 格、
	 * 对 1.8 格的判定半径来说可以忽略。
	 */
	private static final double QI_VISUAL_DROP = 0.3D;
	/** 剑气的伤害半径（格）：以飞行轨迹为轴；2026-10-13 用户要求「变大 20%」（1.5 → 1.8）。 */
	private static final double QI_DAMAGE_RADIUS = 1.8D;
	/** 每隔几刻在当前位置补一张弧状剑气贴图（寿命 8 刻，够连成一道飞过去的光）。 */
	private static final int QI_TRAIL_INTERVAL = 3;
	/** 剑气的长度（格）：弧状剑气那张方片的边长（贴图尺寸，不是伤害范围）。 */
	private static final double QI_LENGTH = 2.0D;
	/** 剑气贴图的方向（弧度，屏幕面内）：0 = 横着那道弧，-20° = 稍微斜一点（多道之间错开）。 */
	private static final float[] QI_ROLLS = { 0.0F, (float) Math.toRadians(-20.0D), (float) Math.toRadians(25.0D) };
	/** 同一发多道剑气之间的横向错开（格）：10 档的两道不会完全重合。 */
	private static final double QI_SPREAD = 0.35D;

	/** 冰长矛的索敌 / 触发范围（格）：与锤子统一（2026-10-13 用户定为 18 格）。 */
	private static final double SPEAR_TRIGGER_RANGE = 18.0D;
	/** 冰长矛「从玩家周围随机位置出现」的范围（格）：同样是 12 格。 */
	private static final double SPEAR_SPAWN_RANGE = 12.0D;
	/** 冰长矛出现点离玩家至少这么远（格）：别直接糊在脸上。 */
	private static final double SPEAR_SPAWN_MIN = 2.5D;
	/** 冰长矛的出现高度（格，相对玩家）：在玩家上下这一带随机。 */
	private static final double SPEAR_SPAWN_HEIGHT = 4.0D;
	/** 冰长矛的飞行距离（格）：2026-10-13 用户从 18 提到 36。 */
	private static final double SPEAR_DISTANCE = 36.0D;
	/** 冰长矛的速度（格/刻）：比剑气快 30%。 */
	private static final double SPEAR_SPEED = QI_SPEED * 1.3D;
	/** 冰长矛的伤害半径（格）：比剑气小 30%、又在 2026-10-13 被要求「提升 20%」。 */
	private static final double SPEAR_RADIUS = QI_DAMAGE_RADIUS * 0.7D * 1.2D;
	/** 冰长矛每隔几刻补一串雪片尾迹。 */
	private static final int SPEAR_TRAIL_INTERVAL = 2;
	/**
	 * 铁长矛贴图里矛身是 45° 斜的（矛尖在右上）：面内转 -45° 把矛身摆正，之后贴图的「右」就是矛尖。
	 *
	 * <p><b>再加 180°</b>：2026-10-13 用户实测「矛的尖端朝向和飞行方向相反」——也就是基准朝向
	 * 把贴图的「右」对到了飞行方向的<b>反面</b>（同一个 180° 的符号问题在斧子上看不出来，
	 * 因为它一路翻滚、面内角度一直在扫）。所以这里是 -45 + 180 = 135°。
	 */
	private static final float SPEAR_IN_PLANE_DEGREES = 135.0F;

	/** 锤子 / 冰弓箭矢的出手点离玩家多远（格）：2026-10-13 用户要求「从玩家周围 2 格左右的随机位置射出」。 */
	private static final double NEAR_SPAWN_RADIUS = 2.0D;
	/** 出手点离玩家至少这么远（格）：别在身体里生成。 */
	private static final double NEAR_SPAWN_MIN = 0.4D;
	/** 出手点的纵向随机幅度（格）。 */
	private static final double NEAR_SPAWN_HEIGHT = 1.0D;

	/** 冰弓箭矢的速度（格/刻）：比长矛再快一点，箭该是「嗖」的一下。 */
	private static final double ARROW_SPEED = 3.0D;
	/** 冰弓箭矢的飞行距离（格）：2026-10-13 用户定为 36。 */
	private static final double ARROW_DISTANCE = 36.0D;
	/** 冰弓箭矢的判定半径（格）：细一点（箭是点状的）；2026-10-13 用户要求「变大 30%」（0.6 → 0.78）。 */
	private static final double ARROW_RADIUS = 0.6D * 1.3D;
	/** 一次打空时每发箭的随机偏角（度）：7 发不会连成一条直线。 */
	private static final double ARROW_SPREAD = 3.0D;
	/** 箭的贴图同样是 45° 斜的（箭头在右上、白色的是尾羽）：和长矛一样取 135°（见上面那条注释）。 */
	private static final float ARROW_IN_PLANE_DEGREES = 135.0F;

	/** 锤子的索敌半径（格）：2026-10-13 与长矛一起定为 18 格（原来是 8 → 12 → 18）。 */
	private static final double HAMMER_RANGE = 18.0D;
	/** 每把锤子落地的伤害半径（格）。 */
	private static final double SMASH_RADIUS = 2.5D;
	/** 锤子飞行的刻数（约 0.4 秒）。 */
	private static final int FLIGHT_TICKS = 8;
	/** 到站后多停的刻数：客户端位置插值晚一刻跟上，接触发才落在「看着砸到」的那一刻。 */
	private static final int FLIGHT_TAIL_TICKS = 1;
	/** 飞行途中的总自转角度（度）：绕「与飞行方向垂直的水平轴」翻滚，像被扔出去的斧头。 */
	private static final float FLIGHT_SPIN = 900.0F;
	/** 铁斧贴图里斧刃在头部右侧（贴图右 = 刃），面内不用转。 */
	private static final float HAMMER_IN_PLANE_DEGREES = 0.0F;
	/** 本体（铁斧 / 铁长矛）的尺寸：模型本身差不多一格长，稍微放大一点才有体积感。 */
	private static final float BODY_SCALE = 1.5F;
	/** 箭矢本体的尺寸：箭比斧子细得多，放小一点才像箭（1.5 倍会变成一根大木棍）。 */
	private static final float ARROW_BODY_SCALE = 1.2F;
	/** 本体的亮度覆盖（满亮）：不覆写的话夜里就是一团黑。 */
	private static final int BRIGHTNESS_OVERRIDE = 0xF000F0;
	/** 缩放 / 自转与位移的客户端插值时长（tick）：与陨石同一套（位移会晚一刻到位）。 */
	private static final int TRANSFORM_INTERPOLATION_TICKS = 2;
	private static final int POS_INTERPOLATION_TICKS = 1;
	/** 本体的清理标签：演出表只在内存里，崩服卡在飞行中途时留下的本体靠它扫掉。 */
	private static final String PROJECTILE_TAG = "sephiria_fantasy_frost_projectile";
	/** 清扫残留本体的半径（格）。 */
	private static final double SWEEP_RADIUS = 64.0D;

	/** 一名玩家的冰霜武具状态：四份充能 / 弹药 + 上一次推给客户端的 HUD 快照。 */
	private static final class State {
		/** 剑鞘已攒的刻数；{@link #scabbardReady} 为真时不再往前走。 */
		private int scabbardCharge;
		private boolean scabbardReady;
		/** 锤子已攒的刻数（每刻都推进）。 */
		private int hammerCharge;
		private boolean hammerReady;
		/** 长矛已攒的刻数（每刻都推进；攒满后要战斗中才会飞）。 */
		private int spearCharge;
		private boolean spearReady;
		/** 冰弓的弹药（发）与装填计时。 */
		private int bowAmmo = BOW_MAX_AMMO;
		private int bowReloadTimer;
		/** 发动后的连发计数：这一轮还剩几发没打出去（每刻一发，见 {@link #tickBow}）。 */
		private int bowBurst;
		/** 上一次推给客户端的 HUD 状态；null = 还没推过（首刻必定同步一次）。 */
		private FrostSyncPayload synced;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();
	/** 飞行中的锤子：先拷一份再 clear 的推进方式与陨石同一套。 */
	private static final List<Hammer> HAMMERS = new ArrayList<>();
	/** 飞行中的剑气（同样是先拷一份再 clear）。 */
	private static final List<Qi> QIS = new ArrayList<>();
	/** 飞行中的冰长矛与冰弓箭矢。 */
	private static final List<Spear> SPEARS = new ArrayList<>();
	private static final List<IceArrow> ARROWS = new ArrayList<>();

	/**
	 * 一道飞行中的剑气。
	 *
	 * @param from      出手点（眼睛前方一点）
	 * @param direction 飞行方向（释放那一刻的准星方向，三维、之后不再改）
	 * @param damage    在挥出那一刻算好的伤害
	 * @param roll      弧状贴图的面内倾角（同一发的多道各不相同）
	 * @param tick      已经飞了几刻
	 * @param hit       已经挨过这一道的实体 id：同一条剑气对同一个目标只结算一次
	 */
	private record Qi(ServerLevel level, UUID ownerId, Vec3 from, Vec3 direction, float damage, float roll,
			int tick, Set<Integer> hit) {
	}

	/**
	 * 一把飞行中的锤子。
	 *
	 * @param from     出手点（玩家胸口）
	 * @param target   被打的那只敌人；飞到一半它没了就继续飞向 {@code fallback}
	 * @param fallback 出发时记下的落点（目标没了时的替补）
	 * @param damage   在出手那一刻算好的伤害
	 * @param tick     已经飞了几刻
	 */
	private record Hammer(ServerLevel level, UUID ownerId, LivingEntity target, Vec3 from, Vec3 fallback,
			float damage, int tick, Display.ItemDisplay body) {
	}

	/**
	 * 一根飞行中的冰长矛（沃尔斯帕）。
	 *
	 * @param from      出现点（玩家 12 格内的随机位置）
	 * @param direction 飞行方向（冲向目标那一刻定下）
	 * @param damage    出现那一刻算好的伤害
	 * @param traveled  已经飞了多少格（到 {@link #SPEAR_DISTANCE} 就散）
	 * @param hit       已经挨过这一根的实体 id：贯穿但同一个目标只打一次
	 */
	private record Spear(ServerLevel level, UUID ownerId, Vec3 from, Vec3 direction, float damage,
			double traveled, Set<Integer> hit, Display.ItemDisplay body) {
	}

	/**
	 * 一支飞行中的冰弓箭矢（冰弓）。
	 *
	 * @param from      出手点（玩家眼睛前方，带一点随机错开）
	 * @param direction 飞行方向（准星方向 + 一点随机偏角）
	 * @param damage    放出去那一刻算好的伤害
	 * @param traveled  已经飞了多少格
	 */
	private record IceArrow(ServerLevel level, UUID ownerId, Vec3 from, Vec3 direction, float damage,
			double traveled, Display.ItemDisplay body) {
	}

	private FrostArmament() {
	}

	/** 注册推进器：两件充能表的推进 + 飞行中的剑气与锤子（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(FrostArmament::tick);
	}

	/** 玩家退出时收掉状态（与其它神器同一批调用）。 */
	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	/**
	 * 左键挥击（打中敌人或挥空都算，由客户端的 {@code MinecraftAttackMixin} 发 {@code FrostQiPayload}
	 * 通知）：剑鞘充能满了就从眼睛前方沿<b>准星方向</b>放出一道剑气。
	 *
	 * <p>不需要瞄准敌人——剑气自己飞出去打沿途的目标；主手不是本模组武器（或人没了）时什么都不做。
	 */
	public static void onSwing(ServerPlayer player) {
		State state = STATES.get(player.getUUID());

		if (state == null || !state.scabbardReady || !(player.level() instanceof ServerLevel level)) {
			return;
		}

		if (!(player.getMainHandItem().getItem() instanceof com.sephiria.weapon.SephiriaWeapon)
				|| !player.isAlive()) {
			return;
		}

		int level0 = ArtifactEffects.highestLevelOf(player, ModItems.EXORCISM_SCABBARD);

		if (level0 < 0) {
			return;
		}

		double damage = qiDamage(player, level0);

		if (damage <= 0.0D) {
			return;
		}

		state.scabbardReady = false;
		state.scabbardCharge = 0;
		int times = 1 + extraTriggers(player);
		Vec3 direction = player.getLookAngle();
		Vec3 origin = player.getEyePosition();

		for (int index = 0; index < times; index++) {
			launchQi(player, level, origin, direction, damage, index, times);
		}
	}

	/**
	 * 「冰霜武具额外触发次数」：连击 10 档 + 冰之翼那一件的同一项，两者相加。
	 *
	 * <p>作用在剑鞘（多一道剑气）、锤子（多一把）、长矛（多一根）上；
	 * <b>冰弓不受它影响</b>——那件的产出量由弹药（最多 7 发）管着，所谓「额外触发」在那里没有意义。
	 */
	private static int extraTriggers(ServerPlayer player) {
		return ArtifactCombo.FROST_ARMAMENT.frostExtraTriggers(comboLevel(player))
				+ ArtifactEffects.frostExtraTriggersBonus(player);
	}

	/**
	 * 冰弓发动：把储存的箭矢交给连发队列（{@code artifact.skill.IceBowSkill} 调它）。
	 *
	 * <p><b>不是一瞬间全打出去</b>（2026-10-13 用户口径：每 0.05 秒一发 = 每刻一发）：
	 * 这里只把弹药数搬进 {@code State.bowBurst}，真正的发射在 {@link #tickBow} 里每刻一发，
	 * 7 发就是约 7 刻打完。每发的方向按<b>当时</b>的准星取，所以连发期间可以扫射。
	 *
	 * <p>没有弹药就什么都不做（技能那边会照常进 0.5 秒冷却，无所谓——反正也没得打）。
	 */
	public static void castIceBow(ServerPlayer player, int level) {
		State state = STATES.get(player.getUUID());

		if (state == null || state.bowAmmo <= 0 || !(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		if (ArtifactEffects.highestLevelOf(player, ModItems.ICE_BOW) < 0) {
			return;
		}

		state.bowBurst = state.bowAmmo;
		state.bowAmmo = 0;
		state.bowReloadTimer = 0;
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END,
				SoundSource.PLAYERS, 0.9F, 1.3F);
	}

	/** 连发推进：每刻打出一发（每发重新取一次准星与伤害），打空了就停。 */
	private static void tickBowBurst(ServerPlayer player, State state) {
		if (state.bowBurst <= 0 || !(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		int level0 = ArtifactEffects.highestLevelOf(player, ModItems.ICE_BOW);

		if (level0 < 0 || !player.isAlive()) {
			state.bowBurst = 0;
			return;
		}

		double damage = bowDamage(player, level0);
		state.bowBurst--;
		launchArrow(player, serverLevel, player.getLookAngle(), damage);
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT,
				SoundSource.PLAYERS, 0.7F, 1.25F + level0 * 0.03F);
	}

	/**
	 * 冲刺时挥出锤子（由 {@link com.sephiria.ability.DashSkill#perform} 在冲刺真正开始后调用）。
	 *
	 * <p>默认只扔<b>一把</b>（打最近的敌人，落地 AOE）；「额外触发」每多一级再多一把
	 * ——第二把找下一个最近的敌人（敌人数不够就重复打同一个）。
	 * 没带锤子 / 没充能完 / 索敌范围内没有有威胁的生物时什么都不做——<b>也不清充能</b>
	 * （与陨石「附近没人就不吃冷却」同一套口径）。
	 */
	public static void onDash(ServerPlayer player) {
		State state = STATES.get(player.getUUID());

		if (state == null || !state.hammerReady || !(player.level() instanceof ServerLevel level)) {
			return;
		}

		int level0 = ArtifactEffects.highestLevelOf(player, ModItems.BLIZZARD_HAMMER);

		if (level0 < 0) {
			return;
		}

		int count = 1 + extraTriggers(player);
		List<LivingEntity> victims = nearbyTargets(player, level, HAMMER_RANGE, count);
		double damage = hammerDamage(player, level0);

		if (victims.isEmpty() || damage <= 0.0D) {
			return;
		}

		state.hammerReady = false;
		state.hammerCharge = 0;
		throwHammers(player, level, victims, damage, count);
	}

	// ------------------------------------------------------------------ 充能表

	private static void tick(MinecraftServer server) {
		advanceQis(server);
		advanceHammers(server);
		advanceSpears(server);
		advanceArrows(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// 四件各自查一次：只带其中几件时，没带的那几件不能把带了的那些状态一起清掉
			boolean hasScabbard = ArtifactEffects.highestLevelOf(player, ModItems.EXORCISM_SCABBARD) >= 0;
			boolean hasHammer = ArtifactEffects.highestLevelOf(player, ModItems.BLIZZARD_HAMMER) >= 0;
			boolean hasSpear = ArtifactEffects.highestLevelOf(player, ModItems.VOLSPA) >= 0;
			boolean hasBow = ArtifactEffects.highestLevelOf(player, ModItems.ICE_BOW) >= 0;

			if (!hasScabbard && !hasHammer && !hasSpear && !hasBow) {
				// 一件都没带：收起 HUD 那一行（推过一次才需要收，免得每次进服都白发一包）
				State gone = STATES.remove(player.getUUID());

				if (gone != null && gone.synced != null) {
					ServerPlayNetworking.send(player, FrostSyncPayload.none());
				}

				continue;
			}

			State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
			int comboLevel = comboLevel(player);

			if (hasScabbard) {
				tickScabbard(player, state, comboLevel);
			} else {
				state.scabbardCharge = 0;
				state.scabbardReady = false;
			}

			if (hasHammer) {
				tickHammer(player, state, comboLevel);
			} else {
				state.hammerCharge = 0;
				state.hammerReady = false;
			}

			if (hasSpear) {
				tickSpear(player, state, comboLevel);
			} else {
				state.spearCharge = 0;
				state.spearReady = false;
			}

			if (hasBow) {
				tickBowBurst(player, state);
				tickBow(state);
			} else {
				// 没带弓时弹药直接回满：重新带上就是满的（装填表不用在背包里空转）；
				// 连发队列也一起清掉，免得卸下再带上时突然自己往外冒箭
				state.bowAmmo = BOW_MAX_AMMO;
				state.bowReloadTimer = 0;
				state.bowBurst = 0;
			}

			sync(player, state, hasScabbard, hasHammer, hasSpear, hasBow);
		}
	}

	/**
	 * HUD 同步：四件的带没带 / 攒没攒满 / 弓剩几发，<b>有变化才推</b>（初值 null 保证首刻推一次，
	 * 与乌云 / 太阳剑的「变化才推」同一套）。充能与弹药被打掉时状态自然变化，
	 * 所以这里只管对账、不用在触发口那边再补一句推送。
	 */
	private static void sync(ServerPlayer player, State state, boolean hasScabbard, boolean hasHammer,
			boolean hasSpear, boolean hasBow) {
		FrostSyncPayload payload = new FrostSyncPayload(hasScabbard, state.scabbardReady,
				hasHammer, state.hammerReady, hasSpear, state.spearReady,
				hasBow, state.bowAmmo, BOW_MAX_AMMO);

		if (payload.equals(state.synced)) {
			return;
		}

		state.synced = payload;
		ServerPlayNetworking.send(player, payload);
	}

	/** 剑鞘：6 秒的表一直走（2026-10-13 用户口径：充能不受战斗 / 脱战状态影响）。 */
	private static void tickScabbard(ServerPlayer player, State state, int comboLevel) {
		if (state.scabbardReady) {
			return;
		}

		if (++state.scabbardCharge >= chargeTicks(SCABBARD_CHARGE_TICKS, comboLevel, player)) {
			state.scabbardReady = true;
			ready(player, 1.6F);
		}
	}

	/**
	 * 沃尔斯帕：充能表一直走（与其他两件一样，不看战斗状态），攒满后<b>周围有敌人就自动飞长矛</b>
	 * ——没有敌人在附近就不飞、也不清充能（与红蛇之眼「附近没人就不落石」同一套口径）。
	 *
	 * <p><b>但「开打」仍要求战斗状态</b>（2026-10-13 用户口径：进入战斗状态才会自动触发，
	 * 脱战不会自动攻击敌人）——脱战时它静静攒满等着，一进战斗立刻飞那一下（是设计）。
	 */
	private static void tickSpear(ServerPlayer player, State state, int comboLevel) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		if (!state.spearReady) {
			if (++state.spearCharge >= chargeTicks(SPEAR_CHARGE_TICKS, comboLevel, player)) {
				state.spearReady = true;
				ready(player, 1.4F);
			}

			return;
		}

		// 攒满了也要在战斗中才会自动开打（用户口径：脱战不自动攻击敌人）
		if (!CombatState.isInCombat(player)) {
			return;
		}

		int level0 = ArtifactEffects.highestLevelOf(player, ModItems.VOLSPA);

		if (level0 < 0) {
			return;
		}

		int count = VolspaItem.countAt(level0) + extraTriggers(player);
		List<LivingEntity> victims = nearbyTargets(player, level, SPEAR_TRIGGER_RANGE, count);
		double damage = spearDamage(player, level0);

		if (victims.isEmpty() || damage <= 0.0D) {
			return;
		}

		state.spearReady = false;
		state.spearCharge = 0;
		throwSpears(player, level, victims, damage, count);
	}

	/** 冰弓：带上就一直装填（不要求战斗），每 {@link #BOW_RELOAD_TICKS} 刻 +1 发、到上限停。 */
	private static void tickBow(State state) {
		if (state.bowAmmo >= BOW_MAX_AMMO) {
			state.bowAmmo = BOW_MAX_AMMO;
			state.bowReloadTimer = 0;
			return;
		}

		if (++state.bowReloadTimer >= BOW_RELOAD_TICKS) {
			state.bowReloadTimer = 0;
			state.bowAmmo++;
		}
	}

	/** 锤子：7 秒的表一直走（2026-10-13 用户口径：充能不受战斗 / 脱战状态影响）。 */
	private static void tickHammer(ServerPlayer player, State state, int comboLevel) {
		if (state.hammerReady) {
			return;
		}

		if (++state.hammerCharge >= chargeTicks(HAMMER_CHARGE_TICKS, comboLevel, player)) {
			state.hammerReady = true;
			ready(player, 1.2F);
		}
	}

	/**
	 * 实际充能刻数：基础刻数 ÷ (1 + 连击的「充能速度」+ 冰封的心脏那几件的同一项)，最小 1 刻。
	 *
	 * <p>两处加起来再除：装满 8 颗满级冰封的心脏（+100% 一颗）理论上能叠到 +800%，也就是快 9 倍
	 * ——那是用户给的数值口径，不额外设上限（【唯一】的神器才管叠加，这颗没有【唯一】）。
	 */
	private static int chargeTicks(int base, int comboLevel, ServerPlayer player) {
		double percent = ArtifactCombo.FROST_ARMAMENT.frostChargeSpeedPercent(comboLevel)
				+ ArtifactEffects.frostChargeSpeedBonus(player);
		return Math.max(1, (int) Math.round(base / (1.0D + percent / 100.0D)));
	}

	/** 攒满时的小提示：脚下一捧雪片 + 一声轻响（这一套没有 HUD 条，靠它提醒「可以打了」）。 */
	private static void ready(ServerPlayer player, float pitch) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(),
				14, 0.35D, 0.5D, 0.35D, 0.04D);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.POWDER_SNOW_BREAK,
				SoundSource.PLAYERS, 0.5F, pitch);
	}

	private static int comboLevel(ServerPlayer player) {
		return ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.FROST_ARMAMENT, 0);
	}

	// ------------------------------------------------------------------ 剑气

	/** 剑气的伤害：固定值 + 冰元素强度 × 倍率，再乘「冰霜武具的伤害」。 */
	private static double qiDamage(ServerPlayer player, int level0) {
		double flat = ExorcismScabbardItem.flatDamageAt(level0);
		double ice = PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
				* ExorcismScabbardItem.icePercentAt(level0) / 100.0D;
		return (flat + ice) * frostDamageMultiplier(player);
	}

	/** 锤子的伤害：固定值 + 冰元素强度 × 倍率，再乘「冰霜武具的伤害」。 */
	private static double hammerDamage(ServerPlayer player, int level0) {
		double flat = BlizzardHammerItem.flatDamageAt(level0);
		double ice = PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
				* BlizzardHammerItem.icePercentAt(level0) / 100.0D;
		return (flat + ice) * frostDamageMultiplier(player);
	}

	/** 冰长矛的伤害：固定值 + 冰元素强度 × 倍率，再乘「冰霜武具的伤害」。 */
	private static double spearDamage(ServerPlayer player, int level0) {
		double flat = VolspaItem.flatDamageAt(level0);
		double ice = PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
				* VolspaItem.icePercentAt(level0) / 100.0D;
		return (flat + ice) * frostDamageMultiplier(player);
	}

	/** 冰弓箭矢的伤害（每发）：固定值 + 冰元素强度 × 倍率，再乘「冰霜武具的伤害」。 */
	private static double bowDamage(ServerPlayer player, int level0) {
		double flat = IceBowItem.flatDamageAt(level0);
		double ice = PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
				* IceBowItem.icePercentAt(level0) / 100.0D;
		return (flat + ice) * frostDamageMultiplier(player);
	}

	/** 「冰霜武具的伤害」：连击档位与神器的同一项相加。 */
	private static double frostDamageMultiplier(ServerPlayer player) {
		double percent = ArtifactCombo.FROST_ARMAMENT.frostDamagePercent(comboLevel(player))
				+ ArtifactEffects.frostArmamentDamagePercent(player);
		return 1.0D + percent / 100.0D;
	}

	/**
	 * 放出一道飞行中的剑气：从眼睛前方 {@link #QI_START_OFFSET} 格处出发，沿 {@code direction}
	 * 飞 {@link #QI_DISTANCE} 格。同一发多道时按「屏幕右」错开一点，两道不会完全重合。
	 *
	 * @param index 第几道（0 起）
	 * @param total 这一发一共几道
	 */
	private static void launchQi(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 direction,
			double damage, int index, int total) {
		Vec3 from = eye.add(direction.scale(QI_START_OFFSET)).add(sideOffset(direction, index, total));

		QIS.add(new Qi(level, player.getUUID(), from, direction.normalize(), (float) damage,
				QI_ROLLS[index % QI_ROLLS.length], 0, new HashSet<>()));

		level.playSound(null, from.x, from.y, from.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5F, 1.6F);
	}

	/** 玩家周围 {@link #NEAR_SPAWN_RADIUS} 格内的随机一点：锤子与冰弓箭矢的出手点。 */
	private static Vec3 randomNearSpawn(ServerLevel level, LivingEntity player) {
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double radius = NEAR_SPAWN_MIN + random.nextDouble() * (NEAR_SPAWN_RADIUS - NEAR_SPAWN_MIN);
		double height = (random.nextDouble() - 0.5D) * 2.0D * NEAR_SPAWN_HEIGHT;
		return new Vec3(player.getX() + Math.cos(angle) * radius,
				player.getY() + player.getBbHeight() * 0.6D + height,
				player.getZ() + Math.sin(angle) * radius);
	}

	/** 以飞行方向为基准的横向偏移：把同一发的多道剑气分摊在「屏幕右」方向上。 */
	private static Vec3 sideOffset(Vec3 direction, int index, int total) {
		if (total <= 1) {
			return Vec3.ZERO;
		}

		Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = direction.cross(up);

		if (right.lengthSqr() < 1.0E-4D) {
			// 垂直向上的剑气没有「右」可言（水平投影是 0），退化成世界 X 轴
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}

		return right.normalize().scale((index - (total - 1) / 2.0D) * QI_SPREAD);
	}

	/** 推进所有飞行中的剑气（先拷一份再 clear，与锤子 / 陨石同一套）。 */
	private static void advanceQis(MinecraftServer server) {
		if (QIS.isEmpty()) {
			return;
		}

		List<Qi> inFlight = new ArrayList<>(QIS);
		QIS.clear();

		for (Qi qi : inFlight) {
			Qi next = advanceQi(qi);

			if (next != null) {
				QIS.add(next);
			}
		}
	}

	/**
	 * 把一道剑气往前推一刻；返回 null 表示它已经飞完（或撞墙 / 主人掉线）可以收工。
	 *
	 * <p>每刻走 {@link #QI_SPEED} 格：路上先做一次方块射线检测（撞到实心方块就停在墙上），
	 * 再对轨迹周围 {@link #QI_DAMAGE_RADIUS} 格内的有威胁生物各结算一次（同一条剑气对同一个目标
	 * 只打一次），最后按 {@link #QI_TRAIL_INTERVAL} 补一张弧状贴图 + 每刻一串雪片。
	 */
	private static Qi advanceQi(Qi qi) {
		ServerPlayer owner = qi.level().getServer().getPlayerList().getPlayer(qi.ownerId());

		// 主人掉线 / 死了：这一道直接散掉（伤害要归到玩家头上，没有玩家就没有归属）
		if (owner == null || !owner.isAlive()) {
			return null;
		}

		// 穿墙：不做方块检测（2026-10-13 用户口径「冰霜武具的伤害可以穿墙」）
		Vec3 stop = qi.from().add(qi.direction().scale(qi.tick() * QI_SPEED + QI_SPEED));

		strikeAlong(qi, owner, stop);

		// 特效整体下移 QI_VISUAL_DROP：第一人称下才看得见那张水平的面（伤害仍在 stop 上结算）
		double visualY = stop.y - QI_VISUAL_DROP;

		if (qi.tick() % QI_TRAIL_INTERVAL == 0) {
			Vec3 dir = qi.direction();
			qi.level().sendParticles(new ModParticleTypes.FrostQiOptions((float) QI_LENGTH, qi.roll(),
							(float) dir.x, (float) dir.y, (float) dir.z),
					stop.x, visualY, stop.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		qi.level().sendParticles(ParticleTypes.SNOWFLAKE, stop.x, visualY, stop.z, 2, 0.25D, 0.25D, 0.25D, 0.02D);

		int tick = qi.tick() + 1;

		if (tick > QI_FLIGHT_TICKS) {
			return null;
		}

		return new Qi(qi.level(), qi.ownerId(), qi.from(), qi.direction(), qi.damage(), qi.roll(), tick, qi.hit());
	}

	/** 剑气在 {@code at} 这一小段周围结算一次伤害：有威胁、且这一道还没打过的目标各挨一下。 */
	private static void strikeAlong(Qi qi, ServerPlayer owner, Vec3 at) {
		AABB area = new AABB(at, at).inflate(QI_DAMAGE_RADIUS);

		for (LivingEntity victim : qi.level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner) || !qi.hit().add(victim.getId())) {
				continue;
			}

			// 沿途要稳定打中每个目标：先清无敌帧（与陨石 / 露水同一套）
			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(qi.level(), SephiriaDamage.iceAttack(qi.level(), owner), qi.damage());
		}
	}

	// ------------------------------------------------------------------ 冰长矛（沃尔斯帕）

	/**
	 * 冰长矛出现的范围：{@link #SPEAR_SPAWN_RANGE} 格内、离玩家至少
	 * {@link #SPEAR_SPAWN_MIN} 格、上下 {@link #SPEAR_SPAWN_HEIGHT} 格内的随机位置。
	 *
	 * <p>角度与半径都从随机数取（不用「球内均匀」那种讲究写法——观感只要求「从周围某个地方飞过来」），
	 * 万一抽到方块里也不管：长矛本来就<b>可以穿墙</b>。
	 */
	private static Vec3 randomSpawn(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double radius = SPEAR_SPAWN_MIN + random.nextDouble() * (SPEAR_SPAWN_RANGE - SPEAR_SPAWN_MIN);
		double height = (random.nextDouble() - 0.5D) * 2.0D * SPEAR_SPAWN_HEIGHT;
		return new Vec3(player.getX() + Math.cos(angle) * radius,
				player.getY() + 1.0D + height,
				player.getZ() + Math.sin(angle) * radius);
	}

	/** 飞 {@code count} 根冰长矛：每根从玩家周围的随机位置出现、冲向 {@code victims} 里随机的一只。 */
	private static void throwSpears(ServerPlayer player, ServerLevel level, List<LivingEntity> victims,
			double damage, int count) {
		RandomSource random = level.getRandom();

		for (int index = 0; index < count; index++) {
			LivingEntity target = victims.get(random.nextInt(victims.size()));
			Vec3 from = randomSpawn(level, player);
			Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			Vec3 direction = aim.subtract(from);

			if (direction.lengthSqr() < 1.0E-6D) {
				continue;
			}

			Display.ItemDisplay body = spawnBody(level, from, ModItems.ICE_SPEAR, BODY_SCALE);
			SPEARS.add(new Spear(level, player.getUUID(), from, direction.normalize(), (float) damage,
					0.0D, new HashSet<>(), body));
			orientFlyer(body, from, aim, SPEAR_IN_PLANE_DEGREES, 0.0F);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
				SoundSource.PLAYERS, 0.6F, 0.85F);
	}

	/** 推进所有飞行中的冰长矛。 */
	private static void advanceSpears(MinecraftServer server) {
		if (SPEARS.isEmpty()) {
			return;
		}

		List<Spear> inFlight = new ArrayList<>(SPEARS);
		SPEARS.clear();

		for (Spear spear : inFlight) {
			Spear next = advanceSpear(spear);

			if (next != null) {
				SPEARS.add(next);
			}
		}

		sweepIfIdle(server);
	}

	/**
	 * 把一根长矛往前推一刻；返回 null 表示它已经飞完（或主人掉线）可以收工。
	 *
	 * <p>直线飞行、<b>不做方块检测</b>（用户口径：可以穿墙）；沿途
	 * {@link #SPEAR_RADIUS} 格内的有威胁生物各挨一次（贯穿），飞满
	 * {@link #SPEAR_DISTANCE} 格就散。
	 */
	private static Spear advanceSpear(Spear spear) {
		ServerPlayer owner = spear.level().getServer().getPlayerList().getPlayer(spear.ownerId());

		if (owner == null || !owner.isAlive()) {
			removeBody(spear.body());
			return null;
		}

		double traveled = spear.traveled() + SPEAR_SPEED;
		Vec3 at = spear.from().add(spear.direction().scale(traveled));

		if (spear.body() != null) {
			spear.body().setPos(at.x, at.y, at.z);
		}

		AABB area = new AABB(at, at).inflate(SPEAR_RADIUS);

		for (LivingEntity victim : spear.level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner) || !spear.hit().add(victim.getId())) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(spear.level(), SephiriaDamage.iceAttack(spear.level(), owner), spear.damage());
		}

		if ((int) (traveled / SPEAR_SPEED) % SPEAR_TRAIL_INTERVAL == 0) {
			spear.level().sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 3, 0.2D, 0.2D, 0.2D, 0.03D);
		}

		if (traveled >= SPEAR_DISTANCE) {
			removeBody(spear.body());
			return null;
		}

		return new Spear(spear.level(), spear.ownerId(), spear.from(), spear.direction(), spear.damage(),
				traveled, spear.hit(), spear.body());
	}

	// ------------------------------------------------------------------ 冰弓箭矢（冰弓）

	/**
	 * 放出一支冰弓箭矢：从<b>玩家周围 2 格内的随机位置</b>出发（2026-10-13 用户口径）、
	 * 沿准星方向飞，带一点随机偏角（7 发不会连成一条直线）。
	 */
	private static void launchArrow(ServerPlayer player, ServerLevel level, Vec3 look, double damage) {
		RandomSource random = level.getRandom();
		Vec3 direction = look;

		if (ARROW_SPREAD > 0.0D) {
			double yawJitter = Math.toRadians((random.nextDouble() - 0.5D) * 2.0D * ARROW_SPREAD);
			double pitchJitter = Math.toRadians((random.nextDouble() - 0.5D) * 2.0D * ARROW_SPREAD);
			direction = look.yRot((float) yawJitter).xRot((float) pitchJitter);
		}

		Vec3 from = randomNearSpawn(level, player);
		Display.ItemDisplay body = spawnBody(level, from, ModItems.ICE_ARROW, ARROW_BODY_SCALE);
		ARROWS.add(new IceArrow(level, player.getUUID(), from, direction.normalize(), (float) damage, 0.0D, body));
		orientFlyer(body, from, from.add(direction.scale(ARROW_DISTANCE)), ARROW_IN_PLANE_DEGREES, 0.0F);
	}

	/** 推进所有飞行中的箭矢。 */
	private static void advanceArrows(MinecraftServer server) {
		if (ARROWS.isEmpty()) {
			return;
		}

		List<IceArrow> inFlight = new ArrayList<>(ARROWS);
		ARROWS.clear();

		for (IceArrow arrow : inFlight) {
			IceArrow next = advanceArrow(arrow);

			if (next != null) {
				ARROWS.add(next);
			}
		}

		sweepIfIdle(server);
	}

	/**
	 * 把一支箭往前推一刻；返回 null 表示它已经飞完（或主人掉线 / 打中了 / 撞墙了）可以收工。
	 *
	 * <p>与长矛相反，箭是<b>普通抛射物</b>的口径：会被方块挡住、撞到<b>第一个</b>有威胁生物就结算伤害
	 * 并消失（不贯穿）——用户只给长矛写了「贯穿 / 穿墙」，箭没写就按普通箭来。
	 */
	private static IceArrow advanceArrow(IceArrow arrow) {
		ServerPlayer owner = arrow.level().getServer().getPlayerList().getPlayer(arrow.ownerId());

		if (owner == null || !owner.isAlive()) {
			removeBody(arrow.body());
			return null;
		}

		Vec3 at = arrow.from().add(arrow.direction().scale(arrow.traveled() + ARROW_SPEED));
		double traveled = arrow.traveled() + ARROW_SPEED;
		boolean stop = traveled >= ARROW_DISTANCE;

		// 穿墙：不做方块检测（2026-10-13 用户口径「冰霜武具的伤害可以穿墙（包括弓）」）
		// 撞到第一个有威胁生物：结算伤害、停在这里
		AABB area = new AABB(at, at).inflate(ARROW_RADIUS);

		for (LivingEntity victim : arrow.level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(arrow.level(), SephiriaDamage.iceAttack(arrow.level(), owner), arrow.damage());
			arrow.level().sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 10, 0.25D, 0.25D, 0.25D, 0.06D);
			stop = true;
			break;
		}

		arrow.level().sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);

		if (stop) {
			removeBody(arrow.body());
			return null;
		}

		if (arrow.body() != null) {
			arrow.body().setPos(at.x, at.y, at.z);
		}

		return new IceArrow(arrow.level(), arrow.ownerId(), arrow.from(), arrow.direction(), arrow.damage(),
				traveled, arrow.body());
	}

	// ------------------------------------------------------------------ 锤子

	/** 半径 {@code range} 内的有威胁生物，按距离取最近的 {@code count} 只（锤子与长矛共用）。 */
	private static List<LivingEntity> nearbyTargets(ServerPlayer player, ServerLevel level, double range, int count) {
		AABB area = player.getBoundingBox().inflate(range, range, range);
		return level.getEntitiesOfClass(LivingEntity.class, area).stream()
				.filter(victim -> SephiriaDamage.strikeable(victim, player))
				.sorted(Comparator.comparingDouble(victim -> victim.distanceToSqr(player)))
				.limit(count)
				.toList();
	}

	/**
	 * 扔 {@code count} 把锤子：第 i 把打 {@code victims} 里第 i 近的敌人（敌人比锤子少时绕回来重复打）。
	 *
	 * <p>出手点是<b>玩家周围 2 格内的随机位置</b>（2026-10-13 用户口径：不是从身上飞出来的）。
	 */
	private static void throwHammers(ServerPlayer player, ServerLevel level, List<LivingEntity> victims,
			double damage, int count) {
		for (int index = 0; index < count; index++) {
			LivingEntity victim = victims.get(index % victims.size());
			Vec3 from = randomNearSpawn(level, player);
			Vec3 landing = victim.position().add(0.0D, victim.getBbHeight() * 0.4D, 0.0D);
			HAMMERS.add(new Hammer(level, player.getUUID(), victim, from, landing, (float) damage, 0,
					spawnBody(level, from, ModItems.ICE_HAMMER, BODY_SCALE)));
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
				SoundSource.PLAYERS, 0.7F, 1.1F);
	}

	/**
	 * 造一个飞行本体的物品展示实体：锤子是铁斧、冰长矛是铁长矛、冰弓的箭是箭
	 * （都还没有自己的建模，先用原版物品顶着——用户说的）。满亮、无重力、带清理标签。
	 */
	private static Display.ItemDisplay spawnBody(ServerLevel level, Vec3 at, Item item, float scale) {
		Display.ItemDisplay body = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);

		if (body == null) {
			return null;
		}

		SynchedEntityData data = body.getEntityData();
		// 物品展示的配置与方块展示一样只在私有同步数据里：直接写（访问拓宽已打开这几个字段）
		data.set(Display.ItemDisplay.DATA_ITEM_STACK_ID, new ItemStack(item));
		data.set(Display.ItemDisplay.DATA_ITEM_DISPLAY_ID, (byte) ItemDisplayContext.FIXED.getId());
		data.set(Display.DATA_SCALE_ID, new Vector3f(scale));
		data.set(Display.DATA_BRIGHTNESS_OVERRIDE_ID, BRIGHTNESS_OVERRIDE);
		data.set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, TRANSFORM_INTERPOLATION_TICKS);
		data.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, POS_INTERPOLATION_TICKS);
		body.setPos(at.x, at.y, at.z);
		body.setNoGravity(true);
		body.setSilent(true);
		body.addTag(PROJECTILE_TAG);
		level.addFreshEntity(body);
		return body;
	}

	/** 把飞行中的锤子往前推一刻：先画当前这一帧，飞满了就交接给落地。 */
	private static void advanceHammers(MinecraftServer server) {
		if (HAMMERS.isEmpty()) {
			return;
		}

		List<Hammer> inFlight = new ArrayList<>(HAMMERS);
		HAMMERS.clear();

		for (Hammer hammer : inFlight) {
			Hammer next = advance(hammer);

			if (next != null) {
				HAMMERS.add(next);
			}
		}

		sweepIfIdle(server);
	}

	/** 推进一把锤子一刻；返回 null 表示它已经砸完、可以收工了。 */
	private static Hammer advance(Hammer hammer) {
		Vec3 to = hammer.target().isRemoved() || !hammer.target().isAlive()
				? hammer.fallback()
				: hammer.target().position().add(0.0D, hammer.target().getBbHeight() * 0.4D, 0.0D);
		// 前 FLIGHT_TICKS 刻走完行程（最后一刻正好到站），之后停在落点等那 1 刻插值跟上
		double travel = Math.min(1.0D, hammer.tick() / (double) (FLIGHT_TICKS - 1));
		int duration = FLIGHT_TICKS + FLIGHT_TAIL_TICKS;
		Vec3 at = hammer.from().lerp(to, travel);

		if (hammer.body() != null) {
			hammer.body().setPos(at.x, at.y, at.z);
		}

		orientFlyer(hammer.body(), hammer.from(), to, HAMMER_IN_PLANE_DEGREES,
				(float) (hammer.tick() / (double) (duration - 1) * FLIGHT_SPIN));
		snowTrail(hammer.level(), at, travel);

		if (hammer.tick() + 1 < duration) {
			return new Hammer(hammer.level(), hammer.ownerId(), hammer.target(), hammer.from(), hammer.fallback(),
					hammer.damage(), hammer.tick() + 1, hammer.body());
		}

		smash(hammer, to);
		removeBody(hammer.body());
		return null;
	}

	/**
	 * 摆好本体的朝向：<b>斧刃朝飞行方向、斧面竖直</b>（含飞行方向的那个竖直面），
	 * 再绕「与飞行方向垂直的水平轴」翻滚——也就是斧头被扔出去时的那种前滚，
	 * 而不是原地打转（2026-10-13 用户纠正的口径：刃部对着敌人、面与飞行方向平行、绕那条水平轴转）。
	 *
	 * <p>基准朝向由三级局部旋转拼出来（都是「后乘」的局部旋转，顺序即施加顺序）：
	 * <pre>
	 *     rotateY(yaw) · rotateX(-pitch) · rotateY(+90°)
	 *     yaw   = atan2(dir.x, dir.z)     把局部 +Z 转到飞行方向的水平投影
	 *     pitch = atan2(dir.y, 水平分量)   再抬到真正的飞行方向（抬头/低头挥也对着）
	 * </pre>
	 * 前两级合起来是「局部 +Z → 飞行方向、局部 +Y 尽量朝上」的一套朝向；
	 * <b>最后那个 +90° 是必需的</b>：铁斧的模型父链（item/handheld → item/generated）在
	 * <b>fixed 展示</b>上自带一个 {@code rotateY(180°)}，而展示变换是套在实体旋转<b>里面</b>的
	 * （世界 = 实体旋转 ∘ 模型展示变换 ∘ 模型几何），所以要补偿成正 90° 才能让贴图的「右」
	 * （铁斧贴图里斧刃在头部右侧）真正指向飞行方向。这套数字是用 JOML 逐个方向验算过的：
	 * 局部 +X（斧刃）→ 飞行方向、局部 +Y → 竖直、局部 +Z（斧面法线）→ 水平法线。
	 */
	private static void orientFlyer(Display.ItemDisplay body, Vec3 from, Vec3 to,
			float inPlaneDegrees, float spinDegrees) {
		if (body == null) {
			return;
		}

		Vec3 direction = to.subtract(from);

		if (direction.lengthSqr() < 1.0E-8D) {
			return;
		}

		Vec3 dir = direction.normalize();
		double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		float yaw = (float) Math.atan2(dir.x, dir.z);
		float pitch = (float) Math.atan2(dir.y, horizontal);
		Quaternionf base = new Quaternionf().rotateY(yaw).rotateX(-pitch)
				.rotateY((float) (Math.PI / 2.0D))
				.rotateZ((float) Math.toRadians(inPlaneDegrees));

		// 翻滚轴 = 飞行方向的水平法线（也就是斧面的法线）：绕它转，刃在斧面内转、面本身不动
		Vec3 axis = new Vec3(dir.z, 0.0D, -dir.x);

		if (axis.lengthSqr() < 1.0E-6D) {
			axis = new Vec3(1.0D, 0.0D, 0.0D);
		}

		axis = axis.normalize();
		Quaternionf spin = new Quaternionf().rotateAxis((float) Math.toRadians(spinDegrees),
				(float) axis.x, (float) axis.y, (float) axis.z);
		body.getEntityData().set(Display.DATA_LEFT_ROTATION_ID, spin.mul(base));
	}

	/** 飞行尾迹：越接近落点雪片越多。 */
	private static void snowTrail(ServerLevel level, Vec3 at, double travel) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 2 + (int) (5 * travel),
				0.25D, 0.25D, 0.25D, 0.02D);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1 + (int) (3 * travel),
				0.2D, 0.2D, 0.2D, 0.01D);
	}

	/** 落地：周围一圈的有威胁生物吃一次冰属性伤害 + 一捧雪与碎冰的特效。 */
	private static void smash(Hammer hammer, Vec3 at) {
		ServerLevel level = hammer.level();
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(hammer.ownerId());
		AABB area = new AABB(at, at).inflate(SMASH_RADIUS);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (owner == null ? victim instanceof net.minecraft.world.entity.player.Player
					: !SephiriaDamage.strikeable(victim, owner)) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(level, SephiriaDamage.iceAttack(level, owner), hammer.damage());
		}

		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.2D, at.z, 22, 0.5D, 0.35D, 0.5D, 0.08D);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3D, at.z, 10, 0.4D, 0.3D, 0.4D, 0.05D);
		level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0.72F, 0.92F, 1.0F),
				at.x, at.y + 0.4D, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		ring(level, at);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 0.9F, 0.9F);
	}

	/** 贴地的雪圈：一圈往外推的雪片，落地那一下才看得见的「震荡」。 */
	private static void ring(ServerLevel level, Vec3 at) {
		for (int index = 0; index < 14; index++) {
			double angle = Math.PI * 2.0D * index / 14;
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					at.x + Math.cos(angle) * 2.0D, at.y + 0.15D, at.z + Math.sin(angle) * 2.0D,
					1, 0.1D, 0.05D, 0.1D, 0.05D);
		}
	}

	/** 收掉本体。 */
	private static void removeBody(Display.ItemDisplay body) {
		if (body != null) {
			body.discard();
		}
	}

	/** 三张飞行表都空了才值得扫一次残骸（每批飞完扫一遍，别在还在飞的时候把同伴清掉）。 */
	private static void sweepIfIdle(MinecraftServer server) {
		if (!HAMMERS.isEmpty() || !SPEARS.isEmpty() || !ARROWS.isEmpty()) {
			return;
		}

		sweepOrphans(server);
	}

	/**
	 * 清扫残留的本体：演出表是内存里的，崩服恰好卡在飞行中途时本体（铁斧 / 铁长矛 / 箭）会留在世界里。
	 * 这些东西都发生在玩家附近，所以每批飞完就按标签扫一圈玩家周围的本体。
	 */
	private static void sweepOrphans(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			List<Display.ItemDisplay> orphans = level.getEntitiesOfClass(Display.ItemDisplay.class,
					player.getBoundingBox().inflate(SWEEP_RADIUS),
					body -> body.entityTags().contains(PROJECTILE_TAG));

			for (Display.ItemDisplay orphan : orphans) {
				orphan.discard();
			}
		}
	}
}
