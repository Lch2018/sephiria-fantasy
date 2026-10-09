package com.sephiria.registry;

import com.sephiria.Sephiria;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * SEPHIRIA 的自定义音效：原版没有合适的素材就用程序生成的（见 {@code tools/make-electric-spark.ps1}）。
 *
 * <p>必须在 {@code Sephiria#onInitialize()} 里 {@link #initialize()} 一次——注册要赶在原版注册表
 * 冻结之前完成，等游玩时才初始化就晚了。{@code sounds.json} 负责把每个音效 id 映射到
 * {@code sounds/} 里的 .ogg 文件（一个 id 挂多个文件 = 播放时随机挑一个变体）。
 */
public final class ModSounds {
	/** 电火花「呲呲啦啦」：触电结算与雷之裁决光束的短电流声（约 0.3~0.4 秒，三个变体随机）。 */
	public static final SoundEvent ELECTRIC_SPARK = register("electric_spark");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** 由 {@code Sephiria#onInitialize()} 调用：引用一次本类即触发静态字段的注册。 */
	public static void initialize() {
	}
}
