package com.sephiria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * 刀的 GeckoLib 物品渲染器。
 *
 * <p>1.20.1 的普通物品模型没法按 {@code display_context} 选模型（26.x 的
 * {@code minecraft:select} 在 1.20.1 不存在），所以 GUI / 地面 / 展示框仍走平面贴图，
 * 手持（第一 / 第三人称）由 {@code KatanaItemRendererMixin} 在
 * {@code ItemRenderer.render} 开头接管，改画这个 3D 模型。
 *
 * <p>手里那套显示变换照抄资源包里的 {@code models/item/blade_in_hand.json}
 * （26.x 的 {@code minecraft:special} 用的就是它），包括它从 {@code item/weapon}
 * 继承来的左/右手第三人称值；数值是 JSON 里的「像素」，这里除以 16 换成方块单位——
 * 1.20.1 的 {@link ItemTransform} 构造函数收的就是已经除过的值。
 */
public class KatanaRenderer extends GeoItemRenderer<SephiriaKatanaItem> {
	/** thirdperson_righthand：rotation [0,0,0]（0.5.3.8 口径：刀刃朝前）、translation [0,-2.75,1.25]、scale 0.6。 */
	private static final ItemTransform THIRD_PERSON_RIGHT = new ItemTransform(
			new Vector3f(0.0F, 0.0F, 0.0F),
			new Vector3f(0.0F, -2.75F / 16.0F, 1.25F / 16.0F),
			new Vector3f(0.6F, 0.6F, 0.6F));
	/** thirdperson_lefthand：沿用 weapon.json 的 [0,90,-10] / [0,4,0.5] / 0.85。 */
	private static final ItemTransform THIRD_PERSON_LEFT = new ItemTransform(
			new Vector3f(0.0F, 90.0F, -10.0F),
			new Vector3f(0.0F, 4.0F / 16.0F, 0.5F / 16.0F),
			new Vector3f(0.85F, 0.85F, 0.85F));
	/** firstperson_righthand：rotation [0,0,0]（0.5.3.8 口径：刀刃朝前）、translation [1.75,-2.5,0]、scale 0.75。 */
	private static final ItemTransform FIRST_PERSON_RIGHT = new ItemTransform(
			new Vector3f(0.0F, 0.0F, 0.0F),
			new Vector3f(1.75F / 16.0F, -2.5F / 16.0F, 0.0F),
			new Vector3f(0.75F, 0.75F, 0.75F));
	/** firstperson_lefthand：rotation [0,90,0]、translation [4.5,-4.75,-2.5]、默认缩放。 */
	private static final ItemTransform FIRST_PERSON_LEFT = new ItemTransform(
			new Vector3f(0.0F, 90.0F, 0.0F),
			new Vector3f(4.5F / 16.0F, -4.75F / 16.0F, -2.5F / 16.0F),
			new Vector3f(1.0F, 1.0F, 1.0F));

	public KatanaRenderer() {
		super(new KatanaModel());
		// 剑意满层时再画一遍蓝白微光（物品 isFoil 为 true 才提交）
		addRenderLayer(new WeaponGlintLayer(this));
	}

	/**
	 * 按 {@code ItemRenderer.render} 的规则摆好手持模型。
	 *
	 * <p>原版在调用 BEWLR 前做两件事：先 {@code transform.apply(leftHand)}（左手会把 y/z
	 * 旋转和 x 平移取反），再把模型中心平移到原点（-0.5,-0.5,-0.5）。两件都照做，
	 * 26.x 下同一个模型就是这么被摆到手里的。
	 */
	public static void applyHandTransform(ItemDisplayContext context, boolean leftHand, PoseStack pose) {
		switch (context) {
			case THIRD_PERSON_RIGHT_HAND -> THIRD_PERSON_RIGHT.apply(leftHand, pose);
			case THIRD_PERSON_LEFT_HAND -> THIRD_PERSON_LEFT.apply(leftHand, pose);
			case FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_RIGHT.apply(leftHand, pose);
			case FIRST_PERSON_LEFT_HAND -> FIRST_PERSON_LEFT.apply(leftHand, pose);
			default -> {
				// GUI / 地面 / 展示框不经过这里（还是平面图标）
			}
		}

		// 与 ItemRenderer.render 一致：把 0..1 的物品模型空间挪到以原点为中心
		pose.translate(-0.5F, -0.5F, -0.5F);
	}
}
