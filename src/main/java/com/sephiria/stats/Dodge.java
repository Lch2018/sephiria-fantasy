package com.sephiria.stats;

import com.sephiria.ability.DashSkill;
import com.sephiria.ability.SkillStorage;
import com.sephiria.artifact.ArtifactEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 闪避：玩家受到伤害时先掷一次，闪掉就整个无视这次伤害。
 *
 * <p>只对「有来源实体的伤害」生效——摔落、着火、饥饿这类环境伤害不参与闪避（能闪掉摔伤太怪了）。
 * 闪避率由 {@link PlayerStats#dodgeRatePercent} 给出，判定与暴击共用伪随机分布（连着没闪掉会
 * 逐次提高几率）。
 *
 * <p>判定放在 {@code ALLOW_DAMAGE} 而不是改数值：闪避的语义是「这一下完全不存在」，用取消事件
 * 实现最干净——不改数值、也不留无敌帧。
 */
public final class Dodge {
	/** 音效节流：连着挨打时不要每次都响。 */
	private static final int SOUND_COOLDOWN_TICKS = 10;

	private static final Map<UUID, Long> LAST_SOUND = new HashMap<>();

	private Dodge() {
	}

	/** 注册（由 {@link com.sephiria.Sephiria#onInitialize()} 调用，排在武器自己的减伤/格挡之后）。 */
	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || source.getEntity() == null) {
				return true;
			}

			double rate = PlayerStats.dodgeRatePercent(player);

			if (rate <= 0.0D || !PseudoRandom.dodge(player, rate)) {
				return true;
			}

			// 闪避触发：弹力带那类词条在这里回冲刺次数
			double restore = ArtifactEffects.dodgeRestoreCharges(player);

			if (restore > 0.0D) {
				SkillStorage.regenerate(player, DashSkill.STORAGE, restore);
			}

			long now = player.level().getGameTime();
			Long last = LAST_SOUND.get(player.getUUID());

			if (last == null || now - last >= SOUND_COOLDOWN_TICKS) {
				LAST_SOUND.put(player.getUUID(), now);
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.6F);
			}

			return false;
		});
	}

	/** 玩家退出时清掉音效节流表。 */
	public static void forget(ServerPlayer player) {
		LAST_SOUND.remove(player.getUUID());
	}
}
