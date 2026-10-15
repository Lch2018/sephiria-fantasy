package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：在根之退界面里做的操作。
 *
 * <p>三种动作（见 {@code difficulty.RootRetreatMenu} 的 adjust / clear / apply）：
 * 调整某一项的等级、清空当前方块那份方案、把当前方块那份方案应用到自己身上。
 *
 * <p>只带「第几项 + 方向」这种最小信息：等级上下限、能不能改全由服务端算
 * （{@code RootRetreatBlockEntity.setLevel} / {@code RootRetreat.applyPlan}），客户端说的不算。
 */
public record RootRetreatClickPayload(int action, int option, int delta) implements CustomPacketPayload {
	/** 调整某一项（option + delta）。 */
	public static final int ACTION_ADJUST = 0;
	/** 清空当前方块那份方案。 */
	public static final int ACTION_CLEAR = 1;
	/** 把当前方块那份方案应用到自己身上。 */
	public static final int ACTION_APPLY = 2;

	public static final CustomPacketPayload.Type<RootRetreatClickPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "root_retreat_click"));

	public static final StreamCodec<FriendlyByteBuf, RootRetreatClickPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, RootRetreatClickPayload payload) -> {
				buffer.writeVarInt(payload.action());
				buffer.writeVarInt(payload.option());
				buffer.writeVarInt(payload.delta());
			},
			(FriendlyByteBuf buffer) -> new RootRetreatClickPayload(
					buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

	/** 加减号。 */
	public static RootRetreatClickPayload adjust(int option, int delta) {
		return new RootRetreatClickPayload(ACTION_ADJUST, option, delta);
	}

	/** 「清空」按钮。 */
	public static RootRetreatClickPayload clear() {
		return new RootRetreatClickPayload(ACTION_CLEAR, 0, 0);
	}

	/** 「应用」按钮。 */
	public static RootRetreatClickPayload apply() {
		return new RootRetreatClickPayload(ACTION_APPLY, 0, 0);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
