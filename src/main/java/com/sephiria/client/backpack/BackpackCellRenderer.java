package com.sephiria.client.backpack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.slate.SlateItem;
import com.sephiria.util.Numbers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 背包格子的通用绘制，赛菲莉娅背包界面与神器附魔界面共用，保证两边的格子看起来一样。
 *
 * <p>两件事：旋转的石板要按朝向把图标转过来（不然玩家看不出它朝哪边）；
 * 左上角要画等级文字（神器是「实际等级/上限」，空格子是石板加出来的格子等级）。
 *
 * <p>1.21.1 的 {@code GuiGraphics} 没有 26.3 那层「收集渲染状态」的拆分，
 * 这里直接用 {@code renderItem} 画物品、用 {@code PoseStack} 缩放文字；
 * 两个方法都假定调用方处在「已平移到面板左上角」的坐标系里（原版 {@code renderSlot} 就是这样）。
 */
public final class BackpackCellRenderer {
	/** 格子等级文字：正数绿、负数红。 */
	private static final int SLOT_LEVEL_POSITIVE = 0xFF55FF55;
	private static final int SLOT_LEVEL_NEGATIVE = 0xFFFF5555;
	/** 格子里的等级文字缩放：缩到 0.6 倍，免得盖住图标。 */
	private static final float LEVEL_TEXT_SCALE = 0.6F;
	/** 神器等级文字：失效红、未满白、满级绿、超上限黄。 */
	private static final int LEVEL_INACTIVE = 0xFFFF5555;
	private static final int LEVEL_NORMAL = 0xFFFFFFFF;
	private static final int LEVEL_MAX = 0xFF55FF55;
	private static final int LEVEL_OVER = 0xFFFFD24A;

	private BackpackCellRenderer() {
	}

	/** 旋转的石板：绕着格子中心把图标转过来（自己画，调用方就别再调原版的画法了）。 */
	public static void drawRotatedItem(GuiGraphics graphics, Slot slot, ItemStack stack) {
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(slot.x + 8.0F, slot.y + 8.0F, 0.0F);
		// 屏幕坐标 y 向下：负角度才是视觉上的逆时针，与 SlateItem#rotate 的方向一致
		pose.mulPose(Axis.ZP.rotation(-SlateItem.rotationOf(stack) * ((float) Math.PI / 2.0F)));
		pose.translate(-8.0F, -8.0F, 0.0F);
		graphics.renderItem(stack, 0, 0);
		pose.popPose();
	}

	/** 格子左上角的等级文字（0.6 倍大小）。 */
	public static void drawLevelText(GuiGraphics graphics, Font font, Slot slot, ItemStack stack,
			int slotLevel) {
		Component text;
		int colour;

		if (stack.getItem() instanceof SephiriaArtifact artifact) {
			int level = ArtifactEffects.effectiveLevel(stack, slotLevel);
			int max = artifact.maxLevel();

			if (level < 0) {
				colour = LEVEL_INACTIVE;
			} else if (level > max) {
				colour = LEVEL_OVER;
			} else if (level == max) {
				colour = LEVEL_MAX;
			} else {
				colour = LEVEL_NORMAL;
			}

			text = Component.literal(level + "/" + max);
		} else {
			if (slotLevel == 0) {
				return;
			}

			text = Component.literal((slotLevel > 0 ? "+" : "") + Numbers.format(slotLevel));
			colour = slotLevel > 0 ? SLOT_LEVEL_POSITIVE : SLOT_LEVEL_NEGATIVE;
		}

		// 坐标系：容器内容是在「以面板左上角为原点」的平移里画的，所以这里用面板内坐标；
		// 字号缩到 0.6 倍，等级数字要小到不盖住图标。
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(slot.x + 1.0F, slot.y + 1.0F, 0.0F);
		pose.scale(LEVEL_TEXT_SCALE, LEVEL_TEXT_SCALE, 1.0F);
		graphics.drawString(font, text, 0, 0, colour, true);
		pose.popPose();
	}
}
