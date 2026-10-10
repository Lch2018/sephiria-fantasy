package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 萤火虫（魔法科技，高级品质）：【唯一】闪电属性伤害 +4/8/12，受到攻击时 6 秒内禁用（0..2 级）。
 *
 * <p>「禁用」是整件神器的电元素强度加成失效 6 秒：状态按玩家记在内存里，聚合处
 * （{@code ArtifactEffects#lightningElementBonus}）按它决定要不要算这一份。
 * 面板靠每秒指纹对账，最多晚一秒刷新。
 */
public class FireflyItem extends ArtifactItem {
	/** 各等级的电元素强度加成（点）。 */
	private static final double[] LIGHTNING_BY_LEVEL = { 4.0D, 8.0D, 12.0D };
	/** 受到攻击后的禁用时长（tick）：6 秒。 */
	private static final int SUPPRESS_TICKS = 120;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;
	/** 禁用状态：玩家 → 禁用到的刻。 */
	private static final Map<UUID, Long> SUPPRESSED_UNTIL = new HashMap<>();

	public FireflyItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.firefly.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return LIGHTNING_BY_LEVEL.length - 1;
	}

	@Override
	public double fireflyLightningElement(int level) {
		return valueAt(LIGHTNING_BY_LEVEL, level);
	}

	/** 玩家此刻是否处于「受伤禁用」窗口里（聚合处用它决定要不要算萤火虫那一份）。 */
	public static boolean isSuppressed(ServerPlayer player) {
		Long until = SUPPRESSED_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	/** 挂在伤害事件上：玩家挨打（实扣 > 0）就把萤火虫禁用 6 秒。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken > 0.0F && entity instanceof ServerPlayer player) {
				SUPPRESSED_UNTIL.put(player.getUUID(), player.level().getGameTime() + SUPPRESS_TICKS);
			}
		});
	}

	/** 玩家退出时清状态表。 */
	public static void forget(ServerPlayer player) {
		SUPPRESSED_UNTIL.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.firefly_element",
				Component.literal(Numbers.format(this.fireflyLightningElement(level))).withColor(COLOUR_BONUS)));
	}
}
