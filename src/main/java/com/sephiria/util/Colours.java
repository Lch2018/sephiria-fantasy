package com.sephiria.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 颜色工具：目前只有「彩色」品质要用的逐字彩虹。 */
public final class Colours {
	/** 逐字循环的颜色（红→橙→黄→绿→青→蓝→紫）。 */
	private static final int[] RAINBOW = {
			0xFFFF5555, 0xFFFF9A3C, 0xFFFFE45B, 0xFF6BE86B, 0xFF5BD8E8, 0xFF6B8CFF, 0xFFC46BFF
	};

	private Colours() {
	}

	/** 把文本逐字上色成彩虹；名字这类短文本用它表示最高品质。 */
	public static MutableComponent rainbow(String text) {
		MutableComponent result = Component.empty();
		int index = 0;

		for (int i = 0; i < text.length(); i++) {
			String character = String.valueOf(text.charAt(i));
			result.append(Component.literal(character).withColor(RAINBOW[index % RAINBOW.length]));
			index++;
		}

		return result;
	}
}
