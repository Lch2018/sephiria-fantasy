package com.sephiria.client.beacon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.beacon.BeaconLoot;
import com.sephiria.beacon.SephiriaBeaconBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲莉娅信标的光柱。
 *
 * <p>1.20.1 没有 26.x 的 {@code BeaconBeamOwner} 接口，原版 {@code BeaconRenderer} 也只认
 * {@code BeaconBlockEntity}；这里给自己的方块实体写一个 BER，直接复用原版公开的
 * {@link BeaconRenderer#renderBeaconBeam}（同一张 BEAM 贴图、0.2 半径 / 0.25 外晕），
 * 所以光柱的观感和原版信标一致。颜色取容器里那件物品的品质色，空容器不画。
 */
public class SephiriaBeaconRenderer implements BlockEntityRenderer<SephiriaBeaconBlockEntity> {
	/** 与原版信标一样的完整光柱高度（原版最后一段固定 1024）。 */
	private static final int BEAM_HEIGHT = 1024;

	public SephiriaBeaconRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(SephiriaBeaconBlockEntity beacon, float partialTick, PoseStack pose,
			MultiBufferSource buffer, int light, int overlay) {
		ItemStack held = beacon.heldItem();

		if (held.isEmpty() || beacon.getLevel() == null) {
			return;
		}

		int color = BeaconLoot.beamColor(held);
		float[] rgb = {
				((color >> 16) & 0xFF) / 255.0F,
				((color >> 8) & 0xFF) / 255.0F,
				(color & 0xFF) / 255.0F
		};

		BeaconRenderer.renderBeaconBeam(pose, buffer, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
				beacon.getLevel().getGameTime(), 0, BEAM_HEIGHT, rgb, 0.2F, 0.25F);
	}

	/** 光柱很高，别让原版的方块剔除把它裁掉。 */
	@Override
	public boolean shouldRenderOffScreen(SephiriaBeaconBlockEntity beacon) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}
}
