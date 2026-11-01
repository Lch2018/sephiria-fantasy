package com.sephiria.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.talent.Talent;
import com.sephiria.talent.Talents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 商店的免费刷新次数（天赋「智慧」三阶段）。
 *
 * <p>三阶段首次生效时 +1 次；关掉再打开商店不会重新发。用掉一次后，
 * 使用骰子刷新会把「下一次免费」补回来（用户 2026-11-01 口径）。
 */
public final class ShopFreeRerolls {
	private record Saved(int free, boolean granted) {
		private static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("free", 0).forGetter(Saved::free),
				Codec.BOOL.optionalFieldOf("granted", false).forGetter(Saved::granted)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "shop_free_rerolls"));

	private ShopFreeRerolls() {
	}

	/** 当前可用免费刷新数；第一次达到智慧三阶段时发 1 次。 */
	public static int available(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			saved = new Saved(0, false);
		}

		if (!saved.granted() && Talents.stage(player, Talent.WISDOM) >= 3) {
			saved = new Saved(saved.free() + 1, true);
			player.setAttached(SAVED, saved);
		}

		return Math.max(0, saved.free());
	}

	public static void consume(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);

		if (saved != null && saved.free() > 0) {
			player.setAttached(SAVED, new Saved(saved.free() - 1, saved.granted()));
		}
	}

	/** 用骰子刷新后，把下一次刷新变成免费。 */
	public static void grantNext(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			saved = new Saved(0, false);
		}

		player.setAttached(SAVED, new Saved(Math.max(1, saved.free()), saved.granted()));
	}
}
