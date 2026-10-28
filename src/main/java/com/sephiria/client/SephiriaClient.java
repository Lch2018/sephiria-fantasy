package com.sephiria.client;

import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.backpack.ChestMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.hud.HudClientData;
import com.sephiria.client.hud.SephiriaHud;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.HudSyncPayload;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.registry.ModMenus;
import com.sephiria.shop.ShopMenu;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.trader.SephiriaTraderMenu;
import com.sephiria.wishpool.WishPoolMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * 1.20.1 客户端入口（核心版）。
 *
 * <p>目前只注册容器界面：统一用 {@link SephiriaMenuScreen} 画一个可用面板。
 * HUD、自绘界面、网络同步与 GeckoLib 动画还没有移植。
 *
 * <p>1.20.1 的 {@code MenuScreens.register} 是包私有，靠访问拓宽打开
 * （见 {@code sephiria_fantasy.accesswidener}）。
 */
public final class SephiriaClient implements ClientModInitializer {
	private static final String CATEGORY = "key.categories.gameplay";

	private static final KeyMapping DASH_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.dash", InputConstants.Type.MOUSE, 4, CATEGORY));

	private static final KeyMapping RELOAD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.reload", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY));

	/** 神器技能 6 个栏位：默认不绑键，玩家自己去控制里设置。 */
	private static final KeyMapping[] SKILL_KEYS = new KeyMapping[
			com.sephiria.artifact.skill.SkillSlots.COUNT];

	static {
		for (int slot = 0; slot < SKILL_KEYS.length; slot++) {
			SKILL_KEYS[slot] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
					"key.sephiria_fantasy.artifact_skill_" + (slot + 1),
					InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		}
	}

	@Override
	public void onInitializeClient() {
		MenuScreens.<ArtifactBackpackMenu, com.sephiria.client.backpack.BackpackScreen>register(
				ModMenus.ARTIFACT_BACKPACK, com.sephiria.client.backpack.BackpackScreen::new);
		MenuScreens.<EnchantMenu, com.sephiria.client.backpack.EnchantScreen>register(ModMenus.ARTIFACT_ENCHANT,
				com.sephiria.client.backpack.EnchantScreen::new);
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

		// 原版背包上沿两个入口：赛菲莉娅背包 / 商店。真正的界面由服务端打开。
		// 1.20.1 的标签栏自绘还没移植，先用两个按钮占位。
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
				return;
			}

			int left = (scaledWidth - 176) / 2;
			int top = (scaledHeight - 166) / 2;
			int rowY = screen instanceof CreativeModeInventoryScreen ? top - 48 : top - 20;
			Screens.getButtons(screen).add(Button.builder(
					Component.translatable("screen.sephiria_fantasy.backpack"),
					button -> sendEmpty(com.sephiria.network.OpenBackpackPayload.CHANNEL))
					.bounds(left + 4, rowY, 72, 18).build());
			Screens.getButtons(screen).add(Button.builder(
					Component.translatable("screen.sephiria_fantasy.shop"),
					button -> sendEmpty(com.sephiria.network.OpenShopPayload.CHANNEL))
					.bounds(left + 80, rowY, 40, 18).build());
		});

		// 服务端每 5 刻推一次 HUD 状态；老版网络包在客户端线程外到达，切回主线程再写镜像
		ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					HudSyncPayload payload = HudSyncPayload.read(buffer);
					client.execute(() -> HudClientData.accept(payload));
				});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> HudClientData.reset());
		HudRenderCallback.EVENT.register(new SephiriaHud());

		// 神器 / 石板 / 药水的提示行：名称由原版画，这里补连击、唯一、词条、稀有度与描述
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (DASH_KEY.consumeClick()) {
				if (client.player != null) {
					sendEmpty(com.sephiria.network.DashPayload.CHANNEL);
				}
			}

			while (RELOAD_KEY.consumeClick()) {
				if (client.player != null) {
					sendEmpty(com.sephiria.network.ReloadPayload.CHANNEL);
				}
			}

			for (int slot = 0; slot < SKILL_KEYS.length; slot++) {
				while (SKILL_KEYS[slot].consumeClick()) {
					if (client.player != null) {
						FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
						com.sephiria.network.CastArtifactSkillPayload.write(buffer,
								new com.sephiria.network.CastArtifactSkillPayload(slot));
						ClientPlayNetworking.send(com.sephiria.network.CastArtifactSkillPayload.CHANNEL, buffer);
					}
				}
			}
		});

		ItemTooltipCallback.EVENT.register((stack, flag, lines) -> {
			java.util.List<net.minecraft.network.chat.Component> artifactLines =
					com.sephiria.client.artifact.ArtifactTooltip.lines(stack);

			if (!artifactLines.isEmpty()) {
				lines.add(net.minecraft.network.chat.Component.empty());
				lines.addAll(artifactLines);
			}
		});
	}

	private static void sendEmpty(net.minecraft.resources.ResourceLocation channel) {
		ClientPlayNetworking.send(channel, new FriendlyByteBuf(Unpooled.buffer()));
	}
}
