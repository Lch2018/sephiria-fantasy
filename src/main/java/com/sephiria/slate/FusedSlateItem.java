package com.sephiria.slate;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 合成石板：把两块石板的效果合并成一块新石板（用户 2026-10-24 定）。
 *
 * <p>它<b>没有固定的图案</b>——合并结果存在物品组件 {@link #PATTERN} 里，所以同一种物品的
 * 每个物品栈可以带完全不同的效果。合成规则见 {@link SlateFusion}：两块输入石板各自的朝向
 * 先应用，再按相对格子把增量相加。
 *
 * <p>另一条按输入决定的是「能不能转」：两块输入里<b>只要有一块不可旋转</b>，结果也不可旋转
 * （用户 2026-10-24 定）。这个布尔值同样存在物品组件 {@link #ROTATABLE} 里。
 *
 * <p>只能通过商人界面的「石板合成」得到：不进宝箱 / 商店 / 许愿池 / 挖矿掉落池
 * （那些地方都读 {@code ArtifactLoot.slates()}，它不在里面）。
 */
public class FusedSlateItem extends PatternSlateItem {
	/** 合并后的图案（相对坐标 + 增量）。网络同步是必须的：合成界面要在客户端画预览。 */
	public static final DataComponentType<List<Cell>> PATTERN = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "fused_slate_pattern"),
			DataComponentType.<List<Cell>>builder()
					.persistent(Cell.CODEC.listOf())
					.networkSynchronized(Cell.STREAM_CODEC.apply(ByteBufCodecs.list()))
					.build());

	/** 合成结果能不能按 R 旋转：两块输入都可旋转才为 true。 */
	public static final DataComponentType<Boolean> ROTATABLE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "fused_slate_rotatable"),
			DataComponentType.<Boolean>builder()
					.persistent(Codec.BOOL)
					.networkSynchronized(ByteBufCodecs.BOOL)
					.build());

	public FusedSlateItem(Properties properties) {
		// 品质按用户口径是红色（传说）；图案与可旋转性都来自组件，构造时的默认值只是兜底
		super(properties, ArtifactRarity.LEGENDARY, "artifact.sephiria_fantasy.slate_of_fusion.flavor",
				List.of(), false);
	}

	@Override
	public List<Cell> pattern(ItemStack stack) {
		return stack.getOrDefault(PATTERN, List.of());
	}

	@Override
	public boolean rotatable(ItemStack stack) {
		// 没有组件的合成石板（命令 / 旧数据）按不可旋转处理，免得绕过「输入有固定石板」的规则
		return stack.getOrDefault(ROTATABLE, false);
	}

	/** 图案 +（空图案时的「无效果」）+ 自动生成的描述 +「获取方式」那一行。 */
	@Override
	public List<Component> effectLines(ItemStack stack) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("artifact.sephiria_fantasy.slate_of_fusion.effect")
				.withStyle(ChatFormatting.GRAY));

		if (pattern(stack).isEmpty()) {
			lines.add(Component.translatable("artifact.sephiria_fantasy.slate_of_fusion.no_effect")
					.withColor(0xFFFF5555));
		}

		lines.addAll(super.effectLines(stack));
		lines.add(Component.translatable("artifact.sephiria_fantasy.slate_of_fusion.source")
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}
}
