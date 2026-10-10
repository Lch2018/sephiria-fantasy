package com.sephiria.client;

import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.client.backpack.ArtifactBackpackScreen;
import com.sephiria.client.backpack.ChestScreen;
import com.sephiria.client.shop.ShopScreen;
import com.sephiria.client.backpack.EnchantScreen;
import com.sephiria.client.hud.HudConfigScreen;
import com.sephiria.artifact.skill.ArtifactSkills;
import com.sephiria.artifact.skill.SkillSlots;
import com.sephiria.client.hud.SephiriaHud;
import com.sephiria.client.skill.ClientSkills;
import com.sephiria.network.ArtifactSkillsPayload;
import com.sephiria.network.CastArtifactSkillPayload;
import com.sephiria.network.CloudSyncPayload;
import com.sephiria.network.DebuffSyncPayload;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.DashPayload;
import com.sephiria.network.InvulnerablePayload;
import com.sephiria.network.StatsSyncPayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.network.ShieldSweepPayload;
import com.sephiria.network.SkillSyncPayload;
import com.sephiria.network.SunSwordSyncPayload;
import com.sephiria.registry.ModMenus;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaWeapon;
import com.sephiria.weapon.TooltipScale;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
// 26.3 把 GLFW 从客户端整个移除了，键位常量与按键类型都在 InputConstants 上
// （Type.KEYSYM → Type.KEYBOARD；鼠标侧键用 InputConstants.MOUSE_BUTTON_4）。

/**
 * 客户端入口：武器提示框、HUD、以及固有技能的按键。
 *
 * <p>提示框放在客户端是因为它本来就只存在于客户端，也因为 26.2 的原版 {@code appendHoverText}
 * 已经过时——原版改成由实现了 {@code TooltipProvider} 的数据组件提供提示行，模组这边用
 * Fabric 的 {@link ItemTooltipCallback} 更直接。
 *
 * <p>按键：冲刺默认绑<b>鼠标侧键</b>（4 号键），装填是 {@code R}，HUD 布局是 {@code H}，
 * 三个都能在「选项 → 控制」里改。按键只负责发包，数值全在服务端算。
 */
public class SephiriaClient implements ClientModInitializer {
	/** 原版背包面板的尺寸（宽 × 高），用来把标签贴在它上沿。 */
	private static final int INVENTORY_WIDTH = 176;
	private static final int INVENTORY_HEIGHT = 166;

	private static final KeyMapping DASH_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.sephiria_fantasy.dash",
			InputConstants.Type.MOUSE,
			InputConstants.MOUSE_BUTTON_4,
			KeyMapping.Category.GAMEPLAY));

	private static final KeyMapping RELOAD_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.sephiria_fantasy.reload",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_R,
			KeyMapping.Category.GAMEPLAY));

	private static final KeyMapping HUD_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.sephiria_fantasy.hud",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_H,
			KeyMapping.Category.GAMEPLAY));

	/**
	 * 神器技能的 6 个按键：<b>默认不绑键</b>（UNKNOWN = 未绑定），玩家自己去
	 * 「选项 → 控制 → 游戏玩法」里设「神器技能 1..6」。
	 */
	private static final KeyMapping[] SKILL_KEYS = new KeyMapping[SkillSlots.COUNT];

	static {
		for (int slot = 0; slot < SKILL_KEYS.length; slot++) {
			SKILL_KEYS[slot] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
					"key.sephiria_fantasy.artifact_skill_" + (slot + 1),
					InputConstants.Type.KEYBOARD,
					InputConstants.UNKNOWN.getValue(),
					KeyMapping.Category.GAMEPLAY));
		}
	}

	@Override
	public void onInitializeClient() {
		// 容器菜单的界面：26.2 的 MenuScreens.register 是包私有的，走访问拓宽（sephiria.accesswidener）
		MenuScreens.register(ModMenus.ARTIFACT_BACKPACK, ArtifactBackpackScreen::new);
		MenuScreens.register(ModMenus.ARTIFACT_ENCHANT, EnchantScreen::new);
		MenuScreens.register(ModMenus.CHEST, ChestScreen::new);
		MenuScreens.register(ModMenus.SHOP, ShopScreen::new);

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof SephiriaWeapon detail) {
				// 详细描述：空行分隔，逐条显示。外层套灰，行内的数值用自己的颜色覆盖它。
				// 数值按玩家当前属性换算（武器类里存的是基准值），拼完立刻清掉倍率
				java.util.List<Component> detailLines;

				try {
					TooltipScale.set(ClientStats.damageMultiplier(), ClientStats.skillDamageMultiplier(),
							ClientStats.attackSpeedMultiplier(), ClientStats.rangeMultiplier());
					detailLines = detail.detailLines(stack);
				} finally {
					TooltipScale.reset();
				}

				if (!detailLines.isEmpty()) {
					lines.add(Component.empty());

					for (Component line : detailLines) {
						lines.add(line.copy().withStyle(ChatFormatting.GRAY));
					}
				}
			}

			// 神器：连招、【唯一】、词条、稀有度、背景描述
			java.util.List<Component> artifactLines = ArtifactTooltip.lines(stack);

			if (!artifactLines.isEmpty()) {
				lines.add(Component.empty());
				lines.addAll(artifactLines);
			}

			if (stack.getItem() instanceof SephiriaCrossbowItem) {
				lines.add(Component.translatable("tooltip.sephiria_fantasy.magazine",
						Component.literal(format(SkillClientData.current(SephiriaCrossbowItem.STORAGE))),
						Component.literal(format(SkillClientData.max(SephiriaCrossbowItem.STORAGE)))));
			}
		});

		// 服务端推来的存储量，HUD 读它
		ClientPlayNetworking.registerGlobalReceiver(SkillSyncPayload.TYPE,
				(payload, context) -> SkillClientData.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(InvulnerablePayload.TYPE,
				(payload, context) -> InvulnClientData.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(StatsSyncPayload.TYPE,
				(payload, context) -> ClientStats.accept(payload));
		// 乌云容量：HUD 的乌云 UI 读它（上限 > 0 才显示）
		ClientPlayNetworking.registerGlobalReceiver(CloudSyncPayload.TYPE,
				(payload, context) -> CloudClientData.accept(payload));
		// 减益层数：敌人脚下的层数标签读它（0 = 标签消失）
		ClientPlayNetworking.registerGlobalReceiver(DebuffSyncPayload.TYPE,
				(payload, context) -> ClientDebuffs.accept(payload));
		// 太阳剑数量：HUD 的太阳剑 UI 读它（上限 > 0 才显示）
		ClientPlayNetworking.registerGlobalReceiver(SunSwordSyncPayload.TYPE,
				(payload, context) -> SunSwordClientData.accept(payload));
		// 换世界 / 重连时清掉镜像：云、减益与太阳剑状态都不跨世界，免得新世界里 UI 还挂着上个世界的残值
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			CloudClientData.reset();
			ClientDebuffs.reset();
			SunSwordClientData.reset();
		});
		// 神器技能页的可用技能与 6 个栏位
		ClientPlayNetworking.registerGlobalReceiver(ArtifactSkillsPayload.TYPE, (payload, context) -> {
			ClientSkills.accept(payload);

			// 技能页开着就地重建，否则页面还显示旧内容（「切换不动」的观感来源之一）
			// 26.2 的当前界面在 Minecraft.gui.screen() 上（Minecraft 自己已经没有 screen 字段）
			if (Minecraft.getInstance().gui.screen() instanceof com.sephiria.client.skill.ArtifactSkillScreen screen) {
				screen.refresh();
			}
		});

		// 背包界面顶部的标签栏：原版背包 / 赛菲利亚背包 / 属性。
		// 生存与创造用的是两个不同的界面类，所以两种都挂——之前只挂了生存的，创造模式下看不到。
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
				return;
			}

			int left = (scaledWidth - INVENTORY_WIDTH) / 2;
			int top = (scaledHeight - INVENTORY_HEIGHT) / 2;
			// 创造模式面板上沿那一整条被原版的物品栏标签页占着（顶到面板上方约 28 像素），
			// 所以这一排要再往上让一层，免得被压在原版标签下面。
			int rowY = screen instanceof CreativeModeInventoryScreen ? top - 48 : top - 20;
			SephiriaTabs.add(screen, left, rowY, SephiriaTab.VANILLA);
		});

		HudElementRegistry.addLast(SephiriaHud.ID, new SephiriaHud());

		// 防御中的左键：原版在使用物品期间会把左键整个吞掉（handleKeybinds 的 isUsingItem 分支
		// 只 consumeClick、既不 startAttack 也不发包），所以这里抢在它前面把这次点击取走，
		// 改成给服务端发一个「防御中横扫」的请求。START 刻在这个 tick 的按键处理之前，抢得到。
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null || !SephiriaShieldItem.isDefending(client.player)) {
				return;
			}

			if (client.options.keyAttack.consumeClick()) {
				// 这一 tick 里累积的连点只发一次：服务端有 1 秒冷却，多发没有意义
				while (client.options.keyAttack.consumeClick()) {
					// 丢弃剩余的点击次数
				}

				// 26.3 的 swing 需要动画组件；末参 true = 同步给自己（客户端本地挥动照旧传 true）
				client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
				ClientPlayNetworking.send(ShieldSweepPayload.INSTANCE);
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			InvulnClientData.tick();
			// 实体死亡/卸载后服务端不会再发清除包，靠这里按「实体还在不在」清掉它的减益标签
			ClientDebuffs.prune(client.level);

			// consumeClick 会把这期间累积的按键次数一次取出，避免连点丢事件
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

			while (HUD_KEY.consumeClick()) {
				client.setScreenAndShow(new HudConfigScreen(null));
			}

			// 神器技能：按下第几号栏位就发第几号，冷却与蓝量由服务端判
			for (int slot = 0; slot < SKILL_KEYS.length; slot++) {
				while (SKILL_KEYS[slot].consumeClick()) {
					if (client.player != null) {
						ClientPlayNetworking.send(new CastArtifactSkillPayload(slot));
					}
				}
			}
		});
	}

	private static String format(double value) {
		int rounded = (int) Math.round(value);
		return rounded < 0 ? "0" : String.valueOf(rounded);
	}
}
