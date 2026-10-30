package com.sephiria.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.ChestMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.hud.HudClientData;
import com.sephiria.client.hud.SephiriaHud;
import com.sephiria.difficulty.RootRetreatBlock;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.network.DashPayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.registry.ModMenus;
import com.sephiria.shop.ShopMenu;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.trader.SephiriaTraderMenu;
import com.sephiria.wishpool.WishPoolBlock;
import com.sephiria.wishpool.WishPoolMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LlamaRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import com.sephiria.registry.ModEntities;
import net.minecraft.network.chat.Component;

/**
 * 1.21.1 客户端入口（核心版）。
 *
 * <p>目前只做不依赖 26.3 渲染管线的事：容器界面注册（背包 / 附魔 / 石板合成 / 宝箱这几个
 * 已经换成自绘面板，其余仍用通用面板）、冲刺 / 装填按键、技能与神器两排 HUD 及其布局界面
 * （H 键）。26.3 的武器 / 冲刺 / MP 等自绘 HUD、ToroHealth、GeckoLib 刀动画还没有移植，
 * 别把这一版当完整客户端。
 */
public final class SephiriaClient implements ClientModInitializer {
	/** 1.21.1 的按键分类仍是一个字符串。 */
	private static final String CATEGORY = "key.categories.gameplay";

	private static final KeyMapping DASH_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.dash", InputConstants.Type.MOUSE, 4, CATEGORY));

	private static final KeyMapping RELOAD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.reload", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY));

	private static final KeyMapping HUD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.hud", InputConstants.Type.KEYSYM, InputConstants.KEY_H, CATEGORY));

	// L 被原版「进度」占用，默认不绑键，玩家自己去控制里设
	private static final KeyMapping SKILL_PAGE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.skill_page", InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), "key.categories.gameplay"));

	private static final KeyMapping STATS_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.stats", InputConstants.Type.KEYSYM, InputConstants.KEY_K,
			"key.categories.gameplay"));

	// T 被原版聊天占用，默认不绑键
	private static final KeyMapping TALENT_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.talent", InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), "key.categories.gameplay"));

	@Override
	public void onInitializeClient() {
		com.sephiria.client.particle.SephiriaParticles.register();
		com.sephiria.client.hud.HudConfig.load();
		com.sephiria.client.render.KatanaRenderer katanaRenderer = new com.sephiria.client.render.KatanaRenderer();
		BuiltinItemRendererRegistry.INSTANCE.register(com.sephiria.registry.ModItems.BLADE,
				(stack, context, pose, buffers, light, overlay) -> katanaRenderer.renderByItem(
						stack, context, pose, buffers, light, overlay));

		MenuScreens.<ArtifactBackpackMenu, com.sephiria.client.backpack.ArtifactBackpackScreen>register(
				ModMenus.ARTIFACT_BACKPACK, com.sephiria.client.backpack.ArtifactBackpackScreen::new);
		MenuScreens.<EnchantMenu, com.sephiria.client.backpack.EnchantScreen>register(
				ModMenus.ARTIFACT_ENCHANT, com.sephiria.client.backpack.EnchantScreen::new);
		MenuScreens.<ChestMenu, com.sephiria.client.backpack.ChestScreen>register(ModMenus.CHEST,
				com.sephiria.client.backpack.ChestScreen::new);
		MenuScreens.<ShopMenu, com.sephiria.client.shop.ShopScreen>register(ModMenus.SHOP,
				com.sephiria.client.shop.ShopScreen::new);
		MenuScreens.<SlateFusionMenu, com.sephiria.client.slate.SlateFusionScreen>register(
				ModMenus.SLATE_FUSION, com.sephiria.client.slate.SlateFusionScreen::new);
		MenuScreens.<SephiriaTraderMenu, com.sephiria.client.trader.TraderScreen>register(ModMenus.TRADER,
				com.sephiria.client.trader.TraderScreen::new);
		MenuScreens.<RootRetreatMenu, com.sephiria.client.difficulty.RootRetreatScreen>register(
				ModMenus.ROOT_RETREAT, com.sephiria.client.difficulty.RootRetreatScreen::new);
		MenuScreens.<WishPoolMenu, com.sephiria.client.wishpool.WishPoolScreen>register(
				ModMenus.WISH_POOL, com.sephiria.client.wishpool.WishPoolScreen::new);
		MenuScreens.<com.sephiria.beacon.SephiriaBeaconMenu, com.sephiria.client.beacon.SephiriaBeaconScreen>register(
				ModMenus.SEPHIRIA_BEACON, com.sephiria.client.beacon.SephiriaBeaconScreen::new);

		// 流浪商人先用原版模型与贴图，至少保证实体可见；红袍自定义模型后续再接。
		EntityRendererRegistry.register(ModEntities.SEPHIRIA_WANDERING_TRADER, WanderingTraderRenderer::new);
		EntityRendererRegistry.register(ModEntities.SEPHIRIA_TRADER_LLAMA,
				context -> new LlamaRenderer(context, ModelLayers.TRADER_LLAMA));

		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.WishPoolResultPayload.TYPE,
				(payload, context) -> com.sephiria.client.wishpool.ClientWishPool.accept(payload.items()));
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register(
				(client, screen, scaledWidth, scaledHeight) -> {
					if (!com.sephiria.client.tabs.InventoryTabs.applies(screen)) {
						return;
					}

					int left = (scaledWidth - 176) / 2;
					int top = (scaledHeight - 166) / 2;
					int rowY = screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
							? top - 48 : top - 22;
					com.sephiria.client.tabs.InventoryTabs.add(screen, left, rowY);
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.TalentSyncPayload.TYPE,
				(payload, context) -> ClientTalents.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.ArtifactSkillsPayload.TYPE,
				(payload, context) -> ClientSkills.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.HudSyncPayload.TYPE,
				(payload, context) -> HudClientData.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.StatsSyncPayload.TYPE,
				(payload, context) -> ClientStats.accept(payload));
		// ToroHealth 可选兼容：收服务端的伤害显示信息，等粒子生成时再匹配。
		// 没装 ToroHealth 时 ToroHealthCompatClient.accept 会直接丢弃，不会积压。
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.compat.torohealth.ToroHealthDamagePayload.TYPE,
				(payload, context) -> com.sephiria.compat.torohealth.ToroHealthCompatClient.accept(payload));

		// 神器 / 石板 / 药水的提示行：名称由原版画，这里补连击、唯一、词条、稀有度与描述
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			java.util.List<net.minecraft.network.chat.Component> artifactLines =
					com.sephiria.client.artifact.ArtifactTooltip.lines(stack);

			if (!artifactLines.isEmpty()) {
				lines.add(net.minecraft.network.chat.Component.empty());
				lines.addAll(artifactLines);
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			HudClientData.reset();
			// 换世界后别让上个世界残留的待匹配伤害串到新实体上
			com.sephiria.compat.torohealth.ToroHealthCompatClient.reset();
		});
		HudRenderCallback.EVENT.register(new SephiriaHud());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (DASH_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(DashPayload.INSTANCE);
				}
			}

			while (RELOAD_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(ReloadPayload.INSTANCE);
				}
			}

			// H 打开 HUD 布局：与 K / T 一样只在没有任何界面开着时响应，避免在聊天里误触
			while (HUD_KEY.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new com.sephiria.client.hud.HudConfigScreen(null));
				}
			}

			// K / T 打开属性页与天赋页：只在没有任何界面开着时响应，避免在聊天/背包里误触
			while (STATS_KEY.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new com.sephiria.client.gui.StatsScreen());
				}
			}

			while (TALENT_KEY.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new com.sephiria.client.gui.TalentScreen());
				}
			}
		});

		// 引用一次，保证标题常量被初始化（也避免 IDE 把 import 当成未使用）
		Component.empty();
		RootRetreatBlock.title();
		WishPoolBlock.title();
	}
}
