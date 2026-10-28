package com.sephiria.client;

import com.sephiria.network.TalentSyncPayload;
import com.sephiria.talent.Talent;

/**
 * 客户端的「天赋点」镜像：天赋界面上的数字全部读这里。
 *
 * <p>服务端在进服、加点、退点、重置与获得天赋点时各推一次，所以客户端不需要自己算；
 * 没收到过包时按 0 显示。
 */
public final class ClientTalents {
	private static int earned;
	private static int rage;
	private static int swift;
	private static int will;
	private static int quickWit;
	private static int survival;
	private static int patience;
	private static int wisdom;
	/** 服务端算的「能否在出生点附近改天赋」：世界出生点或玩家自己设置的床 / 重生锚都算。 */
	private static boolean nearSpawn;

	private ClientTalents() {
	}

	public static void accept(TalentSyncPayload payload) {
		earned = payload.earned();
		rage = payload.rage();
		swift = payload.swift();
		will = payload.will();
		quickWit = payload.quickWit();
		survival = payload.survival();
		patience = payload.patience();
		wisdom = payload.wisdom();
		nearSpawn = payload.nearSpawn();
	}

	/** 换世界 / 重连时清掉镜像，等新世界的同步包。 */
	public static void reset() {
		earned = 0;
		rage = 0;
		swift = 0;
		will = 0;
		quickWit = 0;
		survival = 0;
		patience = 0;
		wisdom = 0;
		nearSpawn = false;
	}

	public static int earned() {
		return earned;
	}

	public static int points(Talent talent) {
		return switch (talent) {
			case RAGE -> rage;
			case SWIFT -> swift;
			case WILL -> will;
			case QUICK_WIT -> quickWit;
			case SURVIVAL -> survival;
			case PATIENCE -> patience;
			case WISDOM -> wisdom;
		};
	}

	/** 能否修改天赋：服务端在进服 / 变更 / 打开界面时算好并在同步包里告知。 */
	public static boolean nearSpawn() {
		return nearSpawn;
	}

	/** 客户端按同一套门槛算阶段（只用于界面显示，不参与数值结算）。 */
	public static int stage(Talent talent) {
		return talent.stage(points(talent));
	}

	public static int spent() {
		return rage + swift + will + quickWit + survival + patience + wisdom;
	}

	public static int remaining() {
		return Math.max(0, earned - spent());
	}
}
