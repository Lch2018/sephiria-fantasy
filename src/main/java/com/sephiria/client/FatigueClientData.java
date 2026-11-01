package com.sephiria.client;

/** 客户端疲劳镜像。 */
public final class FatigueClientData {
	private static int points;

	private FatigueClientData() {
	}

	public static void accept(int value) {
		points = Math.max(0, Math.min(6000, value));
	}

	public static int points() {
		return points;
	}

	public static void reset() {
		points = 0;
	}
}
