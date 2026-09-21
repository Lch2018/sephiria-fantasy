package com.sephiria.util;

/** 数值格式化：提示框与界面里统一用它，免得各处小数位不一致。 */
public final class Numbers {
	private Numbers() {
	}

	/** 整数不带小数点，小数最多两位并去掉末尾的 0（0.80 → 0.8、10.25 保留）。 */
	public static String format(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.005D) {
			return String.valueOf((long) Math.rint(value));
		}

		String text = String.format("%.2f", value);

		while (text.endsWith("0")) {
			text = text.substring(0, text.length() - 1);
		}

		return text;
	}
}
