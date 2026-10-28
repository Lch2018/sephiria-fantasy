package com.sephiria.compat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 1.20.1 的物品属性只能通过 {@code getDefaultAttributeModifiers(EquipmentSlot)} 提供
 * （1.21+ 改成了 Item.Properties#attributes 里的数据组件）。这里把面板数值换算成原版加减值：
 * 面板攻击力 = 1（玩家基础）+ 修饰符，面板攻速 = 4（玩家基础）+ 修饰符。
 */
public final class WeaponAttributes {
	private static final UUID DAMAGE_ID = UUID.nameUUIDFromBytes("sephiria:weapon_attack_damage".getBytes());
	private static final UUID SPEED_ID = UUID.nameUUIDFromBytes("sephiria:weapon_attack_speed".getBytes());

	private WeaponAttributes() {
	}

	public static Multimap<Attribute, AttributeModifier> of(float panelDamage, float panelSpeed) {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE,
				new AttributeModifier(DAMAGE_ID, "sephiria:weapon_attack_damage",
						panelDamage - 1.0D, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED,
				new AttributeModifier(SPEED_ID, "sephiria:weapon_attack_speed",
						panelSpeed - 4.0D, AttributeModifier.Operation.ADDITION));
		return builder.build();
	}

	public static Multimap<Attribute, AttributeModifier> speedOnly(float panelSpeed) {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_SPEED,
				new AttributeModifier(SPEED_ID, "sephiria:weapon_attack_speed",
						panelSpeed - 4.0D, AttributeModifier.Operation.ADDITION));
		return builder.build();
	}
}
