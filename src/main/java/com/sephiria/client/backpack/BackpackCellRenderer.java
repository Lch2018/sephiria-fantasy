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
import net.minecraft.world.item.ItemStack;

/**
 * 背包格子的通用绘制，赛菲莉娅背包界面与神器附魔界面共用，保证两边的格子看起来一样。
 *
 * <p>两件事：旋转的石板要按朝向把图标转过来（不然玩家看不出它朝哪边）；
 * 左上角要画等级文字（神器是「实际等级/上限」，空格子是石板加出来的格子等级）。
 *
 * <p>与 1.21.1 的差别：那边可以直接重写 {@code renderSlot}；1.20.1 的 {@code renderSlot}
 * 是 private，所以这里的方法吃**绝对屏幕坐标**，由界面在物品画完之后调用（先擦格底再重画）。
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

	/**
	 * 旋转的石板：绕着格子中心 (x+8, y+8) 把图标转过来。
	 *
	 * <p>调用方必须先擦掉原版画的没旋转的图标（见 {@code SephiriaMenuScreen#redrawSlot}），
	 * 否则两张图标会叠在一起。物品数量/耐久条由这里补画，原版的装饰一起被擦掉了。
	 */
	public static void drawRotatedItem(GuiGraphics graphics, Font font, int x, int y, ItemStack stack) {
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x + 8.0F, y + 8.0F, 0.0F);
		// 屏幕坐标 y 向下：负角度才是视觉上的逆时针，与 SlateItem#rotate 的方向一致
		pose.mulPose(Axis.ZP.rotation(-SlateItem.rotationOf(stack) * ((float) Math.PI / 2.0F)));
		pose.translate(-8.0F, -8.0F, 0.0F);
		graphics.renderItem(stack, 0, 0);
		pose.popPose();
		graphics.renderItemDecorations(font, stack, x, y);
	}

	/** 格子左上角的等级文字（0.6 倍大小，绝对屏幕坐标）。 */
	public static void drawLevelText(GuiGraphics graphics, Font font, int x, int y, ItemStack stack,
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

		// 字号缩到 0.6 倍，等级数字要小到不盖住图标
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x + 1.0F, y + 1.0F, 300.0F);
		pose.scale(LEVEL_TEXT_SCALE, LEVEL_TEXT_SCALE, 1.0F);
		graphics.drawString(font, text, 0, 0, colour, true);
		pose.popPose();
	}
}
