package com.sephiria.ability;

import com.sephiria.Sephiria;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 冲刺：固有技能，默认绑在鼠标侧键上（键位可以在设置菜单里改）。
 *
 * <p>按下后消耗一次技能存储，立刻获得 {@value #INVULNERABLE_TICKS} tick（0.1 秒）无敌，
 * 并沿准星朝向的<b>水平投影</b>方向突进 {@value #DISTANCE} 格、耗时
 * {@value #DURATION_TICKS} tick（0.3 秒）。方向只有准星方向，不能后退或横移。
 *
 * <p>存储规则：总量 {@value #MAX_CHARGES} 次，每次消耗 {@value #COST}，
 * 每满 {@value #REGEN_INTERVAL_TICKS} tick（1 秒）回复 {@value #REGEN_AMOUNT}——见 {@link SkillStorage}。
 *
 * <p>注意和其它系统区分：本模组里「闪避」指的是另一套还没做的属性（闪避率），
 * 这里的位移技能叫「冲刺」，底层统一走 {@link Dash}。
 */
public final class DashSkill {
	/** 技能存储 id。 */
	public static final Identifier STORAGE = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "dash");

	/** 存储总量：3 次。 */
	public static final double MAX_CHARGES = 3.0D;
	/** 每次使用的消耗量。 */
	public static final double COST = 1.0D;
	/** 回复量：每满一个间隔回复 1。 */
	public static final double REGEN_AMOUNT = 1.0D;
	/** 回复间隔（tick）：1 秒。 */
	public static final int REGEN_INTERVAL_TICKS = 20;

	/** 无敌时长（tick）：0.1 秒。 */
	public static final int INVULNERABLE_TICKS = 2;
	/** 位移距离（格）。 */
	public static final double DISTANCE = 3.0D;
	/** 位移耗时（tick）：0.3 秒。 */
	public static final int DURATION_TICKS = 6;

	private DashSkill() {
	}

	/** 注册存储配置（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		SkillStorage.register(STORAGE, MAX_CHARGES, REGEN_AMOUNT, REGEN_INTERVAL_TICKS);
	}

	public static void perform(ServerPlayer player) {
		// 存储不足就什么都不做（未来可以在这里补一个"次数不足"的提示音）
		if (!SkillStorage.consume(player, STORAGE, COST)) {
			return;
		}

		Invulnerability.grant(player, INVULNERABLE_TICKS);

		Vec3 look = player.getLookAngle();
		Vec3 direction = new Vec3(look.x, 0.0D, look.z);

		if (direction.lengthSqr() < 1.0E-6D) {
			// 垂直看天/看地时水平投影长度是 0，退化成朝向的水平方向
			float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
			direction = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
		}

		Dash.start(player, direction, DISTANCE, DURATION_TICKS, Dash.DEFAULT_DECAY);
	}
}
