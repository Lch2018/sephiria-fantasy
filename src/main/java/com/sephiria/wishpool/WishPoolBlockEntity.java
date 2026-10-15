package com.sephiria.wishpool;

import com.sephiria.registry.ModBlockEntities;
import com.sephiria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 许愿池方块实体：<b>只干一件事</b>——把扔进来的「升级宝箱」像漏斗一样吸走，每吸一个给玩家一片四叶草。
 *
 * <p>用户口径（2026-10-15）：「四叶草的获取方式为向许愿池中丢入升级宝箱，许愿池会像漏斗一样吸走升级宝箱，
 * 每一个升级宝箱为玩家提供一个四叶草」。所以这里<b>不存任何数据</b>（能量与锚定都是玩家自己的，
 * 见 {@link WishPool}），方块实体存在的意义只是有个 ticker 能吸东西。
 *
 * <p>吸的过程：每刻把范围内的升级宝箱往池心拉（速度给得不大，看着像被吸过去），
 * 拉到足够近就吃掉、给主人一片四叶草、来一声水花。宝箱主人（扔它的玩家）不在线 / 没有主人时，
 * 找附近最近的玩家；<b>附近一个玩家都没有就先不吸</b>——不然宝箱白没了、四叶草也没人拿。
 */
public class WishPoolBlockEntity extends BlockEntity {
	/** 吸走范围（格，以方块为中心各方向）。 */
	private static final double SUCK_RANGE = 3.0D;
	/** 拉到多近算「吸进去了」（格）。 */
	private static final double CONSUME_RANGE = 0.9D;
	/** 每刻往池心拉的速度（格/刻）：慢一点才有「被吸过去」的观感。 */
	private static final double SUCK_SPEED = 0.12D;
	/** 没有主人时，找这个范围内的最近玩家给四叶草（格）。 */
	private static final double OWNER_RANGE = 8.0D;

	public WishPoolBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WISH_POOL, pos, state);
	}

	/** 方块 ticker（{@code WishPoolBlock#getTicker} 挂上来的）。 */
	public static void serverTick(Level level, BlockPos pos, BlockState state, WishPoolBlockEntity pool) {
		Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.35D, 0.0D);
		AABB area = new AABB(pos).inflate(SUCK_RANGE);

		for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, area)) {
			if (!drop.isAlive() || !drop.getItem().is(ModItems.UPGRADE_CHEST)) {
				continue;
			}

			Vec3 to = center.subtract(drop.position());
			double distance = to.length();

			if (distance <= CONSUME_RANGE) {
				swallow(level, pos, drop);
				continue;
			}

			// 往池心拉：速度不大、竖直方向再多给一点，看着像被「吸上去」
			Vec3 pull = to.normalize().scale(SUCK_SPEED).add(0.0D, 0.04D, 0.0D);
			drop.setDeltaMovement(pull);
			// 速度是服务端算的，要标一下才会同步给客户端（不然客户端还在按旧速度插值）
			drop.needsSync = true;
		}
	}

	/** 吸进去：宝箱消失、主人得一片四叶草、来点水花。 */
	private static void swallow(Level level, BlockPos pos, ItemEntity drop) {
		ServerPlayer owner = ownerOf(level, pos, drop);

		if (owner == null) {
			// 附近没人：先留着，等有人来再吸（不然四叶草没人拿）
			return;
		}

		drop.discard();
		WishPool.giveClover(owner);

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
					12, 0.3D, 0.2D, 0.3D, 0.05D);
			serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5D, pos.getY() + 1.0D,
					pos.getZ() + 0.5D, 6, 0.25D, 0.15D, 0.25D, 0.02D);
		}

		level.playSound(null, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
				SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.4F);
	}

	/** 宝箱的主人（扔它的那个玩家）；没有主人就找附近最近的玩家。 */
	private static ServerPlayer ownerOf(Level level, BlockPos pos, ItemEntity drop) {
		if (drop.getOwner() instanceof ServerPlayer owner && owner.isAlive()) {
			return owner;
		}

		Player nearest = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
				OWNER_RANGE, false);

		return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
	}
}
