package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ColorlessCubeItem;
import com.sephiria.artifact.SilverPlateItem;
import com.sephiria.artifact.EncouragementBannerItem;
import com.sephiria.artifact.HasteGrimoireItem;
import com.sephiria.artifact.RedDewItem;
import com.sephiria.artifact.LongingAmuletItem;
import com.sephiria.artifact.FaultProbeItem;
import com.sephiria.artifact.DeftAmuletItem;
import com.sephiria.artifact.PinwheelItem;
import com.sephiria.artifact.SpecimenBeakItem;
import com.sephiria.artifact.EvergreenCloakItem;
import com.sephiria.artifact.ResistanceBandItem;
import com.sephiria.artifact.WarmStoneItem;
import com.sephiria.artifact.CharmOfStrengthItem;
import com.sephiria.artifact.ChestItem;
import com.sephiria.artifact.DiceItem;
import com.sephiria.artifact.ShieldTextbookItem;
import com.sephiria.artifact.SwordTextbookItem;
import com.sephiria.artifact.WindScoreItem;
import com.sephiria.artifact.EnchantCoinItem;
import com.sephiria.artifact.GoldenCloakItem;
import com.sephiria.artifact.PressureBandageItem;
import com.sephiria.artifact.ProjectionSwordItem;
import com.sephiria.artifact.WandererNecklaceItem;
import com.sephiria.artifact.WarriorsProofItem;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.potion.PotionEffect;
import com.sephiria.potion.Potions;
import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.slate.PatternSlateItem;
import com.sephiria.slate.Slates;
import com.sephiria.weapon.BaseWeapon;
import com.sephiria.weapon.SephiriaBoltItem;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaGreatswordItem;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
import com.sephiria.weapon.SephiriaKatanaItem;
import com.sephiria.weapon.SephiriaWeaponItem;
import com.sephiria.weapon.WeaponBranch;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseEffects;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * SEPHIRIA 的物品注册表。当前只有「基础武器」——六大分支各一把，还没有锻造系统。
 *
 * <p>各武器的当前数值以 {@code 武器数据表.txt} 为准，物品注册只负责把那些数值写进
 * 数据组件（伤害/攻速尽可能引用武器类自己的常量，避免两处各写一份）。
 *
 * <p>要注意传进 {@code sword()} 的是「原版参数」，不是工具栏里显示的数字：
 * 显示的伤害 = 1（玩家基础）+ 材质加成 + 传入值，显示的攻速 = 4（玩家基础）+ 传入值。
 */
public final class ModItems {
	/** 玩家空手的基础攻击力，原版 Player#createAttributes 里是 1.0。 */
	private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0F;
	/** 弩的攻速：每秒 3 发。 */
	private static final double CROSSBOW_ATTACK_SPEED = 3.0D;

	public static final Item DEFAULT_SWORD_AND_SHIELD = register(key("default_sword_and_shield"),
			p -> new SephiriaShieldItem(WeaponBranch.SWORD_AND_SHIELD, p),
			new Item.Properties()
					.sword(ToolMaterial.IRON,
							SephiriaShieldItem.ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE - ToolMaterial.IRON.attackDamageBonus(),
							SephiriaShieldItem.ATTACK_SPEED - (float) Attributes.DEFAULT_ATTACK_SPEED)
					.durability(250)
					// 原版使用物品期间会把人拖到 20% 移速（UseEffects.DEFAULT 的 speed_multiplier=0.2）。
					// 防御是持续按住的姿态，被拖慢会让"举盾走位"完全没法玩，所以这里显式改成不减速、
					// 也允许疾跑——这是原版给的开关，不用去 mixin 里改移动逻辑。
					.component(DataComponents.USE_EFFECTS, new UseEffects(true, true, 1.0F))
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));
	/** 钢铁巨剑：左键横扫 + 右键蓄力「旋风」（见 {@link SephiriaGreatswordItem}）。 */
	public static final Item STEEL_GREATSWORD = register(key("steel_greatsword"),
			p -> new SephiriaGreatswordItem(WeaponBranch.GREATSWORD, p),
			new Item.Properties()
					.sword(ToolMaterial.IRON,
							SephiriaGreatswordItem.ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE - ToolMaterial.IRON.attackDamageBonus(),
							SephiriaGreatswordItem.ATTACK_SPEED - (float) Attributes.DEFAULT_ATTACK_SPEED)
					.durability(600)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));
	/** 匕首：右键是招架/狂怒两段技能（见 {@link SephiriaDaggerItem}）。 */
	public static final Item DAGGER = register(key("dagger"),
			p -> new SephiriaDaggerItem(WeaponBranch.DAGGER, p),
			new Item.Properties()
					.sword(ToolMaterial.IRON,
							SephiriaDaggerItem.ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE - ToolMaterial.IRON.attackDamageBonus(),
							SephiriaDaggerItem.ATTACK_SPEED - (float) Attributes.DEFAULT_ATTACK_SPEED)
					.durability(160)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));
	public static final Item COLOSSAL_CROSSBOW = crossbow("colossal_crossbow", WeaponBranch.CROSSBOW, SephiriaCrossbowItem.MAGAZINE_SIZE);
	/** 重型弩的专用弹药：弩矢。弩不消耗、也不需要背包里有原版箭矢。 */
	public static final Item CROSSBOW_BOLT = Registry.register(
			BuiltInRegistries.ITEM,
			key("crossbow_bolt"),
			new SephiriaBoltItem(new Item.Properties().stacksTo(64).setId(key("crossbow_bolt"))));
	/** 刀：GeckoLib 骨骼模型 + 出鞘/入鞘两态（见 {@link SephiriaKatanaItem}）。 */
	public static final Item BLADE = register(key("blade"),
			p -> new SephiriaKatanaItem(WeaponBranch.KATANA, p),
			new Item.Properties()
					// sword() 只是为了拿到 Tool 组件（切蛛网、剑类方块加速）和通用属性；
					// 它写的攻击力/攻速随后被下面按状态的修饰符覆盖，所以这里传 0。
					.sword(ToolMaterial.IRON, 0.0F, 0.0F)
					.durability(320)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
					.component(DataComponents.ATTRIBUTE_MODIFIERS, SephiriaKatanaItem.attributeModifiers(false))
					.component(SephiriaKatanaItem.SHEATHED, Boolean.FALSE));
	/** 长棍：左键横扫 + 右键两段「回击」（见 {@link SephiriaStaffItem}）。 */
	public static final Item QUARTERSTAFF = register(key("quarterstaff"),
			p -> new SephiriaStaffItem(WeaponBranch.STAFF, p),
			new Item.Properties()
					.sword(ToolMaterial.IRON,
							SephiriaStaffItem.ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE - ToolMaterial.IRON.attackDamageBonus(),
							SephiriaStaffItem.ATTACK_SPEED - (float) Attributes.DEFAULT_ATTACK_SPEED)
					.durability(300)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

	/** 全部基础武器，顺序即创造模式标签页里的顺序。 */
	public static final List<BaseWeapon> BASE_WEAPONS = List.of(
			new BaseWeapon(WeaponBranch.SWORD_AND_SHIELD, DEFAULT_SWORD_AND_SHIELD),
			new BaseWeapon(WeaponBranch.GREATSWORD, STEEL_GREATSWORD),
			new BaseWeapon(WeaponBranch.DAGGER, DAGGER),
			new BaseWeapon(WeaponBranch.CROSSBOW, COLOSSAL_CROSSBOW),
			new BaseWeapon(WeaponBranch.KATANA, BLADE),
			new BaseWeapon(WeaponBranch.STAFF, QUARTERSTAFF));

	// ------------------------------------------------------------------ 神器

	/** 力量护符（坚固）：【唯一】物理伤害 +2/3/4/6。 */
	public static final Item CHARM_OF_STRENGTH = register(key("charm_of_strength"),
			p -> new CharmOfStrengthItem(p),
			new Item.Properties().stacksTo(1));

	/** 战士的证明（坚固，高级品质）：【唯一】物理伤害 +1..8、攻击速度 +5..10%。 */
	public static final Item WARRIORS_PROOF = register(key("warriors_proof"),
			p -> new WarriorsProofItem(p),
			new Item.Properties().stacksTo(1));

	/** 石板「未来」（稀有品质）：让周围格子等级 +1，可按 R 旋转。 */
	public static final Item SLATE_OF_FUTURE = registerSlate("slate_of_future", ArtifactRarity.ADVANCED,
			Slates.FUTURE, true);

	/** 石板 · 誓言（稀有）：正上方 +2 / +1、左右与下方各 +1。 */
	public static final Item SLATE_OF_OATH = registerSlate("slate_of_oath", ArtifactRarity.RARE,
			Slates.OATH, true);

	/** 石板 · 信念（传说）：正上方一格 +5。 */
	public static final Item SLATE_OF_BELIEF = registerSlate("slate_of_belief", ArtifactRarity.LEGENDARY,
			Slates.BELIEF, true);

	/** 石板 · 入口（高级）：正上方一排 +1 / +2 / +1，<b>不可旋转</b>。 */
	public static final Item SLATE_OF_ENTRANCE = registerSlate("slate_of_entrance", ArtifactRarity.ADVANCED,
			Slates.ENTRANCE, false);

	/** 石板 · 竞争（高级）：左上方与正上方各 -1，正下方 +3。 */
	public static final Item SLATE_OF_COMPETITION = registerSlate("slate_of_competition", ArtifactRarity.ADVANCED,
			Slates.COMPETITION, true);

	/** 石板 · 集结（高级）：正上方与正左方各 +2。 */
	public static final Item SLATE_OF_RALLY = registerSlate("slate_of_rally", ArtifactRarity.ADVANCED,
			Slates.RALLY, true);

	/** 石板 · 波浪（高级）：正上方 -1、正右方 -1、右上方 +3。 */
	public static final Item SLATE_OF_WAVE = registerSlate("slate_of_wave", ArtifactRarity.ADVANCED,
			Slates.WAVE, true);

	/** 石板 · 双星（高级）：正上方与正下方各 +2。 */
	public static final Item SLATE_OF_DOUBLE_STAR = registerSlate("slate_of_double_star", ArtifactRarity.ADVANCED,
			Slates.DOUBLE_STAR, true);

	/** 石板 · 握手（普通）：正上方与正下方各 +1。 */
	public static final Item SLATE_OF_HANDSHAKE = registerSlate("slate_of_handshake", ArtifactRarity.COMMON,
			Slates.HANDSHAKE, true);

	/** 石板统一走通用实现：范围与增量都在 Slates 里，这里只挑品质与风味文本。 */
	private static Item registerSlate(String name, ArtifactRarity rarity, java.util.List<PatternSlateItem.Cell> pattern,
			boolean rotatable) {
		return register(key(name),
				p -> new PatternSlateItem(p, rarity, "artifact.sephiria." + name + ".flavor", pattern, rotatable),
				new Item.Properties().stacksTo(1));
	}

	/** 神器附魔币（赛菲利亚道具）：右键打开附魔面板。 */
	public static final Item ENCHANT_COIN = register(key("enchant_coin"),
			p -> new EnchantCoinItem(p),
			new Item.Properties());

	/** 盾牌术教材（坚固，普通）：无【唯一】，特殊攻击伤害 +5/8%。 */
	public static final Item SHIELD_TEXTBOOK = register(key("shield_textbook"),
			p -> new ShieldTextbookItem(p),
			new Item.Properties().stacksTo(1));

	/** 剑术教材（风之歌，稀有）：【唯一】攻击速度 +7/14/26%。 */
	public static final Item SWORD_TEXTBOOK = register(key("sword_textbook"),
			p -> new SwordTextbookItem(p),
			new Item.Properties().stacksTo(1));

	/** 乐谱「风」（风之歌，高级）：【唯一】近战攻击范围 +10..50%。 */
	public static final Item WIND_SCORE = register(key("wind_score"),
			p -> new WindScoreItem(p),
			new Item.Properties().stacksTo(1));

	/** 压迫绷带（风之歌，普通）：冲刺恢复速度 +10/15/30%。 */
	public static final Item PRESSURE_BANDAGE = register(key("pressure_bandage"),
			p -> new PressureBandageItem(p),
			new Item.Properties().stacksTo(1));

	/** 金色斗篷（风之歌，高级）：冲刺次数 +1/1/2、攻速 +3/6/9%。 */
	public static final Item GOLDEN_CLOAK = register(key("golden_cloak"),
			p -> new GoldenCloakItem(p),
			new Item.Properties().stacksTo(1));

	/** 流浪者的项链（风之歌，稀有）：攻速 +6/12/18%、最高元素伤害 +2/4/6。 */
	public static final Item WANDERER_NECKLACE = register(key("wanderer_necklace"),
			p -> new WandererNecklaceItem(p),
			new Item.Properties().stacksTo(1));

	/** 阿格玛投影剑190,191号（坚固，稀有）：物理伤害 +2..14（8 档）。 */
	public static final Item PROJECTION_SWORD = register(key("projection_sword"),
			p -> new ProjectionSwordItem(p),
			new Item.Properties().stacksTo(1));

	/** 无色立方体（风之歌，稀有）：无视防御伤害 +1/1/2/2/3、移动速度与攻击速度 +5/6/7/9/12%。 */
	public static final Item COLORLESS_CUBE = register(key("colorless_cube"),
			p -> new ColorlessCubeItem(p),
			new Item.Properties().stacksTo(1));

	/** 银盘子（坚固，普通）：【唯一】普通攻击伤害 +3/6/10%、特殊攻击伤害 +3/6/10%。 */
	public static final Item SILVER_PLATE = register(key("silver_plate"),
			p -> new SilverPlateItem(p),
			new Item.Properties().stacksTo(1));

	/** 鼓励旗帜（风之歌，稀有）：发动型神器，召唤旗帜给范围内友军 +10/20/30% 攻速。 */
	public static final Item ENCOURAGEMENT_BANNER = register(key("encouragement_banner"),
			p -> new EncouragementBannerItem(p),
			new Item.Properties().stacksTo(1));

	/** 急速（风之歌，高级，魔法书）：【唯一】获得技能「急速」。 */
	public static final Item HASTE_GRIMOIRE = register(key("haste_grimoire"),
			p -> new HasteGrimoireItem(p),
			new Item.Properties().stacksTo(1));

	/** 红色露水（精密，普通）：【唯一】暴击时对周围敌人造成最高属性值 10% 的伤害。 */
	public static final Item RED_DEW = register(key("red_dew"),
			p -> new RedDewItem(p),
			new Item.Properties().stacksTo(1));

	/** 渴望护符（精密，普通）：【唯一】武器攻击的暴击几率 +3/6/10/14%。 */
	public static final Item LONGING_AMULET = register(key("longing_amulet"),
			p -> new LongingAmuletItem(p),
			new Item.Properties().stacksTo(1));

	/** 故障探测针（精密，稀有）：【唯一】暴击几率 +3/6/9/12/15/18/21%。 */
	public static final Item FAULT_PROBE = register(key("fault_probe"),
			p -> new FaultProbeItem(p),
			new Item.Properties().stacksTo(1));

	/** 灵巧护符（影子，普通）：闪避 +3/6。 */
	public static final Item DEFT_AMULET = register(key("deft_amulet"),
			p -> new DeftAmuletItem(p),
			new Item.Properties().stacksTo(1));

	/** 风车（风之歌 + 影子，高级）：【唯一】双连击，攻速 +3/6/9%、闪避 +2/4/6。 */
	public static final Item PINWHEEL = register(key("pinwheel"),
			p -> new PinwheelItem(p),
			new Item.Properties().stacksTo(1));

	/** 标本喙（影子，稀有）：【唯一】物理伤害 +1/2/3/5/8、闪避 +2/3/4/5/6。 */
	public static final Item SPECIMEN_BEAK = register(key("specimen_beak"),
			p -> new SpecimenBeakItem(p),
			new Item.Properties().stacksTo(1));

	/** 常青斗篷（影子，高级）：【唯一】冲刺次数 +0/1/1、闪避 +4/6/8、暴击几率 +1/2/4%。 */
	public static final Item EVERGREEN_CLOAK = register(key("evergreen_cloak"),
			p -> new EvergreenCloakItem(p),
			new Item.Properties().stacksTo(1));

	/** 弹力带（影子，高级）：【唯一】闪避触发回冲刺、暴击伤害 +2/4/6%、闪避 +1/2/3。 */
	public static final Item RESISTANCE_BAND = register(key("resistance_band"),
			p -> new ResistanceBandItem(p),
			new Item.Properties().stacksTo(1));

	/** 温暖的石头（精密，稀有）：【唯一】暴击伤害 +10/20/30/40%。 */
	public static final Item WARM_STONE = register(key("warm_stone"),
			p -> new WarmStoneItem(p),
			new Item.Properties().stacksTo(1));

	/** 骰子（赛菲利亚道具）：商店刷新用，可堆叠；自然箱子里 10% 概率开出。 */
	public static final Item DICE = register(key("dice"),
			p -> new DiceItem(p),
			new Item.Properties());

	/** 神器宝箱（赛菲利亚道具）：右键开出 5 件随机神器，选一件拿走。 */
	public static final Item ARTIFACT_CHEST = register(key("artifact_chest"),
			p -> new ChestItem(ChestItem.Kind.ARTIFACT, p),
			new Item.Properties().stacksTo(1));

	/** 石板宝箱：同上，奖池是石板。 */
	public static final Item SLATE_CHEST = register(key("slate_chest"),
			p -> new ChestItem(ChestItem.Kind.SLATE, p),
			new Item.Properties().stacksTo(1));

	/** 升级宝箱：每 1000 点经验发一个，奖池混着神器与石板，好东西更少。 */
	public static final Item UPGRADE_CHEST = register(key("upgrade_chest"),
			p -> new ChestItem(ChestItem.Kind.UPGRADE, p),
			new Item.Properties().stacksTo(1));

	/**
	 * 神器标签页的图标物品。
	 *
	 * <p>创造模式的标签页图标只能用物品栈，而设计要求用「袋子」那张图，所以注册一个只用于图标的
	 * 隐藏物品（不出现在任何标签页里，也不可获取）。
	 */
	public static final Item SLATE_TAB_ICON = register(key("slate_tab_icon"),
			Item::new,
			new Item.Properties());

	public static final Item ARTIFACT_TAB_ICON = register(key("artifact_tab_icon"),
			Item::new,
			new Item.Properties());

	/** 再生药水（白）：立即回复最大生命值的 20%。 */
	public static final Item REGENERATION_POTION = registerPotion("regeneration_potion", Potions.REGENERATION,
			Potions.REGENERATION_RARITY);
	/** 苹果汁（白）：每秒回 1 点生命，持续 30 秒。 */
	public static final Item APPLE_JUICE = registerPotion("apple_juice", Potions.APPLE_JUICE,
			Potions.APPLE_JUICE_RARITY);
	/** 特拉普派的神圣（蓝）：物理伤害 +2，永久。 */
	public static final Item TRAPPIST_SACRED = registerPotion("trappist_sacred", Potions.TRAPPIST_SACRED,
			Potions.TRAPPIST_SACRED_RARITY);
	/** 大骰子药水（黄）：立即获得 3 个骰子。 */
	public static final Item BIG_DICE_POTION = registerPotion("big_dice_potion", Potions.BIG_DICE,
			Potions.BIG_DICE_RARITY);
	/** 吸血鬼领主的誓约（红）：HP 偷取 +1，永久。 */
	public static final Item VAMPIRE_LORD_OATH = registerPotion("vampire_lord_oath", Potions.VAMPIRE_LORD_OATH,
			Potions.VAMPIRE_LORD_OATH_RARITY);

	/** 药水池（商店的两个药水格按品质加权从这里抽）。 */
	public static final List<Item> POTIONS = List.of(REGENERATION_POTION, APPLE_JUICE, TRAPPIST_SACRED,
			BIG_DICE_POTION, VAMPIRE_LORD_OATH);

	private ModItems() {
	}

	/** 药水统一注册：不可堆叠（和原版药水一样，喝完手里留一个玻璃瓶）。 */
	private static Item registerPotion(String name, PotionEffect effect, ArtifactRarity rarity) {
		return register(key(name),
				p -> new SephiriaPotionItem(p, effect, rarity),
				new Item.Properties().stacksTo(1));
	}

	/** 注册发生在类加载时，由 {@link Sephiria#onInitialize()} 触发。 */
	public static void initialize() {
	}

	private static Item sword(String name, WeaponBranch branch, float attackDamage, float attackSpeed, int durability, boolean drawAnimation, boolean blocks) {
		ResourceKey<Item> key = key(name);
		Item.Properties properties = new Item.Properties()
				.sword(ToolMaterial.IRON,
						attackDamage - PLAYER_BASE_ATTACK_DAMAGE - ToolMaterial.IRON.attackDamageBonus(),
						attackSpeed - (float) Attributes.DEFAULT_ATTACK_SPEED)
				.durability(durability)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE);

		if (blocks) {
			// 剑盾的格挡参数：90° 格挡角、满减伤、按格挡次数掉耐久。
			// 这里直接构造而不是抄原版盾牌的组件——物品注册发生在组件绑定之前，
			// 初始化期读 Items.SHIELD 的组件会抛 "Components not bound yet"。
			properties = properties.component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
					0.0F,
					1.0F,
					List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 1.0F, 1.0F)),
					BlocksAttacks.ItemDamageFunction.DEFAULT,
					Optional.empty(),
					Optional.empty(),
					Optional.empty()));
		}

		return register(key, p -> new SephiriaWeaponItem(branch, drawAnimation, blocks, p), properties);
	}

	private static Item crossbow(String name, WeaponBranch branch, int durability) {
		ResourceKey<Item> key = key(name);
		// 与原版弩一致：不可堆叠、465 耐久、带一个空的装填组件、附魔能力 1。
		// 攻速 3 = 每秒 3 发（射击间隔在物品冷却里按 20/3 取整实现）。
		Item.Properties properties = new Item.Properties()
				.stacksTo(1)
				.durability(durability)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
				.component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY)
				.component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
						.add(Attributes.ATTACK_SPEED,
								new AttributeModifier(
										Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "crossbow_attack_speed"),
										CROSSBOW_ATTACK_SPEED - Attributes.DEFAULT_ATTACK_SPEED,
										AttributeModifier.Operation.ADD_VALUE),
								EquipmentSlotGroup.MAINHAND)
						.build())
				.enchantable(1);

		return register(key, p -> new SephiriaCrossbowItem(branch, p), properties);
	}

	private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
		// setId 必须在构造之前调用：物品 id 本身就是一个默认数据组件。
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static ResourceKey<Item> key(String name) {
		return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name));
	}
}
