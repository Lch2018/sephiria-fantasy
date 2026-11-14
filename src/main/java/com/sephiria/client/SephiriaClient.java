package com.sephiria.client;

import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.ChestMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.hud.HudClientData;
import com.sephiria.client.hud.SephiriaHud;
import com.sephiria.network.HudSyncPayload;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.registry.ModMenus;
import com.sephiria.shop.ShopMenu;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.trader.SephiriaTraderMenu;
import com.sephiria.wishpool.WishPoolMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
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
 * <p>目前注册容器界面与完整 HUD（武器 / 冲刺 / MP / 乌云 / 太阳剑 / 冰霜武具 / 无敌条 +
 * 技能 / 神器两排）：背包 / 附魔 / 石板合成 / 宝箱这几个已经换成自绘面板，H 键打开
 * 赛菲莉娅设置（HUD 布局 / 伤害显示）。HUD 用到的各份客户端镜像（技能存储、乌云、太阳剑、
 * 冰霜武具、无敌窗口、护盾、魔法书、石板数）都在这里注册接收。
 *
 * <p>客户端功能已按 0.5.3.8 对齐：无敌描边、ToroHealth 数字、GeckoLib 刀动画与微光、
 * 防御中左键横扫、光标保持、冰冻染色都在这里挂接。
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

	private static final KeyMapping STATS_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.stats", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY));

	// T 在 1.20.1 是原版聊天键，天赋页默认不绑键，玩家自己去控制里设置
	private static final KeyMapping TALENT_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.talent", InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), CATEGORY));

	private static final KeyMapping HUD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.hud", InputConstants.Type.KEYSYM, InputConstants.KEY_H, CATEGORY));

	// L 被原版「进度」占用，默认不绑键
	private static final KeyMapping SKILL_PAGE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.skill_page", InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), CATEGORY));

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
		com.sephiria.client.particle.SephiriaParticles.register();
		// 赛菲莉娅流浪商人走原版模型，只换红袍贴图；不注册就没有渲染器
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.sephiria.registry.ModEntities.SEPHIRIA_WANDERING_TRADER,
				com.sephiria.client.trader.SephiriaTraderRenderer::new);
		com.sephiria.client.hud.HudConfig.load();

		MenuScreens.<ArtifactBackpackMenu, com.sephiria.client.backpack.ArtifactBackpackScreen>register(
				ModMenus.ARTIFACT_BACKPACK, com.sephiria.client.backpack.ArtifactBackpackScreen::new);
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

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!com.sephiria.client.tabs.InventoryTabs.applies(screen)) {
				return;
			}

			int left = (scaledWidth - 176) / 2;
			int top = (scaledHeight - 166) / 2;
			int rowY = screen instanceof CreativeModeInventoryScreen ? top - 48 : top - 22;
			com.sephiria.client.tabs.InventoryTabs.add(screen, left, rowY);
		});

		// 服务端每 5 刻推一次 HUD 状态；老版网络包在客户端线程外到达，切回主线程再写镜像
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.ArtifactSkillsPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.ArtifactSkillsPayload payload =
							com.sephiria.network.ArtifactSkillsPayload.read(buffer);
					client.execute(() -> com.sephiria.client.skill.ClientSkills.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.TalentSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.TalentSyncPayload payload =
							com.sephiria.network.TalentSyncPayload.read(buffer);
					client.execute(() -> ClientTalents.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.WishPoolResultPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.WishPoolResultPayload payload =
							com.sephiria.network.WishPoolResultPayload.read(buffer);
					client.execute(() -> com.sephiria.client.wishpool.ClientWishPool.accept(payload.items(), payload.energyGained()));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.StatsSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.StatsSyncPayload payload =
							com.sephiria.network.StatsSyncPayload.read(buffer);
					client.execute(() -> ClientStats.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.MagicBookStatsPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.MagicBookStatsPayload payload =
							com.sephiria.network.MagicBookStatsPayload.read(buffer);
					client.execute(() -> ClientBookStats.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.SlateCountPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.SlateCountPayload payload =
							com.sephiria.network.SlateCountPayload.read(buffer);
					client.execute(() -> ClientSlateCount.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.BookEffectPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.BookEffectPayload payload =
							com.sephiria.network.BookEffectPayload.read(buffer);
					client.execute(() -> com.sephiria.artifact.BookEffectData.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.FatigueSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.FatigueSyncPayload payload =
							com.sephiria.network.FatigueSyncPayload.read(buffer);
					client.execute(() -> FatigueClientData.accept(payload.points()));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.DebuffSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.DebuffSyncPayload payload =
							com.sephiria.network.DebuffSyncPayload.read(buffer);
					client.execute(() -> ClientDebuffs.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.CardSelectionPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.CardSelectionPayload payload =
							com.sephiria.network.CardSelectionPayload.read(buffer);
					client.execute(() -> {
						com.sephiria.client.card.ClientCardSelection.accept(payload);

						if (!payload.open()) {
							if (client.screen instanceof com.sephiria.client.card.CardSelectionScreen screen) {
								screen.onClose();
							}

							return;
						}

						if (com.sephiria.client.card.ClientCardSelection.autoOpenSuppressed()) {
							return;
						}

						if (client.screen instanceof com.sephiria.client.card.CardSelectionScreen screen) {
							screen.syncFromCache(false);
						} else {
							com.sephiria.client.CursorKeeper.show(client, new com.sephiria.client.card.CardSelectionScreen());
						}
					});
				});
		ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					HudSyncPayload payload = HudSyncPayload.read(buffer);
					client.execute(() -> HudClientData.accept(payload));
				});
		// 技能存储（冲刺次数 / 弹匣 / 专注）：武器面板与冲刺 UI 读它
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.SkillSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.SkillSyncPayload payload =
							com.sephiria.network.SkillSyncPayload.read(buffer);
					client.execute(() -> SkillClientData.accept(payload));
				});
		// 乌云容量：HUD 的乌云 UI 读它（上限 > 0 才显示）
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.CloudSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.CloudSyncPayload payload =
							com.sephiria.network.CloudSyncPayload.read(buffer);
					client.execute(() -> CloudClientData.accept(payload));
				});
		// 太阳剑数量：HUD 的太阳剑 UI 读它（上限 > 0 才显示）
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.SunSwordSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.SunSwordSyncPayload payload =
							com.sephiria.network.SunSwordSyncPayload.read(buffer);
					client.execute(() -> SunSwordClientData.accept(payload));
				});
		// 冰霜武具的状态：HUD 那一行「冰霜武具：剑 锤 矛 弓x/7 斩」读它
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.FrostSyncPayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.FrostSyncPayload payload =
							com.sephiria.network.FrostSyncPayload.read(buffer);
					client.execute(() -> FrostClientData.accept(payload));
				});
		// 无敌窗口：HUD 的无敌条按剩余刻数自己递减
		ClientPlayNetworking.registerGlobalReceiver(com.sephiria.network.InvulnerablePayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.network.InvulnerablePayload payload =
							com.sephiria.network.InvulnerablePayload.read(buffer);
					client.execute(() -> InvulnClientData.accept(payload));
				});
		// ToroHealth 可选兼容：收服务端的伤害显示信息，等粒子生成时再匹配。
		// 没装 ToroHealth 时 ToroHealthCompatClient.accept 会直接丢弃，不会积压。
		ClientPlayNetworking.registerGlobalReceiver(
				com.sephiria.compat.torohealth.ToroHealthDamagePayload.CHANNEL,
				(client, handler, buffer, sender) -> {
					com.sephiria.compat.torohealth.ToroHealthDamagePayload payload =
							com.sephiria.compat.torohealth.ToroHealthDamagePayload.read(buffer);
					client.execute(() -> com.sephiria.compat.torohealth.ToroHealthCompatClient.accept(payload));
				});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			HudClientData.reset();
			// 乌云 / 太阳剑 / 冰霜武具 / 技能存储 / 无敌窗口都不跨世界：不清的话新世界里
			// 还挂着上个世界的残值（HUD 会多亮一瞬、无敌条卡住）
			CloudClientData.reset();
			SunSwordClientData.reset();
			FrostClientData.reset();
			SkillClientData.reset();
			InvulnClientData.reset();
			SephiriaHud.resetFlash();
			// 减益标签 / 石板数量 / 魔法书快照 / 选卡三张卡 / 天赋页都不跨世界：
			// 不清的话新服会先显示上个世界的数据一瞬，选卡还可能把上个服的卡弹出来
			com.sephiria.client.ClientDebuffs.reset();
			com.sephiria.client.ClientSlateCount.reset();
			com.sephiria.client.ClientBookStats.reset();
			com.sephiria.artifact.BookEffectData.reset();
			com.sephiria.client.ClientTalents.reset();
			com.sephiria.client.card.ClientCardSelection.clear();
			// 换世界后别让上个世界残留的待匹配伤害串到新实体上
			com.sephiria.compat.torohealth.ToroHealthCompatClient.reset();
		});
		HudRenderCallback.EVENT.register(new SephiriaHud());

		// 防御中的左键：原版在使用物品期间会把左键整个吞掉（handleKeybinds 的 isUsingItem 分支
		// 只 consumeClick、既不 startAttack 也不发包），所以这里抢在它前面把这次点击取走，
		// 改成给服务端发一个「防御中横扫」的请求。START 刻在这个 tick 的按键处理之前，抢得到。
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null || !com.sephiria.weapon.SephiriaShieldItem.isDefending(client.player)) {
				return;
			}

			if (client.options.keyAttack.consumeClick()) {
				// 这一 tick 里累积的连点只发一次：服务端有冷却，多发没有意义
				while (client.options.keyAttack.consumeClick()) {
					// 丢弃剩余的点击次数
				}

				client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
				sendEmpty(com.sephiria.network.ShieldSweepPayload.CHANNEL);
			}
		});

		// 神器 / 石板 / 药水的提示行：名称由原版画，这里补连击、唯一、词条、稀有度与描述
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// 无敌窗口只在收到包时写入，之后由客户端自己每刻递减
			InvulnClientData.tick();
			// 实体死亡/卸载后服务端不会再发清除包，靠这里按「实体还在不在」清掉它的减益标签
			com.sephiria.client.ClientDebuffs.prune(client.level);

			while (DASH_KEY.consumeClick()) {
				if (client.player != null) {
					sendEmpty(com.sephiria.network.DashPayload.CHANNEL);
				}
			}

			while (STATS_KEY.consumeClick()) {
				client.setScreen(new com.sephiria.client.gui.StatsScreen());
			}

			// H 打开 HUD 布局：和属性 / 天赋页一样只在没有界面开着时响应，避免在聊天里误触
			while (HUD_KEY.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new com.sephiria.client.config.SephiriaSettingsScreen(null));
				}
			}

			while (TALENT_KEY.consumeClick()) {
				client.setScreen(new com.sephiria.client.gui.TalentScreen());
			}

			while (SKILL_PAGE_KEY.consumeClick()) {
				client.setScreen(new com.sephiria.client.skill.ArtifactSkillScreen());
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
			// 赛菲莉娅额外背包栏：一行用途说明（「+1」绿色，其余灰色）
			if (stack.getItem() == com.sephiria.registry.ModItems.EXTRA_BACKPACK_ROW) {
				lines.add(net.minecraft.network.chat.Component.translatable(
								"tooltip.sephiria_fantasy.extra_backpack_row",
								net.minecraft.network.chat.Component.literal("+1")
										.withStyle(net.minecraft.ChatFormatting.GREEN))
						.withStyle(net.minecraft.ChatFormatting.GRAY));
			}

			// 弩：当前弹匣 / 上限（备用弹匣数）
			if (stack.getItem() instanceof com.sephiria.weapon.SephiriaCrossbowItem) {
				lines.add(net.minecraft.network.chat.Component.translatable(
						"tooltip.sephiria_fantasy.magazine_reserve",
						net.minecraft.network.chat.Component.literal(formatNumber(
								com.sephiria.client.SkillClientData.current(
										com.sephiria.weapon.SephiriaCrossbowItem.STORAGE))),
						net.minecraft.network.chat.Component.literal(formatNumber(
								com.sephiria.client.SkillClientData.max(
										com.sephiria.weapon.SephiriaCrossbowItem.STORAGE))),
						net.minecraft.network.chat.Component.literal(formatNumber(
								com.sephiria.client.SkillClientData.current(
										com.sephiria.weapon.SephiriaCrossbowItem.MAGAZINES)))));
			}

			java.util.List<net.minecraft.network.chat.Component> artifactLines =
					com.sephiria.client.artifact.ArtifactTooltip.lines(stack);

			if (!artifactLines.isEmpty()) {
				lines.add(net.minecraft.network.chat.Component.empty());
				lines.addAll(artifactLines);
			}
		});
	}

	private static String formatNumber(double value) {
		int rounded = (int) Math.round(value);
		return rounded < 0 ? "0" : String.valueOf(rounded);
	}

	private static void sendEmpty(net.minecraft.resources.ResourceLocation channel) {
		ClientPlayNetworking.send(channel, new FriendlyByteBuf(Unpooled.buffer()));
	}
}
