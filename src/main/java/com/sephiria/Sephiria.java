package com.sephiria;

import com.sephiria.ability.Dash;
import com.sephiria.ability.DashSkill;
import com.sephiria.ability.Invulnerability;
import com.sephiria.ability.SkillStorage;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.network.DashPayload;
import com.sephiria.network.InvulnerablePayload;
import com.sephiria.network.OpenBackpackPayload;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.UpgradeArtifactPayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.network.ShieldSweepPayload;
import com.sephiria.network.SkillSyncPayload;
import com.sephiria.network.StatsSyncPayload;
import com.sephiria.stats.PlayerStats;
import com.sephiria.stats.WeaponStats;
import com.sephiria.registry.ModCreativeTabs;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaGreatswordItem;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
import com.sephiria.slate.SlateItem;
import com.sephiria.weapon.WeaponSweep;
import com.sephiria.weapon.SephiriaKatanaItem;
import com.sephiria.weapon.WeaponBranch;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.ItemStack;
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
		ModMenus.initialize();
		ArtifactBackpack.register();
		OffHandGuard.register();
		SephiriaKatanaItem.registerEvents();
		SephiriaKatanaItem.register();

		// 固有技能系统：无敌窗口 + 通用突进 + 技能存储，冲刺/装填由客户端按键包触发。
		// 匕首：招架/狂怒两段技能（招架窗口自行挡伤害，奖励延后一刻发放）
		SephiriaDaggerItem.register();
		SephiriaGreatswordItem.register();
		SephiriaStaffItem.register();
		WeaponSweep.register();
		SephiriaShieldItem.register();
		// 注意注册顺序：ALLOW_DAMAGE 的调用器是"第一个返回 false 的处理器直接短路"，
		// 所以各武器自己的格挡窗口（刀的切换、匕首的招架、长棍的回击）必须排在
		// Invulnerability 这个"通用无敌"之前，否则它们收不到事件、奖励也就发不出来。
		Invulnerability.register();
		Dash.register();
		DashSkill.register();
		SephiriaCrossbowItem.register();
		SkillStorage.registerTicker();
		WeaponStats.register();

		PayloadTypeRegistry.serverboundPlay().register(DashPayload.TYPE, DashPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ReloadPayload.TYPE, ReloadPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ShieldSweepPayload.TYPE, ShieldSweepPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RotateSlatePayload.TYPE, RotateSlatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpgradeArtifactPayload.TYPE, UpgradeArtifactPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillSyncPayload.TYPE, SkillSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(InvulnerablePayload.TYPE, InvulnerablePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StatsSyncPayload.TYPE, StatsSyncPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(DashPayload.TYPE,
				(payload, context) -> DashSkill.perform(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(ReloadPayload.TYPE,
				(payload, context) -> SephiriaCrossbowItem.tryReload(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(ShieldSweepPayload.TYPE,
				(payload, context) -> SephiriaShieldItem.tryDefendSweep(context.player()));
		// 赛菲利亚背包是容器菜单，必须由服务端打开（客户端只负责画那个标签页按钮）
		ServerPlayNetworking.registerGlobalReceiver(OpenBackpackPayload.TYPE,
				(payload, context) -> context.player().openMenu(ArtifactBackpackMenu.provider(context.player())));
		// 背包装界里按 R 旋转石板：只带格子编号，服务端自己查朝向再接 90°
		ServerPlayNetworking.registerGlobalReceiver(RotateSlatePayload.TYPE, (payload, context) -> {
			if (!(context.player().containerMenu instanceof ArtifactBackpackMenu menu)) {
				return;
			}

			ItemStack stack = menu.getSlot(payload.slot()).getItem();

			if (stack.getItem() instanceof SlateItem) {
				SlateItem.rotate(stack);
				// 朝向变了：让背包重算格子等级（缓存里存着上次的结果），并把新组件同步给客户端
				menu.container().setChanged();
				menu.broadcastChanges();
			}
		});
		// 附魔面板：点「升级 N 级」——从附魔币栏扣币，把神器升上去（不会溢出）
		ServerPlayNetworking.registerGlobalReceiver(UpgradeArtifactPayload.TYPE,
				(payload, context) -> {
					if (context.player().containerMenu instanceof EnchantMenu menu) {
						menu.upgrade(payload.levels());
					}
				});

		// 进服时把各技能的存储量推给客户端，HUD 才有初始值
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SkillStorage.syncAll(handler.player);
			PlayerStats.syncOnJoin(handler.player);
			WeaponStats.refresh(handler.player);
		});

		// 退出时把背包从缓存里放掉（附件里已经是最新状态，下次进来重新读）
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ArtifactBackpack.forget(handler.player));

		LOGGER.info("[SEPHIRIA] 武器系统已载入：{} 个分支，{} 把基础武器（锻造系统尚未实现）",
				WeaponBranch.values().length, ModItems.BASE_WEAPONS.size());
	}
}
