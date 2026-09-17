package com.sephiria;

import com.sephiria.ability.Dash;
import com.sephiria.ability.DashSkill;
import com.sephiria.ability.Invulnerability;
import com.sephiria.ability.SkillStorage;
import com.sephiria.network.DashPayload;
import com.sephiria.network.InvulnerablePayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.network.SkillSyncPayload;
import com.sephiria.registry.ModCreativeTabs;
import com.sephiria.registry.ModItems;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaKatanaItem;
import com.sephiria.weapon.WeaponBranch;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sephiria implements ModInitializer {
	public static final String MOD_ID = "sephiria";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModCreativeTabs.initialize();
		OffHandGuard.register();
		SephiriaKatanaItem.registerEvents();

		// 固有技能系统：无敌窗口 + 通用突进 + 技能存储，冲刺/装填由客户端按键包触发。
		// 匕首的招架判定要在 Invulnerability 之前注册：后者会把伤害直接吃掉，事件按注册顺序派发
		SephiriaDaggerItem.register();
		Invulnerability.register();
		Dash.register();
		DashSkill.register();
		SephiriaCrossbowItem.register();
		SkillStorage.registerTicker();

		PayloadTypeRegistry.serverboundPlay().register(DashPayload.TYPE, DashPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ReloadPayload.TYPE, ReloadPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillSyncPayload.TYPE, SkillSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(InvulnerablePayload.TYPE, InvulnerablePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(DashPayload.TYPE,
				(payload, context) -> DashSkill.perform(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(ReloadPayload.TYPE,
				(payload, context) -> SephiriaCrossbowItem.tryReload(context.player()));

		// 进服时把各技能的存储量推给客户端，HUD 才有初始值
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SkillStorage.syncAll(handler.player));

		LOGGER.info("[SEPHIRIA] 武器系统已载入：{} 个分支，{} 把基础武器（锻造系统尚未实现）",
				WeaponBranch.values().length, ModItems.BASE_WEAPONS.size());
	}
}
