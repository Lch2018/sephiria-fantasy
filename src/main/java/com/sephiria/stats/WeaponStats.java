package com.sephiria.stats;

import com.sephiria.Sephiria;
import com.sephiria.weapon.SephiriaWeapon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 把属性系统接到武器的普通攻击上（伤害与攻速；技能另算，攻速不影响技能）。
 *
 * <p><b>基准从物品自己读</b>，不在代码里再抄一份数值：物品的 {@code ATTRIBUTE_MODIFIERS} 里，
 * ATTACK_DAMAGE 的加成等于"面板伤害 − 1"、ATTACK_SPEED 的加成等于"基础攻速 − 4"（1 是玩家
 * 基础攻击力、4 是玩家基础攻速），所以从当前手持物品就能反推出这把武器的基础值——刀这种
 * 两态武器切状态会换组件，读到的自然是当前状态的基准。
 *
 * <p>然后给玩家挂两个<b>临时修饰符</b>（按玩家、不写回物品，所以多人各算各的）：
 * <pre>
 *   伤害增量 = 面板伤害 × (总伤害倍率 − 1)
 *              总伤害倍率 = 物理强度/20 ×（1 + 物理伤害增幅）×（武器伤害/100）
 *   攻速增量 = 基础攻速 × (攻击速度总倍率 − 1)
 * </pre>
 * 属性为默认值时增量恰好为 0，也就是表里的数值原样生效。
 *
 * <p>不能只在"手持物变化"时才写：属性（神器、连击、药水）随时会变，所以每
 * {@value #CHECK_INTERVAL} tick 重算一次；写之前先跟属性表上的当前值比一下，一样就跳过，
 * 所以正常运行时这里只是一次读取。
 */
public final class WeaponStats {
	private static final Identifier DAMAGE_MODIFIER = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_weapon_damage");
	private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_weapon_speed");
	/** 检查间隔（tick）：换武器或属性变化后最多等这么久就会重算。 */
	private static final int CHECK_INTERVAL = 4;

	private static int ticker;

	private WeaponStats() {
	}

	/** 注册推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(WeaponStats::tick);
	}

	/** 立刻按当前属性和手持物刷新一次（进服、属性变化时调用）。 */
	public static void refresh(ServerPlayer player) {
		apply(player, of(player));
	}

	private static void tick(MinecraftServer server) {
		if (++ticker % CHECK_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			apply(player, of(player));
		}
	}

	private static double[] of(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();

		if (!(stack.getItem() instanceof SephiriaWeapon)) {
			return new double[]{0.0D, 0.0D};
		}

		double damage = 0.0D;
		double speed = 0.0D;

		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			Holder<Attribute> attribute = entry.attribute();

			if (attribute.is(Attributes.ATTACK_DAMAGE)) {
				damage = 1.0D + entry.modifier().amount();
			}
			else if (attribute.is(Attributes.ATTACK_SPEED)) {
				speed = Attributes.DEFAULT_ATTACK_SPEED + entry.modifier().amount();
			}
		}

		// 增量按「总倍率 − 1」算：总倍率里已经含了物理强度、物理伤害增幅、武器伤害与普通攻击伤害（攻速同理），
		// 全取默认值（物理强度 20、攻速 100%）时增量恰好是 0，表里的数值就原样生效。
		//
		// 这里必须用 PlayerStats 算好的**总值**：神器、连击、药水给的加成都只进总值，
		// 直接读 values.physical（永远是 20）会让增量恒为 0——普通攻击就一直是基础伤害。
		return new double[]{
				damage * (PlayerStats.normalAttackMultiplier(player) - 1.0D),
				speed * (PlayerStats.attackSpeedMultiplier(player) - 1.0D)};
	}

	private static void apply(ServerPlayer player, double[] deltas) {
		write(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, deltas[0]);
		write(player, Attributes.ATTACK_SPEED, SPEED_MODIFIER, deltas[1]);
	}

	/**
	 * 把一条增量写到玩家属性上（0 表示撤掉）。
	 *
	 * <p><b>以属性表上的当前值为准，而不是记「上次算出来是多少」</b>：复活会新建玩家实体，
	 * 而属性表只继承永久修饰符——这里挂的临时修饰符会随旧实体一起消失。只比对自家缓存的话，
	 * 复活后每一轮都会以为"已经写过了"而跳过，普通攻击就悄悄退回基础伤害。
	 */
	private static void write(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);

		if (instance == null) {
			return;
		}

		AttributeModifier current = instance.getModifier(id);

		if (amount == 0.0D) {
			if (current != null) {
				instance.removeModifier(id);
			}

			return;
		}

		// 数值没变就什么都不做——绝大多数 tick 走的是这条路，不重建修饰符
		if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}
