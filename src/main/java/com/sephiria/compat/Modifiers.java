package com.sephiria.compat;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * 1.20.1 的 {@code addOrUpdateTransientModifier} 还不存在，而且同名修饰符不会自动替换。
 * 统一走「先按 UUID 摘掉、再挂新的」，语义与 1.21+ 的 addOrUpdate 一致。
 */
public final class Modifiers {
	private Modifiers() {
	}

	public static void setTransient(AttributeInstance instance, UUID id, String name, double amount,
			AttributeModifier.Operation operation) {
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
	}
}
