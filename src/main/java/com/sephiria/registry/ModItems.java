package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.artifact.CharmOfStrengthItem;
import com.sephiria.artifact.EnchantCoinItem;
import com.sephiria.artifact.WarriorsProofItem;
import com.sephiria.slate.FutureSlateItem;
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
	public static final Item SLATE_OF_FUTURE = register(key("slate_of_future"),
			p -> new FutureSlateItem(p),
			new Item.Properties().stacksTo(1));

	/** 神器附魔币（赛菲利亚道具）：右键打开附魔面板。 */
	public static final Item ENCHANT_COIN = register(key("enchant_coin"),
			p -> new EnchantCoinItem(p),
			new Item.Properties());

	/**
	 * 神器标签页的图标物品。
	 *
	 * <p>创造模式的标签页图标只能用物品栈，而设计要求用「袋子」那张图，所以注册一个只用于图标的
	 * 隐藏物品（不出现在任何标签页里，也不可获取）。
	 */
	public static final Item ARTIFACT_TAB_ICON = register(key("artifact_tab_icon"),
			Item::new,
			new Item.Properties());

	private ModItems() {
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
