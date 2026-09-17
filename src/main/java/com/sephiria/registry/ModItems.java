package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.weapon.BaseWeapon;
import com.sephiria.weapon.SephiriaBoltItem;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * SEPHIRIA 的物品注册表。当前只有「基础武器」——六大分支各一把，还没有锻造系统。
 *
 * <p>数值按原作的分支命中倍率换算，以剑与盾为基准（6.0 伤害 / 1.6 攻速 / 250 耐久）：
 * <pre>
 * 分支        武器        伤害   攻速   耐久   倍率
 * 剑与盾      标准剑盾     6.0   1.6    250   100%
 * 大剑        钢铁巨剑     9.0   1.0    600   150%
 * 匕首        匕首         4.0   2.6    160   68.75%
 * 弩          重型弩       —     —      465   120%（远程，伤害取决于弹药）
 * 刀          刀           5.0   2.0    320   82.5%
 * 法杖/长棍   长棍         5.5   1.5    300   90%
 * </pre>
 *
 * <p>换算时要用的是「原版参数」，不是工具栏里显示的数字：显示的伤害 = 1（玩家基础）
 * + 材质加成 + 传入 {@code sword()} 的值，显示的攻速 = 4（玩家基础）+ 传入值。
 */
public final class ModItems {
	/** 玩家空手的基础攻击力，原版 Player#createAttributes 里是 1.0。 */
	private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0F;
	/** 弩的攻速：每秒 3 发。 */
	private static final double CROSSBOW_ATTACK_SPEED = 3.0D;

	public static final Item DEFAULT_SWORD_AND_SHIELD = sword("default_sword_and_shield", WeaponBranch.SWORD_AND_SHIELD, 6.0F, 1.6F, 250, false, true);
	public static final Item STEEL_GREATSWORD = sword("steel_greatsword", WeaponBranch.GREATSWORD, 9.0F, 1.0F, 600, false, false);
	/** 匕首：右键是招架/狂怒两段技能（见 {@link SephiriaDaggerItem}）。 */
	public static final Item DAGGER = register(key("dagger"),
			p -> new SephiriaDaggerItem(WeaponBranch.DAGGER, p),
			new Item.Properties()
					.sword(ToolMaterial.IRON,
							4.0F - PLAYER_BASE_ATTACK_DAMAGE - ToolMaterial.IRON.attackDamageBonus(),
							3.0F - (float) Attributes.DEFAULT_ATTACK_SPEED)
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
	public static final Item QUARTERSTAFF = sword("quarterstaff", WeaponBranch.STAFF, 5.5F, 1.5F, 300, false, false);

	/** 全部基础武器，顺序即创造模式标签页里的顺序。 */
	public static final List<BaseWeapon> BASE_WEAPONS = List.of(
			new BaseWeapon(WeaponBranch.SWORD_AND_SHIELD, DEFAULT_SWORD_AND_SHIELD),
			new BaseWeapon(WeaponBranch.GREATSWORD, STEEL_GREATSWORD),
			new BaseWeapon(WeaponBranch.DAGGER, DAGGER),
			new BaseWeapon(WeaponBranch.CROSSBOW, COLOSSAL_CROSSBOW),
			new BaseWeapon(WeaponBranch.KATANA, BLADE),
			new BaseWeapon(WeaponBranch.STAFF, QUARTERSTAFF));

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
