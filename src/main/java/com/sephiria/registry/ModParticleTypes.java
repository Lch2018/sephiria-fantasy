package com.sephiria.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

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
 * <p>必须在 {@code Sephiria#onInitialize()} 里 {@link #initialize()} 一次——粒子类型
 * 要赶在原版注册表冻结之前注册；客户端侧的粒子渲染器由 mixin 在
 * {@code ParticleResources#registerProviders()} 末尾挂进来（Fabric 在 26.3 已经没有
 * 粒子工厂注册 API 可用了）。
 */
public final class ModParticleTypes {
	/** 电蓝色电火花：触电（挂层/每刻光环/结算）与乌云雷击（闪电线/命中爆点）共用。 */
	public static final SimpleParticleType BLUE_SPARK = register("blue_spark");

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
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name);
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.simple());
	}

	private static ParticleType<SunSlashOptions> registerSunSlash() {
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "sun_slash");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id,
				FabricParticleTypes.complex(SunSlashOptions.CODEC, SunSlashOptions.STREAM_CODEC));
	}

	private static ParticleType<FrostQiOptions> registerFrostQi() {
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frost_qi");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id,
				FabricParticleTypes.complex(FrostQiOptions.CODEC, FrostQiOptions.STREAM_CODEC));
	}

	private static ParticleType<FlameQiOptions> registerFlameQi() {
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "flame_qi");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id,
				FabricParticleTypes.complex(FlameQiOptions.CODEC, FlameQiOptions.STREAM_CODEC));
	}

	private static ParticleType<IceSlashOptions> registerIceSlash() {
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "ice_slash");
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id,
				FabricParticleTypes.complex(IceSlashOptions.CODEC, IceSlashOptions.STREAM_CODEC));
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
	 *
	 * <p>这两个值都必须跟着数据走、不能写死在客户端：长度是服务端算的
	 * （目前只有火红的夕阳的「增大」会把它拉到 1.6 倍）、倾角是服务端掷的三个方向之一
	 * （连击打出去的那一刀朝哪边偏），随粒子包一起发给客户端。
	 * 之所以用带参数的粒子类型、而不是原版横扫那种「把尺寸塞进速度字段」的写法：
	 * 那条路要靠 {@code count = 0} 分支里 {@code 散布 × 速度} 的巧合，读代码的人看不出来是在传尺寸。
	 */
	public record SunSlashOptions(float size, float roll) implements SlashOptions {
		public static final MapCodec<SunSlashOptions> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.FLOAT.fieldOf("size").forGetter(SunSlashOptions::size),
						Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(SunSlashOptions::roll)
				).apply(instance, SunSlashOptions::new));

		public static final StreamCodec<ByteBuf, SunSlashOptions> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.FLOAT, SunSlashOptions::size,
				ByteBufCodecs.FLOAT, SunSlashOptions::roll,
				SunSlashOptions::new);

		@Override
		public ParticleType<?> getType() {
			return SUN_SLASH;
		}
	}

	/** 剑痕的冰蓝版（永恒蚀在右半边时太阳剑用它）：字段与 {@link SunSlashOptions} 相同。 */
	public record IceSlashOptions(float size, float roll) implements SlashOptions {
		public static final MapCodec<IceSlashOptions> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.FLOAT.fieldOf("size").forGetter(IceSlashOptions::size),
						Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(IceSlashOptions::roll)
				).apply(instance, IceSlashOptions::new));

		public static final StreamCodec<ByteBuf, IceSlashOptions> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.FLOAT, IceSlashOptions::size,
				ByteBufCodecs.FLOAT, IceSlashOptions::roll,
				IceSlashOptions::new);

		@Override
		public ParticleType<?> getType() {
			return ICE_SLASH;
		}
	}

	/**
	 * 冰霜武具那道弧状剑气的粒子参数：边长（格）、面内倾角（弧度）与<b>飞行方向</b>。
	 *
	 * <p>方向是 2026-10-13 加的（用户要求「弧面朝飞行方向，而不是朝上」）：方片不再整张朝摄像机，
	 * 而是摆成一面<b>垂直于飞行方向</b>的刀墙（法线 = 飞行方向），贴图的「上」取世界竖直方向在面内的投影
	 * ——见 {@code client.particle.FrostQiParticle}。倾角仍是那张面内的旋转（多道之间错开）。
	 */
	public record FrostQiOptions(float size, float roll, float dirX, float dirY, float dirZ)
			implements QiOptions {
		public static final MapCodec<FrostQiOptions> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.FLOAT.fieldOf("size").forGetter(FrostQiOptions::size),
						Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(FrostQiOptions::roll),
						Codec.FLOAT.fieldOf("dir_x").forGetter(FrostQiOptions::dirX),
						Codec.FLOAT.fieldOf("dir_y").forGetter(FrostQiOptions::dirY),
						Codec.FLOAT.fieldOf("dir_z").forGetter(FrostQiOptions::dirZ)
				).apply(instance, FrostQiOptions::new));

		public static final StreamCodec<ByteBuf, FrostQiOptions> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.FLOAT, FrostQiOptions::size,
				ByteBufCodecs.FLOAT, FrostQiOptions::roll,
				ByteBufCodecs.FLOAT, FrostQiOptions::dirX,
				ByteBufCodecs.FLOAT, FrostQiOptions::dirY,
				ByteBufCodecs.FLOAT, FrostQiOptions::dirZ,
				FrostQiOptions::new);

		@Override
		public ParticleType<?> getType() {
			return FROST_QI;
		}
	}

	/** 剑气的焰黄版（永恒蚀在左半边时冰霜武具用它）：字段与 {@link FrostQiOptions} 相同。 */
	public record FlameQiOptions(float size, float roll, float dirX, float dirY, float dirZ)
			implements QiOptions {
		public static final MapCodec<FlameQiOptions> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.FLOAT.fieldOf("size").forGetter(FlameQiOptions::size),
						Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(FlameQiOptions::roll),
						Codec.FLOAT.fieldOf("dir_x").forGetter(FlameQiOptions::dirX),
						Codec.FLOAT.fieldOf("dir_y").forGetter(FlameQiOptions::dirY),
						Codec.FLOAT.fieldOf("dir_z").forGetter(FlameQiOptions::dirZ)
				).apply(instance, FlameQiOptions::new));

		public static final StreamCodec<ByteBuf, FlameQiOptions> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.FLOAT, FlameQiOptions::size,
				ByteBufCodecs.FLOAT, FlameQiOptions::roll,
				ByteBufCodecs.FLOAT, FlameQiOptions::dirX,
				ByteBufCodecs.FLOAT, FlameQiOptions::dirY,
				ByteBufCodecs.FLOAT, FlameQiOptions::dirZ,
				FlameQiOptions::new);

		@Override
		public ParticleType<?> getType() {
			return FLAME_QI;
		}
	}
}
