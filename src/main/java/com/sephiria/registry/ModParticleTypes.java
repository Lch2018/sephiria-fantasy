package com.sephiria.registry;

import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * SEPHIRIA 的自定义粒子类型。
 *
 * <p>原版 ELECTRIC_SPARK 的颜色写死在客户端渲染器里（白粉色），服务端没法单独换色；
 * 想要「电火花的形状 + 指定颜色」就必须有自己的粒子类型，颜色由客户端粒子类
 * （{@code com.sephiria.client.particle.BlueSparkParticle}）写死成 #7EFAFF。
 * 形状与原版电火花完全一致：贴的就是 glow 贴图（见 particles/blue_spark.json）。
 *
 * <p>{@link #SUN_SLASH} 反过来要带数据：太阳剑的剑痕长度跟着连击档位与神器走
 * （火红的夕阳的「增大」把划痕拉到 1.6 倍），所以它是个「带参数的粒子类型」，
 * 长度随粒子包一起发给客户端（{@link SunSlashOptions}），渲染在
 * {@code com.sephiria.client.particle.SunSlashParticle}。
 *
 * <p>1.20.1 的粒子参数没有 StreamCodec，走 {@code ParticleOptions#writeToNetwork} /
 * {@code ParticleOptions.Deserializer#fromNetwork} 这一对老接口；命名字段只用于命令，
 * 这里给默认值即可（模组自己从不通过命令生成这些粒子）。
 */
public final class ModParticleTypes {
	/** 电蓝色电火花：触电（挂层/每刻光环/结算）与乌云雷击（闪电线/命中爆点）共用。 */
	public static final SimpleParticleType BLUE_SPARK = register("blue_spark");

	/**
	 * 暴风雪云的雪：贴图直接从原版天气雪的 {@code environment/snow.png} 裁出来，
	 * 外观与原版降雪一样（不是原版 {@code SNOWFLAKE} 那颗雪花图标）。
	 */
	public static final SimpleParticleType SNOWFALL = register("snowfall");

	/**
	 * 等离子体的菱形粒子：贴图是一颗白色菱形，颜色由客户端按 20% 粉 / 60% 白 / 20% 蓝随机染。
	 *
	 * <p>用户 2026-10-25 定：等离子体的特效粒子形状是菱形，颜色比例固定。
	 */
	public static final SimpleParticleType PLASMA = register("plasma");

	/** 太阳剑的剑痕：一道笔直的发光划痕（贴图 textures/particle/sun_slash_0.png）。 */
	public static final ParticleType<SunSlashOptions> SUN_SLASH = registerSunSlash();

	/** 冰霜武具的剑气：一道冰蓝色的<b>弧</b>（贴图 textures/particle/frost_qi_0.png）。 */
	public static final ParticleType<FrostQiOptions> FROST_QI = registerFrostQi();

	/**
	 * 永恒蚀在左半边时的剑气：同一道弧，颜色换成焰黄（贴图 textures/particle/flame_qi_0.png）。
	 *
	 * <p>颜色画在贴图里，所以「换色」= 换一个粒子类型（渲染那一套完全共用，见
	 * {@code client.particle.FrostQiParticle}）；参数形状与冰蓝那份一样（{@link QiOptions}）。
	 */
	public static final ParticleType<FlameQiOptions> FLAME_QI = registerFlameQi();

	/**
	 * 永恒蚀在右半边时的剑痕：太阳剑那道直痕的冰蓝版（贴图 textures/particle/ice_slash_0.png）。
	 *
	 * <p>同上：形状与太阳剑剑痕一模一样，只有贴图的配色不同。
	 */
	public static final ParticleType<IceSlashOptions> ICE_SLASH = registerIceSlash();

	private ModParticleTypes() {
	}

	private static SimpleParticleType register(String name) {
		ResourceLocation id = new ResourceLocation(Sephiria.MOD_ID, name);
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.simple());
	}

	private static ParticleType<SunSlashOptions> registerSunSlash() {
		ResourceLocation id = new ResourceLocation(Sephiria.MOD_ID, "sun_slash");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.complex(
				new ParticleOptions.Deserializer<SunSlashOptions>() {
					@Override
					public SunSlashOptions fromCommand(ParticleType<SunSlashOptions> type,
							com.mojang.brigadier.StringReader reader) {
						return new SunSlashOptions(1.0F, 0.0F);
					}

					@Override
					public SunSlashOptions fromNetwork(ParticleType<SunSlashOptions> type, FriendlyByteBuf buf) {
						return new SunSlashOptions(buf.readFloat(), buf.readFloat());
					}
				}));
	}

	private static ParticleType<FrostQiOptions> registerFrostQi() {
		ResourceLocation id = new ResourceLocation(Sephiria.MOD_ID, "frost_qi");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.complex(
				new ParticleOptions.Deserializer<FrostQiOptions>() {
					@Override
					public FrostQiOptions fromCommand(ParticleType<FrostQiOptions> type,
							com.mojang.brigadier.StringReader reader) {
						return new FrostQiOptions(1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
					}

					@Override
					public FrostQiOptions fromNetwork(ParticleType<FrostQiOptions> type, FriendlyByteBuf buf) {
						return new FrostQiOptions(buf.readFloat(), buf.readFloat(), buf.readFloat(),
								buf.readFloat(), buf.readFloat());
					}
				}));
	}

	private static ParticleType<FlameQiOptions> registerFlameQi() {
		ResourceLocation id = new ResourceLocation(Sephiria.MOD_ID, "flame_qi");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.complex(
				new ParticleOptions.Deserializer<FlameQiOptions>() {
					@Override
					public FlameQiOptions fromCommand(ParticleType<FlameQiOptions> type,
							com.mojang.brigadier.StringReader reader) {
						return new FlameQiOptions(1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
					}

					@Override
					public FlameQiOptions fromNetwork(ParticleType<FlameQiOptions> type, FriendlyByteBuf buf) {
						return new FlameQiOptions(buf.readFloat(), buf.readFloat(), buf.readFloat(),
								buf.readFloat(), buf.readFloat());
					}
				}));
	}

	private static ParticleType<IceSlashOptions> registerIceSlash() {
		ResourceLocation id = new ResourceLocation(Sephiria.MOD_ID, "ice_slash");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.complex(
				new ParticleOptions.Deserializer<IceSlashOptions>() {
					@Override
					public IceSlashOptions fromCommand(ParticleType<IceSlashOptions> type,
							com.mojang.brigadier.StringReader reader) {
						return new IceSlashOptions(1.0F, 0.0F);
					}

					@Override
					public IceSlashOptions fromNetwork(ParticleType<IceSlashOptions> type, FriendlyByteBuf buf) {
						return new IceSlashOptions(buf.readFloat(), buf.readFloat());
					}
				}));
	}

	/** 由 {@code Sephiria#onInitialize()} 调用：引用一次本类即触发静态字段的注册。 */
	public static void initialize() {
	}

	/**
	 * 剑气弧的粒子参数形状（边长、面内倾角、飞行方向）：冰蓝（{@link FrostQiOptions}）与焰黄
	 * （{@link FlameQiOptions}）两份字段完全一样，只有粒子类型不同——渲染端与工厂因此只写一份
	 * （{@code client.particle.FrostQiParticle}）。
	 */
	public interface QiOptions extends ParticleOptions {
		float size();

		float roll();

		float dirX();

		float dirY();

		float dirZ();
	}

	/**
	 * 剑痕的粒子参数形状（边长、面内倾角）：火红（{@link SunSlashOptions}）与冰蓝
	 * （{@link IceSlashOptions}）两份同形，共用 {@code client.particle.SunSlashParticle}。
	 */
	public interface SlashOptions extends ParticleOptions {
		float size();

		float roll();
	}

	/**
	 * 太阳剑剑痕的粒子参数：一个「尺寸」（格，正方形的边长，也就是那道剑痕的长度）
	 * 与一个「倾角」（弧度，在屏幕面内旋转，见 {@code SunSlashParticle}）。
	 */
	public record SunSlashOptions(float size, float roll) implements SlashOptions {
		@Override
		public ParticleType<?> getType() {
			return SUN_SLASH;
		}

		@Override
		public void writeToNetwork(FriendlyByteBuf buf) {
			buf.writeFloat(this.size);
			buf.writeFloat(this.roll);
		}

		@Override
		public String writeToString() {
			return "sephiria:sun_slash[" + this.size + "," + this.roll + "]";
		}
	}

	/** 剑痕的冰蓝版（永恒蚀在右半边时太阳剑用它）：字段与 {@link SunSlashOptions} 相同。 */
	public record IceSlashOptions(float size, float roll) implements SlashOptions {
		@Override
		public ParticleType<?> getType() {
			return ICE_SLASH;
		}

		@Override
		public void writeToNetwork(FriendlyByteBuf buf) {
			buf.writeFloat(this.size);
			buf.writeFloat(this.roll);
		}

		@Override
		public String writeToString() {
			return "sephiria:ice_slash[" + this.size + "," + this.roll + "]";
		}
	}

	/**
	 * 冰霜武具那道弧状剑气的粒子参数：边长（格）、面内倾角（弧度）与<b>飞行方向</b>。
	 */
	public record FrostQiOptions(float size, float roll, float dirX, float dirY, float dirZ)
			implements QiOptions {
		@Override
		public ParticleType<?> getType() {
			return FROST_QI;
		}

		@Override
		public void writeToNetwork(FriendlyByteBuf buf) {
			buf.writeFloat(this.size);
			buf.writeFloat(this.roll);
			buf.writeFloat(this.dirX);
			buf.writeFloat(this.dirY);
			buf.writeFloat(this.dirZ);
		}

		@Override
		public String writeToString() {
			return "sephiria:frost_qi[" + this.size + "," + this.roll + "]";
		}
	}

	/** 剑气的焰黄版（永恒蚀在左半边时冰霜武具用它）：字段与 {@link FrostQiOptions} 相同。 */
	public record FlameQiOptions(float size, float roll, float dirX, float dirY, float dirZ)
			implements QiOptions {
		@Override
		public ParticleType<?> getType() {
			return FLAME_QI;
		}

		@Override
		public void writeToNetwork(FriendlyByteBuf buf) {
			buf.writeFloat(this.size);
			buf.writeFloat(this.roll);
			buf.writeFloat(this.dirX);
			buf.writeFloat(this.dirY);
			buf.writeFloat(this.dirZ);
		}

		@Override
		public String writeToString() {
			return "sephiria:flame_qi[" + this.size + "," + this.roll + "]";
		}
	}
}
