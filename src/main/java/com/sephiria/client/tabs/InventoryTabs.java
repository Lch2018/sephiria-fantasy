package com.sephiria.client.tabs;

import com.sephiria.network.OpenBackpackPayload;
import com.sephiria.network.OpenShopPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

/** 原版背包上沿的 5 个标签页。 */
public final class InventoryTabs {
	private InventoryTabs() {
	}

	public static void add(Screen screen, int left, int top) {
		int x = left;
		Screens.getButtons(screen).add(new TabButton(x, top, TabButton.BACKPACK_ICON,
				Component.translatable("screen.sephiria_fantasy.backpack"),
				button -> ClientPlayNetworking.send(OpenBackpackPayload.CHANNEL, new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer()))));
		Screens.getButtons(screen).add(new TabButton(x + 22, top, TabButton.ATTRIBUTES_ICON,
				Component.translatable("screen.sephiria_fantasy.stats"),
				button -> Minecraft.getInstance().setScreen(new com.sephiria.client.gui.StatsScreen())));
		Screens.getButtons(screen).add(new TabButton(x + 44, top, TabButton.TALENT_ICON,
				Component.translatable("screen.sephiria_fantasy.talent"),
				button -> Minecraft.getInstance().setScreen(new com.sephiria.client.gui.TalentScreen())));
		Screens.getButtons(screen).add(new TabButton(x + 66, top, TabButton.SKILLS_ICON,
				Component.translatable("screen.sephiria_fantasy.artifact_skills"),
				button -> Minecraft.getInstance().setScreen(new com.sephiria.client.skill.ArtifactSkillScreen())));
		Screens.getButtons(screen).add(new TabButton(x + 88, top, TabButton.SHOP_ICON,
				Component.translatable("screen.sephiria_fantasy.shop"),
				button -> ClientPlayNetworking.send(OpenShopPayload.CHANNEL, new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer()))));
	}

	public static boolean applies(Screen screen) {
		return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
	}
}
