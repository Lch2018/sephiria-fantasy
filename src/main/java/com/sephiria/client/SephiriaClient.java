package com.sephiria.client;

import com.sephiria.client.hud.HudConfigScreen;
import com.sephiria.client.hud.SephiriaHud;
import com.sephiria.network.DashPayload;
import com.sephiria.network.InvulnerablePayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.network.SkillSyncPayload;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaWeapon;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

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
	private static final KeyMapping DASH_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.sephiria.dash",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_4,
			KeyMapping.Category.GAMEPLAY));

	private static final KeyMapping RELOAD_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.sephiria.reload",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			KeyMapping.Category.GAMEPLAY));

	private static final KeyMapping HUD_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.sephiria.hud",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			KeyMapping.Category.GAMEPLAY));

	@Override
	public void onInitializeClient() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof SephiriaWeapon weapon) {
				lines.add(weapon.tooltipLine());
			}
			if (stack.getItem() instanceof SephiriaCrossbowItem) {
				lines.add(Component.translatable("tooltip.sephiria.magazine",
						Component.literal(format(SkillClientData.current(SephiriaCrossbowItem.STORAGE))),
						Component.literal(format(SkillClientData.max(SephiriaCrossbowItem.STORAGE)))));
			}
		});

		// 服务端推来的存储量，HUD 读它
		ClientPlayNetworking.registerGlobalReceiver(SkillSyncPayload.TYPE,
				(payload, context) -> SkillClientData.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(InvulnerablePayload.TYPE,
				(payload, context) -> InvulnClientData.accept(payload));

		HudElementRegistry.addLast(SephiriaHud.ID, new SephiriaHud());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			InvulnClientData.tick();

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
		});
	}

	private static String format(double value) {
		int rounded = (int) Math.round(value);
		return rounded < 0 ? "0" : String.valueOf(rounded);
	}
}
