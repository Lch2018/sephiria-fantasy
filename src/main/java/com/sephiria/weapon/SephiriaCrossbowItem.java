package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.SkillStorage;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.network.SkillSyncPayload;
import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重型弩：弹匣式连发，弹药是弩自己的弩矢（{@link SephiriaBoltItem}）。
 *
 * <p><b>弹匣用技能存储系统</b>（{@link SkillStorage}）：
 * <ul>
 *   <li>存储总量 = {@value #MAGAZINE_SIZE}（弹匣容量），每次射击消耗 {@value #SHOT_COST}；</li>
 *   <li>按 {@code R} 装填：当现有存储量没满时才允许，装填耗时 {@value #RELOAD_TICKS} tick（2 秒），
 *       完成时调用存储的回复动作，回复量 = 存储总量（也就是一次装满）。</li>
 * </ul>
 *
 * <p>射击间隔由 {@value #FIRE_COOLDOWN_TICKS} tick 的物品冷却控制（攻速 3 = 每秒 3 发，
 * 20/3 ≈ 6.67 取整到 7）；装填期间物品同样处于冷却，模型由原版 {@code minecraft:cooldown}
 * 进度驱动播放 {@code colossal_crossbow_reload_*} 动画帧。
 *
 * <p>弩不消耗、也不要求背包里有原版箭矢，装填不消耗任何物品。物品上的
 * {@code charged_projectiles} 组件只是<b>视觉</b>用的（弩身显示已装填的弩矢数），
 * 真正的弹药数在技能存储里，每次射击/装填后按存储量同步一次。
 */
public class SephiriaCrossbowItem extends CrossbowItem implements SephiriaWeapon {
	/** 弹药在技能存储里的 id（HUD 也读它）。 */
	public static final Identifier STORAGE = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "crossbow");
	/** 备用弹匣数量（HUD 的「（m）」读它）；不是 SkillStorage 的自动池，由本类手动同步。 */
	public static final Identifier MAGAZINES = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "crossbow_magazines");

	/** 弹匣容量，同时是存储总量。 */
	public static final int MAGAZINE_SIZE = 10;
	/** 每次射击的消耗量。 */
	public static final double SHOT_COST = 1.0D;
	/** 装填耗时（tick）：2 秒。 */
	public static final int RELOAD_TICKS = 40;
	/** 射击间隔（tick）：攻速 3 → 20/3 取整。 */
	public static final int FIRE_COOLDOWN_TICKS = 4;
	/** 近战数值：弩没有近战加成，等同于空手（伤害 1、攻速 4）。 */
	public static final float MELEE_DAMAGE = 1.0F;
	public static final float MELEE_ATTACK_SPEED = 4.0F;
	/** 与原版弩一致的出膛速度。 */
	private static final float ARROW_SPEED = 3.15F;

	/** 正在装填的玩家：记录装填结束的游戏刻。 */
	private static final Map<UUID, Long> RELOADS = new ConcurrentHashMap<>();
	/** 备用弹匣数（不含当前这一个）；缺省 = 最大弹匣数 - 1。 */
	private static final Map<UUID, Integer> RESERVE_MAGAZINES = new ConcurrentHashMap<>();
	/** 「装填完成」后的弩伤害 buff 到期刻。 */
	private static final Map<UUID, Long> RELOAD_BUFF_UNTIL = new ConcurrentHashMap<>();
	/** 装填完成后 10 秒（200 刻）的弩伤害 buff。 */
	private static final int RELOAD_BUFF_TICKS = 200;
	/** HUD 备用弹匣同步间隔（20 刻 = 1 秒）。 */
	private static int magazineSyncTicker;

	private final WeaponBranch branch;

	public SephiriaCrossbowItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	@Override
	public java.util.List<Component> detailLines(ItemStack stack) {
		return java.util.List.of(
				// 弩没有近战加成（等同空手），远程也不触发横扫，所以这一行不写范围
				Line.attackRanged(MELEE_DAMAGE, MELEE_ATTACK_SPEED).build(),
				Line.titled("tooltip.sephiria_fantasy.skill.fire")
						.damage(SephiriaBoltArrow.FIXED_DAMAGE).cooldownTicks(FIRE_COOLDOWN_TICKS)
						.stat("tooltip.sephiria_fantasy.part.magazine", MAGAZINE_SIZE, COLOR_RANGE).build(),
				desc("tooltip.sephiria_fantasy.skill.fire", "tooltip.sephiria_fantasy.desc.fire"),
				Line.titled("tooltip.sephiria_fantasy.skill.reload")
						.stat("tooltip.sephiria_fantasy.part.reload", RELOAD_TICKS / 20.0D, COLOR_COOLDOWN).build(),
				desc("tooltip.sephiria_fantasy.skill.reload", "tooltip.sephiria_fantasy.desc.reload"));
	}


	/** 注册存储配置与装填推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		// 弹匣不随时间自动回复：只有 R 装填完成时才调 regenerate 加回整个弹匣
		SkillStorage.registerManual(STORAGE, MAGAZINE_SIZE);
		ServerTickEvents.END_SERVER_TICK.register(SephiriaCrossbowItem::tickReloads);
		ServerTickEvents.END_SERVER_TICK.register(SephiriaCrossbowItem::tickMagazines);
	}

	/** 弹匣里现有的弹药数（读技能存储，只在服务端有意义）。 */
	public static int ammoCount(ServerPlayer player) {
		return (int) Math.round(SkillStorage.current(player, STORAGE));
	}

	/** 最大弹匣数：默认 1 + 神器「装填用木箱坚固」的加成。 */
	public static int maxMagazines(ServerPlayer player) {
		return 1 + ArtifactEffects.maxMagazinesBonus(player);
	}

	/** 备用弹匣数（不含当前弹匣）；缺省值 = 最大弹匣数 - 1。 */
	public static int reserveMagazines(ServerPlayer player) {
		int max = maxMagazines(player);
		int value = RESERVE_MAGAZINES.getOrDefault(player.getUUID(), max - 1);
		int clamped = Math.max(0, Math.min(max - 1, value));
		RESERVE_MAGAZINES.put(player.getUUID(), clamped);
		return clamped;
	}

	/** 把备用弹匣数推给客户端（HUD 的「（m）」）。 */
	public static void syncMagazines(ServerPlayer player) {
		ServerPlayNetworking.send(player,
				new SkillSyncPayload(MAGAZINES, reserveMagazines(player), maxMagazines(player)));
	}

	/** 当前实际装填刻数：受「神射手的曲柄」重装速度加成。 */
	public static int reloadTicks(ServerPlayer player) {
		double bonus = ArtifactEffects.crossbowReloadSpeedPercent(player);
		return Math.max(1, (int) Math.ceil(RELOAD_TICKS * 100.0D / (100.0D + bonus)));
	}

	/**
	 * 当前弩矢伤害加成（%）：神器常驻部分 + 装填完成后的 10 秒 buff。
	 *
	 * <p>只给 {@link SephiriaBoltArrow} 读；近战部分不受影响。
	 */
	public static double damageBonusPercent(ServerPlayer player) {
		double total = ArtifactEffects.crossbowDamagePercent(player);
		Long until = RELOAD_BUFF_UNTIL.get(player.getUUID());

		if (until != null && player.level().getGameTime() < until) {
			total += ArtifactEffects.crossbowReloadBuffPercent(player);
		}

		return total;
	}

	/** 每秒同步一次备用弹匣；拿着弩的玩家才需要。 */
	private static void tickMagazines(MinecraftServer server) {
		if (++magazineSyncTicker % 20 != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (heldCrossbow(player) != null) {
				syncMagazines(player);
			}
		}
	}

	/** 玩家退出时清掉本类的运行时表。 */
	public static void forget(ServerPlayer player) {
		RESERVE_MAGAZINES.remove(player.getUUID());
		RELOAD_BUFF_UNTIL.remove(player.getUUID());
		RELOADS.remove(player.getUUID());
	}

	/** 右击：有弹药就射一发。空匣什么都不做，装填走 R 键。 */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack crossbow = player.getItemInHand(hand);

		if (player.getCooldowns().isOnCooldown(crossbow)) {
			return InteractionResult.FAIL;   // 射击间隔中 / 装填中
		}

		if (level.isClientSide()) {
			return InteractionResult.CONSUME;
		}

		ServerPlayer serverPlayer = (ServerPlayer) player;

		if (!SkillStorage.consume(serverPlayer, STORAGE, SHOT_COST)) {
			return InteractionResult.CONSUME;   // 空匣
		}

		fire(serverPlayer, crossbow);
		player.getCooldowns().addCooldown(crossbow, FIRE_COOLDOWN_TICKS);
		return InteractionResult.CONSUME;
	}

	/**
	 * 按 R 装填（仅服务端调用）。
	 *
	 * <p>现有存储量已经满了就不做事；否则记下结束时刻并进入物品冷却，冷却结束时由
	 * {@link #tickReloads} 触发一次"回复存储总量"。
	 */
	public static void tryReload(ServerPlayer player) {
		// 弹匣满、备用弹匣也满时才没事可做；自动换弹后备用是 0，按 R 可以再补一个备用弹匣
		if (SkillStorage.current(player, STORAGE) >= MAGAZINE_SIZE
				&& reserveMagazines(player) >= maxMagazines(player) - 1) {
			return;
		}

		ItemStack crossbow = heldCrossbow(player);

		if (crossbow == null) {
			return;
		}

		int ticks = reloadTicks(player);
		RELOADS.put(player.getUUID(), player.level().getGameTime() + ticks);
		player.getCooldowns().addCooldown(crossbow, ticks);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	/** 装填推进：到点了就回复整个弹匣，并同步一次视觉组件。 */
	private static void tickReloads(MinecraftServer server) {
		if (RELOADS.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		Iterator<Map.Entry<UUID, Long>> iterator = RELOADS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Long> entry = iterator.next();

			if (entry.getValue() > now) {
				continue;
			}

			iterator.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			if (player == null) {
				continue;
			}

			SkillStorage.regenerate(player, STORAGE, MAGAZINE_SIZE);
			// 装填完成：备用弹匣补满到「最大弹匣数 - 1」；同时触发曲柄的 10 秒伤害 buff
			RESERVE_MAGAZINES.put(player.getUUID(), Math.max(0, maxMagazines(player) - 1));
			RELOAD_BUFF_UNTIL.put(player.getUUID(), now + RELOAD_BUFF_TICKS);
			syncMagazines(player);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);

			ItemStack crossbow = heldCrossbow(player);

			if (crossbow != null) {
				syncDisplay(crossbow, ammoCount(player));
			}
		}
	}

	/** 射出一发（仅服务端调用）；打空当前弹匣时自动换下一个备用弹匣。 */
	private static void fire(ServerPlayer player, ItemStack crossbow) {
		ServerLevel level = (ServerLevel) player.level();
		shoot(level, player, crossbow, new ItemStack(ModItems.CROSSBOW_BOLT));
		syncDisplay(crossbow, ammoCount(player));

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

		if (ammoCount(player) <= 0) {
			int reserve = reserveMagazines(player);

			if (reserve > 0) {
				// 备用弹匣换弹：不耗装填时间、不触发「装填完成」buff
				RESERVE_MAGAZINES.put(player.getUUID(), reserve - 1);
				SkillStorage.regenerate(player, STORAGE, MAGAZINE_SIZE);
				syncDisplay(crossbow, MAGAZINE_SIZE);
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.7F, 1.2F);
			}

			syncMagazines(player);
		}
	}

	private static void shoot(ServerLevel level, ServerPlayer player, ItemStack crossbow, ItemStack projectileStack) {
		if (!(projectileStack.getItem() instanceof ArrowItem arrowItem)) {
			return;
		}
		AbstractArrow arrow = arrowItem.createArrow(level, projectileStack, player, crossbow);
		arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ARROW_SPEED, 1.0F);
		arrow.setCritArrow(true);
		level.addFreshEntity(arrow);
	}

	/** 玩家主手/副手拿着的弩，没有就返回 null。 */
	private static ItemStack heldCrossbow(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);

			if (stack.getItem() instanceof SephiriaCrossbowItem) {
				return stack;
			}
		}

		return null;
	}

	/**
	 * 把显示对齐到存储量：{@code charged_projectiles} 组件决定弩身是否显示已装填的弩矢，
	 * 同时也是耐久条（弹匣条）读的数据。
	 */
	private static void syncDisplay(ItemStack crossbow, int count) {
		List<ItemStack> ammo = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			ammo.add(new ItemStack(ModItems.CROSSBOW_BOLT));
		}

		crossbow.set(DataComponents.CHARGED_PROJECTILES,
				ammo.isEmpty() ? ChargedProjectiles.EMPTY : ChargedProjectiles.ofNonEmpty(ammo));
	}

	// ------------------------------------------------------------------ 耐久条 = 弹匣条
	//
	// 赛菲莉娅武器都带 UNBREAKABLE，而 26.2 的 ItemStack#isDamageableItem() 明确排除了
	// 不可破坏的物品，于是 isDamaged() 永远为 false、原版耐久条不会绘制。所以这里直接重写
	// 耐久条的三个方法，让它读弹匣数而不是 damage 组件。

	/** 弩始终显示弹匣条（这是它的弹药指示器，不像原版那样只在受损时出现）。 */
	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	/** 条的长度 = 弹匣余量（原版是 13 像素满格）。 */
	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * visualAmmoCount(stack) / (float) MAGAZINE_SIZE);
	}

	/** 沿用原版的绿→红渐变：满弹绿、快空红。 */
	@Override
	public int getBarColor(ItemStack stack) {
		float ratio = (float) visualAmmoCount(stack) / MAGAZINE_SIZE;
		return Mth.hsvToRgb(Mth.clamp(ratio, 0.0F, 1.0F) / 3.0F, 1.0F, 1.0F);
	}

	/** 视觉上的弩矢数（供提示框、耐久条等只读用途）。 */
	public static int visualAmmoCount(ItemStack crossbow) {
		return crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).size();
	}
}
