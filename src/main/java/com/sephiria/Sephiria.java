package com.sephiria;

import com.sephiria.ability.Dash;
import com.sephiria.ability.DashSkill;
import com.sephiria.ability.Invulnerability;
import com.sephiria.ability.SkillStorage;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackGridMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.network.DashPayload;
import com.sephiria.network.CloudSyncPayload;
import com.sephiria.network.DebuffSyncPayload;
import com.sephiria.network.InvulnerablePayload;
import com.sephiria.network.OpenBackpackPayload;
import com.sephiria.network.OpenShopPayload;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.UpgradeArtifactPayload;
import com.sephiria.network.RefreshShopPayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.network.SellItemPayload;
import com.sephiria.network.ShieldSweepPayload;
import com.sephiria.network.SkillSyncPayload;
import com.sephiria.network.StatsSyncPayload;
import com.sephiria.network.SunSwordSyncPayload;
import com.sephiria.potion.PotionTimers;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.debuff.Debuffs;
import com.sephiria.stats.Dodge;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PseudoRandom;
import com.sephiria.stats.Lifesteal;
import com.sephiria.stats.StatAttributes;
import com.sephiria.stats.PlayerStats;
import com.sephiria.stats.WeaponStats;
import com.sephiria.artifact.skill.ArtifactSkills;
import com.sephiria.artifact.skill.EncouragementBannerSkill;
import com.sephiria.artifact.skill.SkillSlots;
import com.sephiria.network.ArtifactSkillsPayload;
import com.sephiria.network.AssignArtifactSkillPayload;
import com.sephiria.network.CastArtifactSkillPayload;
import com.sephiria.network.OpenArtifactSkillsPayload;
import com.sephiria.registry.ModCreativeTabs;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.registry.ModSounds;
import com.sephiria.stats.TimedAttributes;
import net.minecraft.server.level.ServerPlayer;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaGreatswordItem;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
import com.sephiria.shop.ShopMenu;
import com.sephiria.shop.ShopStock;
import com.sephiria.slate.SlateItem;
import com.sephiria.weapon.WeaponSweep;
import com.sephiria.weapon.SephiriaKatanaItem;
import com.sephiria.weapon.WeaponBranch;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
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
		ModSounds.initialize();
		ModParticleTypes.initialize();
		ArtifactBackpack.register();
		// 商店货架的内容与限购次数存在玩家附件里：跨会话保留，重登/重启不会换一批货
		com.sephiria.shop.ShopStock.register();
		PlayerStats.register();
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
		// 闪避：排在武器自己的格挡/减伤之后，闪掉的那一下完全不存在
		Dodge.register();
		Dash.register();
		DashSkill.register();
		SephiriaCrossbowItem.register();
		SkillStorage.registerTicker();
		// 药水：按秒回血的计时器 + HP 偷取（造成伤害后按比例回血）
		PotionTimers.registerTicker();
		Lifesteal.register();
		// 减益效果：触电（魔法科技连击）挂在吸血同一个 AFTER_DAMAGE 上，结算那一击的实扣伤害
		Debuffs.register();
		WeaponStats.register();
		// 神器技能：技能注册、技能栏存档、限时增益（急速 / 旗帜）的推进器
		ArtifactSkills.initialize();
		SkillSlots.register();
		TimedAttributes.register();
		EncouragementBannerSkill.register();
		StatAttributes.register();
		// 魔法科技那批神器的钩子：树枝的附加闪电、萤火虫的受伤禁用、桑德耳环的周期闪电、雷之裁决的光束
		com.sephiria.artifact.LightningStruckBranchItem.register();
		com.sephiria.artifact.FireflyItem.register();
		com.sephiria.artifact.SandeEarringsItem.registerTicker();
		com.sephiria.artifact.skill.ThunderVerdictSkill.register();
		// 脱战/战斗状态（乌云容量回复用）与「乌云」连击（头顶乌云 + 周期雷击）；台风的武器附加闪电
		CombatState.register();
		com.sephiria.cloud.DarkCloud.registerTicker();
		com.sephiria.artifact.TyphoonScoreItem.register();
		// 「太阳剑」连击：武器/魔法书伤害投掷太阳剑（触发 + 数量回复/拾取/UI 同步）
		com.sephiria.sun.SunSword.register();
		com.sephiria.sun.SunSword.registerTicker();
		// 「余烬」连击的红蛇之眼：每 5 秒掉落的陨石（下落推进在它自己的推进器里）
		com.sephiria.artifact.RedSnakeEyeItem.registerTicker();

		PayloadTypeRegistry.serverboundPlay().register(DashPayload.TYPE, DashPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ReloadPayload.TYPE, ReloadPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ShieldSweepPayload.TYPE, ShieldSweepPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenShopPayload.TYPE, OpenShopPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SellItemPayload.TYPE, SellItemPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RefreshShopPayload.TYPE, RefreshShopPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RotateSlatePayload.TYPE, RotateSlatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenArtifactSkillsPayload.TYPE, OpenArtifactSkillsPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CastArtifactSkillPayload.TYPE, CastArtifactSkillPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AssignArtifactSkillPayload.TYPE, AssignArtifactSkillPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ArtifactSkillsPayload.TYPE, ArtifactSkillsPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpgradeArtifactPayload.TYPE, UpgradeArtifactPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillSyncPayload.TYPE, SkillSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CloudSyncPayload.TYPE, CloudSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DebuffSyncPayload.TYPE, DebuffSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SunSwordSyncPayload.TYPE, SunSwordSyncPayload.STREAM_CODEC);
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
		// 背包页与附魔页都能按 R 旋转石板：只带菜单格子编号。服务端先确认开着的是带
		// 背包格区的菜单、这一格真是背包格区（附魔页里背包格区前面还有神器槽/币槽），
		// 别的菜单或玩家背包区里的格子不认
		ServerPlayNetworking.registerGlobalReceiver(RotateSlatePayload.TYPE, (payload, context) -> {
			AbstractContainerMenu menu = context.player().containerMenu;

			if (!(menu instanceof BackpackGridMenu grid)
					|| payload.slot() < 0 || payload.slot() >= menu.slots.size()) {
				return;
			}

			Slot slot = menu.getSlot(payload.slot());

			if (grid.isBackpackSlot(slot) && slot.getItem().getItem() instanceof SlateItem) {
				SlateItem.rotate(slot.getItem());
				// 朝向变了：让背包重算格子等级（缓存里存着上次的结果），并把新组件同步给客户端
				slot.setChanged();
				menu.broadcastChanges();
			}
		});
		// 商店也是容器菜单（货架在服务端），同样只能由服务端打开
		ServerPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE,
				(payload, context) -> context.player().openMenu(ShopMenu.provider(context.player())));
		// 神器技能页：打开时把可用技能与 6 个栏位推过去；放入 / 清空后重推一次
		ServerPlayNetworking.registerGlobalReceiver(OpenArtifactSkillsPayload.TYPE,
				(payload, context) -> syncArtifactSkills(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(CastArtifactSkillPayload.TYPE,
				(payload, context) -> ArtifactSkills.cast(context.player(), payload.slot()));
		ServerPlayNetworking.registerGlobalReceiver(AssignArtifactSkillPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			String entry = payload.entry();

			// 只接受「背包里确实有、且生效」的神器：客户端塞别的过来一律当清空处理
			if (!entry.isEmpty() && ArtifactSkills.resolve(player, entry) == null) {
				return;
			}

			SkillSlots.set(player, payload.slot(), entry);
			syncArtifactSkills(player);
		});

		// 商店页面点「刷新」：吃掉骰子栏里的一颗骰子，重刷一批货
		ServerPlayNetworking.registerGlobalReceiver(RefreshShopPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof ShopMenu menu) {
				menu.refresh(context.player());
			}
		});

		// 商店页面点「出售」：卖出售栏里的东西，按原价 3 折换叶子
		ServerPlayNetworking.registerGlobalReceiver(SellItemPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof ShopMenu menu) {
				menu.sell(context.player());
			}
		});

		// 附魔面板：点「升级 N 级」——从附魔币栏扣币，把神器升上去（不会溢出）
		ServerPlayNetworking.registerGlobalReceiver(UpgradeArtifactPayload.TYPE,
				(payload, context) -> {
					if (context.player().containerMenu instanceof EnchantMenu menu) {
						menu.upgrade(payload.levels());
					}
				});

		// 赛菲利亚货币进自然生成的箱子（骰子 + 神器附魔币）：所有 minecraft:chests/* 的表
		// 各加一个 10% 概率的池子
		com.sephiria.shop.ChestLoot.register();

		// 进服时把各技能的存储量推给客户端，HUD 才有初始值
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SkillStorage.syncAll(handler.player);
			PlayerStats.syncOnJoin(handler.player);
			WeaponStats.refresh(handler.player);
			StatAttributes.refresh(handler.player);
		});

		// 退出时把背包与属性从缓存里放掉（附件里已经是最新状态，下次进来重新读）
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ArtifactBackpack.forget(handler.player);
			PlayerStats.forget(handler.player);
			PseudoRandom.forget(handler.player);
			SephiriaDamage.forget(handler.player);
			Debuffs.forget(handler.player);
			com.sephiria.artifact.LightningStruckBranchItem.forget(handler.player);
			com.sephiria.artifact.FireflyItem.forget(handler.player);
			com.sephiria.artifact.SandeEarringsItem.forget(handler.player);
		com.sephiria.artifact.RedSnakeEyeItem.forget(handler.player);
			CombatState.forget(handler.player);
			com.sephiria.cloud.DarkCloud.forget(handler.player);
			com.sephiria.sun.SunSword.forget(handler.player);
			ArtifactSkills.forget(handler.player);
			Dodge.forget(handler.player);
			ShopStock.forget(handler.player);
		});

		LOGGER.info("[SEPHIRIA] 武器系统已载入：{} 个分支，{} 把基础武器（锻造系统尚未实现）",
				WeaponBranch.values().length, ModItems.BASE_WEAPONS.size());
	}
	/** 把神器技能页需要的数据推给客户端（可用技能 + 6 个栏位）。 */
	private static void syncArtifactSkills(ServerPlayer player) {
		java.util.List<ArtifactSkillsPayload.Entry> skills = new java.util.ArrayList<>();

		for (ArtifactSkills.Available available : ArtifactSkills.available(player)) {
			skills.add(new ArtifactSkillsPayload.Entry(
					net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(available.item()), available.level()));
		}

		ServerPlayNetworking.send(player, new ArtifactSkillsPayload(skills, java.util.List.copyOf(SkillSlots.of(player))));
	}

}
