package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：许愿池界面里点的按钮。
 *
 * <p>五种动作：
 * <ul>
 *   <li>{@link #ACTION_DRAW_SINGLE} / {@link #ACTION_DRAW_TEN}：抽卡（1 次 / 10 次，每次 1 片四叶草）；</li>
 *   <li>{@link #ACTION_ANCHOR}：把某一卡池的锚定设成 {@code itemId}（0 = 取消锚定）；</li>
 *   <li>{@link #ACTION_FORGE}：神铸——花能量换 {@code itemId}；</li>
 *   <li>{@link #ACTION_TOGGLE_OVERFLOW}：翻转「溢出的升级宝箱自动转化为四叶草」开关。</li>
 * </ul>
 *
 * <p>{@code slatePool} 表示当前在石板池（抽卡与锚定都分池）。
 * 物品用注册表数字 id 传（0 = 空 / 取消），实际能不能换、够不够能量都由服务端算。
 */
public record WishPoolClickPayload(int action, boolean slatePool, int itemId) implements CustomPacketPayload {
	public static final int ACTION_DRAW_SINGLE = 0;
	public static final int ACTION_DRAW_TEN = 1;
	public static final int ACTION_ANCHOR = 2;
	public static final int ACTION_FORGE = 3;
	public static final int ACTION_TOGGLE_OVERFLOW = 4;
	/** 清掉指定的一个锚定格（itemId 当作格子下标用）。 */
	public static final int ACTION_CLEAR_ANCHOR_SLOT = 5;

	public static final CustomPacketPayload.Type<WishPoolClickPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool_click"));

	public static final StreamCodec<FriendlyByteBuf, WishPoolClickPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, WishPoolClickPayload payload) -> {
				buffer.writeVarInt(payload.action());
				buffer.writeBoolean(payload.slatePool());
				buffer.writeVarInt(payload.itemId());
			},
			(FriendlyByteBuf buffer) -> new WishPoolClickPayload(
					buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt()));

	/** 单抽 / 十连。 */
	public static WishPoolClickPayload draw(boolean slatePool, boolean ten) {
		return new WishPoolClickPayload(ten ? ACTION_DRAW_TEN : ACTION_DRAW_SINGLE, slatePool, 0);
	}

	/** 锚定（{@code itemId} = 0 表示清空这个池子的全部锚定）。 */
	public static WishPoolClickPayload anchor(boolean slatePool, int itemId) {
		return new WishPoolClickPayload(ACTION_ANCHOR, slatePool, itemId);
	}

	/** 清掉某个池子的第 {@code slot} 个锚定格。 */
	public static WishPoolClickPayload clearAnchorSlot(boolean slatePool, int slot) {
		return new WishPoolClickPayload(ACTION_CLEAR_ANCHOR_SLOT, slatePool, slot);
	}

	/** 神铸。 */
	public static WishPoolClickPayload forge(int itemId) {
		return new WishPoolClickPayload(ACTION_FORGE, false, itemId);
	}

	/** 翻转「溢出的升级宝箱自动转化为四叶草」开关（不带别的参数，值与反转都由服务端管）。 */
	public static WishPoolClickPayload toggleOverflow() {
		return new WishPoolClickPayload(ACTION_TOGGLE_OVERFLOW, false, 0);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
