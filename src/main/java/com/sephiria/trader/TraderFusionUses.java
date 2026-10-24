package com.sephiria.trader;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛菲莉娅流浪商人的「石板合成」使用记录：**每个商人、每个玩家各一次**（用户 2026-10-24 定）。
 *
 * <p>记录写在<b>商人实体</b>上而不是玩家身上：商人死了 / 消失，这条记录也就没有意义；
 * 玩家换一只商人仍然可以再用一次。存的是玩家 UUID 字符串的列表，跟着实体存档走
 * （Fabric 的持久化附件），重进世界 / 重新加载区块都不会丢。
 *
 * <p>「使用」按<b>成功合成一次</b>算，不是打开界面——玩家打开合成界面看看、没合成就关掉，
 * 不会消耗这一次机会（见 {@code SlateFusionMenu#fuse}）。
 */
public final class TraderFusionUses {
	/** 存档：这只商人已经用过石板合成的玩家 UUID。 */
	public record Saved(List<String> players) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.listOf().optionalFieldOf("players", List.of()).forGetter(Saved::players)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> USED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "trader_fusion_uses"));

	private TraderFusionUses() {
	}

	/** 在模组初始化时调一次，确保附件在实体加载之前已经注册。 */
	public static void register() {
	}

	/** 这个玩家在这只商人身上用过石板合成了吗。 */
	public static boolean used(Entity trader, Player player) {
		Saved saved = trader.getAttached(USED);
		return saved != null && saved.players().contains(player.getStringUUID());
	}

	/** 记下「这个玩家在这只商人身上合成了石板」；重复调用不会写出重复 UUID。 */
	public static void mark(Entity trader, Player player) {
		String id = player.getStringUUID();
		Saved saved = trader.getAttached(USED);
		List<String> players = saved == null ? new ArrayList<>() : new ArrayList<>(saved.players());

		if (!players.contains(id)) {
			players.add(id);
			trader.setAttached(USED, new Saved(List.copyOf(players)));
		}
	}
}
