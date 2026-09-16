package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.Invulnerability;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import org.joml.Quaternionf;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/**
 * 赛菲利亚的「刀」：带刀鞘的居合刀，模型与动画来自 Blockbench 的 GeckoLib 工程
 * （models/katana.bbmodel -> assets/sephiria/geckolib/{models,animations} + textures/item/katana.png）。
 *
 * <p>两种状态，右键切换，两个状态都会打出原版式的横扫（sweep）：
 * <ul>
 *   <li><b>出鞘</b>（默认）：伤害 {@value #UNSHEATHED_DAMAGE}，横扫充能速度是原版剑的
 *       {@value #UNSHEATHED_CHARGE_SCALE} 倍（轻快但伤害低）。</li>
 *   <li><b>入鞘</b>：伤害 {@value #SHEATHED_DAMAGE}，相当于加强版横扫之刃——横扫范围是原版
 *       {@value #SHEATHED_SWEEP_RANGE} 倍，但充能速度只有原版剑的 {@value #SHEATHED_CHARGE_SCALE} 倍。</li>
 * </ul>
 *
 * <p>原版横扫的判定（{@code Player#attack} / {@code Player#doSweepAttack}）：
 * 主目标受到正常伤害，另以<b>目标</b>为中心 {@code inflate(1.0, 0.25, 1.0)} 扫描生物，
 * 伤害为 {@code 1 + SWEEPING_DAMAGE_RATIO × 主伤害}，且离玩家不超过 3 格；
 * 触发条件是充能 {@code getAttackStrengthScale(0.5F) > 0.9F}、未疾跑、站在地面。
 * 充能速度就是 {@code ATTACK_SPEED} 属性（冷却 = 20 / 攻速），所以攻速直接决定"充能快慢"。
 *
 * <p>刀不在 {@code minecraft:swords} 标签里，原版不会替它判定横扫，横扫由
 * {@link #registerEvents()} 里的攻击回调自己实现，参数按状态取。
 *
 * <p>切换状态会播放 {@code switch_to_sheathed} / {@code switch_to_unsheathed}，
 * 切换开始后的 {@value #BLOCK_TICKS} tick（0.2 秒）内玩家处于格挡状态，免疫一切伤害。
 */
public class SephiriaKatanaItem extends Item implements GeoItem, SephiriaWeapon {
	/** 切换动画的时长（tick）：0.65 秒，期间不能再次切换。 */
	public static final int SWITCH_TICKS = 13;
	/** 切换开始后的格挡窗口（tick）：0.2 秒免疫一切伤害。 */
	public static final int BLOCK_TICKS = 4;

	/** 玩家空手的基础攻击力 / 攻速（原版 {@code Player#createAttributes}）。 */
	private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0F;
	private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;
	/** 原版剑的攻速：横扫充能速度的基准。 */
	private static final float VANILLA_SWORD_ATTACK_SPEED = 1.6F;

	/** 出鞘：伤害低、充能快（原版剑的 2 倍）。 */
	private static final float UNSHEATHED_DAMAGE = 5.0F;
	private static final float UNSHEATHED_CHARGE_SCALE = 2.0F;
	/** 出鞘的横扫范围/伤害比：等同原版一般附魔水平。 */
	private static final double UNSHEATHED_SWEEP_RANGE = 1.0D;
	private static final double UNSHEATHED_SWEEP_RATIO = 0.5D;

	/** 入鞘：伤害高、范围 2 倍，但充能只有原版剑的 0.7 倍。 */
	private static final float SHEATHED_DAMAGE = 12.0F;
	private static final float SHEATHED_CHARGE_SCALE = 0.7F;
	private static final double SHEATHED_SWEEP_RANGE = 2.0D;
	/** 1.0 相当于原版横扫之刃满级。 */
	private static final double SHEATHED_SWEEP_RATIO = 1.0D;

	/** 原版横扫的扫描盒：以命中目标为中心 {@code inflate(1.0, 0.25, 1.0)}，离玩家不超过 3 格。 */
	private static final double VANILLA_SWEEP_RANGE = 1.0D;
	private static final double VANILLA_SWEEP_HEIGHT = 0.25D;
	private static final double VANILLA_SWEEP_MAX_DISTANCE = 3.0D;
	/** 原版横扫要求的充能下限（{@code getAttackStrengthScale(0.5F) > 0.9F}）。 */
	private static final float SWEEP_CHARGE_THRESHOLD = 0.9F;

	private static final Identifier DAMAGE_MODIFIER_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "katana_attack_damage");
	private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "katana_attack_speed");

	/** 入鞘状态。物品注册时作为默认值写入，注册发生在组件绑定之前，所以直接构造。 */
	public static final DataComponentType<Boolean> SHEATHED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "katana_sheathed"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final WeaponBranch branch;

	public SephiriaKatanaItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
		// 服务端触发的动画需要注册同步（右键切换是在服务端做的）。
		GeoItem.registerSyncedAnimatable(this);
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	// ------------------------------------------------------------------ 状态

	public static boolean isSheathed(ItemStack stack) {
		return Boolean.TRUE.equals(stack.get(SHEATHED));
	}

	/**
	 * 某个状态下的攻击力/攻速。
	 *
	 * <p>物品属性是放在 {@code ATTRIBUTE_MODIFIERS} 组件里的，而组件属于 ItemStack，
	 * 所以切换状态时直接换掉这个组件即可；修饰符的值要减去玩家自身的基础值
	 * （攻击力 1、攻速 4），这样工具栏里显示的数字就是 {@value #SHEATHED_DAMAGE} /
	 * {@value #UNSHEATHED_DAMAGE} 和换算出来的攻速。
	 */
	public static ItemAttributeModifiers attributeModifiers(boolean sheathed) {
		float damage = sheathed ? SHEATHED_DAMAGE : UNSHEATHED_DAMAGE;
		float speed = VANILLA_SWORD_ATTACK_SPEED * (sheathed ? SHEATHED_CHARGE_SCALE : UNSHEATHED_CHARGE_SCALE);

		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(DAMAGE_MODIFIER_ID, damage - PLAYER_BASE_ATTACK_DAMAGE,
								AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(SPEED_MODIFIER_ID, speed - PLAYER_BASE_ATTACK_SPEED,
								AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build();
	}

	/** 服务端：切换状态、换掉属性并播放对应动画。 */
	private static void toggle(ServerPlayer player, ItemStack stack) {
		boolean sheathed = !isSheathed(stack);
		stack.set(SHEATHED, sheathed);
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers(sheathed));
		player.getCooldowns().addCooldown(stack, SWITCH_TICKS);
		Invulnerability.grant(player, BLOCK_TICKS);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, sheathed ? 0.8F : 1.2F);
		if (stack.getItem() instanceof SephiriaKatanaItem katana) {
			katana.triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) player.level()), "main",
					sheathed ? "to_sheathed" : "to_unsheathed");
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			toggle((ServerPlayer) player, stack);
		}
		return InteractionResult.CONSUME;
	}

	// ------------------------------------------------------------------ 格挡与横扫

	/** 注册伤害拦截与攻击回调（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void registerEvents() {
		// 切换状态的 0.2 秒无敌由 Invulnerability 统一处理，见 Sephiria#onInitialize。
		// 两个状态都打横扫，参数按状态取；主目标仍然吃正常伤害，所以返回 PASS。
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!(stack.getItem() instanceof SephiriaKatanaItem)) {
				return InteractionResult.PASS;
			}
			if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}
			if (isSheathed(stack)) {
				// 入鞘状态砍中目标时播拔刀攻击动画（服务端触发，GeckoLib 会同步给客户端）。
				((SephiriaKatanaItem) stack.getItem()).triggerAnim(serverPlayer,
						GeoItem.getOrAssignId(stack, serverLevel), "main", "attack_sheathed");
			}
			if (!canSweep(serverPlayer)) {
				return InteractionResult.PASS;
			}
			sweep(serverPlayer, serverLevel, stack, entity);
			return InteractionResult.PASS;
		});
	}

	/**
	 * 与原版横扫一致的前置条件：充能 ≥ {@value #SWEEP_CHARGE_THRESHOLD}、未疾跑、站在地面。
	 *
	 * <p>原版还有一条"水平移动不能超过 2.5 倍移速"的判定（{@code isSweepAttack}），这里没抄——
	 * 那条只是防止边跑边扫，手感上更像"卡顿"，对刀来说没必要。
	 */
	private static boolean canSweep(ServerPlayer player) {
		return player.getAttackStrengthScale(0.5F) > SWEEP_CHARGE_THRESHOLD
				&& !player.isSprinting()
				&& player.onGround();
	}

	/** 原版 {@code Player#doSweepAttack} 的复刻：以命中目标为中心扫一圈，参数按状态缩放。 */
	private static void sweep(ServerPlayer player, ServerLevel level, ItemStack stack, Entity target) {
		boolean sheathed = isSheathed(stack);
		float mainDamage = sheathed ? SHEATHED_DAMAGE : UNSHEATHED_DAMAGE;
		double ratio = sheathed ? SHEATHED_SWEEP_RATIO : UNSHEATHED_SWEEP_RATIO;
		double rangeScale = sheathed ? SHEATHED_SWEEP_RANGE : UNSHEATHED_SWEEP_RANGE;

		// 原版公式：1 + 横扫伤害比 × 主伤害
		float sweepDamage = 1.0F + (float) (ratio * mainDamage);
		double reach = VANILLA_SWEEP_MAX_DISTANCE * rangeScale;
		AABB area = target.getBoundingBox().inflate(
				VANILLA_SWEEP_RANGE * rangeScale,
				VANILLA_SWEEP_HEIGHT * rangeScale,
				VANILLA_SWEEP_RANGE * rangeScale);
		DamageSource source = level.damageSources().playerAttack(player);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim == target || victim.isAlliedTo(player)) {
				continue;
			}
			if (victim instanceof ArmorStand stand && stand.isMarker()) {
				continue;
			}
			if (player.distanceToSqr(victim) >= reach * reach) {
				continue;
			}
			if (victim.hurtServer(level, source, sweepDamage)) {
				victim.knockback(0.4D, player.getX() - victim.getX(), player.getZ() - victim.getZ(), source, sweepDamage);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);

		// 原版 doSweepAttack 末尾的横扫特效：玩家正前方一格、半身高处放一个 SWEEP_ATTACK 粒子，
		// 速度方向就是朝向（count=0 + 非零 spread 表示"单个粒子带这个初速度"）。
		double dx = -Mth.sin(player.getYRot() * ((float) Math.PI / 180.0F));
		double dz = Mth.cos(player.getYRot() * ((float) Math.PI / 180.0F));
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				player.getX() + dx, player.getY(0.5D), player.getZ() + dz,
				0, dx, 0.0D, dz, 0.0D);
	}

	private static boolean isGuarding(Player player) {
		return Invulnerability.isActive(player);
	}

	// ------------------------------------------------------------------ GeckoLib

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// 默认播 idle_unsheathed：模型里"收鞘"是靠动画把刀鞘缩到 0 实现的，
		// 所以出鞘状态也必须有一条待机动画在跑，否则刀鞘会留在原地。
		//
		// 这里不要调 receiveTriggeredAnimations()：它的语义是"触发动画期间也让状态处理器运行"
		// （AnimationController#checkControllerState 里 handlesTriggeredAnimations 为 true 时
		// 不跳过状态处理器），那样状态处理器会每帧用 idle 覆盖掉切换动画。保持默认 false，
		// 触发动画播放期间状态处理器才会被跳过。
		controllers.add(new AnimationController<SephiriaKatanaItem>("main", 0,
				state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle_unsheathed")))
				.triggerableAnim("to_sheathed",
						RawAnimation.begin().thenPlay("animation.switch_to_sheathed").thenLoop("animation.idle_sheathed"))
				.triggerableAnim("to_unsheathed",
						RawAnimation.begin().thenPlay("animation.switch_to_unsheathed").thenLoop("animation.idle_unsheathed"))
				// 入鞘状态下的攻击动画：拔刀一击，播完回到入鞘待机。
				.triggerableAnim("attack_sheathed",
						RawAnimation.begin().thenPlay("animation.idle_sheathed2").thenLoop("animation.idle_sheathed")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	/**
	 * 客户端渲染器。
	 *
	 * <p>模型与动画的 Identifier 是 {@code GeckoLibResources} 的缓存键——资源扫描器把
	 * {@code assets/<ns>/geckolib/models/*.geo.json} 与 {@code .../geckolib/animations/*.animation.json}
	 * 按 {@code <ns>:<文件名>} 建索引，所以这里传裸名 {@code sephiria:katana}。
	 *
	 * <p>贴图不一样：它不走缓存，而是直接交给原版的 {@code RenderTypes.entityCutout}，
	 * 必须是<b>带 textures/ 前缀和 .png 后缀的完整文件路径</b>（GeckoLib 自己的
	 * {@code DefaultedGeoModel} 也是这么拼的：{@code textures/<子类>/<名字>.png}）。
	 * 少写前缀就会指向不存在的 {@code assets/sephiria/item/katana.png}，模型整个渲染成黑色。
	 */
	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private GeoItemRenderer<SephiriaKatanaItem> renderer;

			@Override
			public GeoItemRenderer<?> getGeoItemRenderer() {
				if (this.renderer == null) {
					this.renderer = new GeoItemRenderer<>(new GeoModel<SephiriaKatanaItem>() {
						@Override
						public Identifier getModelResource(GeoRenderState state) {
							return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "katana");
						}

						@Override
						public Identifier getTextureResource(GeoRenderState state) {
							return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/item/katana.png");
						}

						@Override
						public Identifier getAnimationResource(SephiriaKatanaItem animatable) {
							return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "katana");
						}
					}) {
						@Override
						public void adjustRenderPose(RenderPassInfo<GeoRenderState> passInfo) {
							super.adjustRenderPose(passInfo);
							// 出鞘时刀刃朝前：模型长轴是自身的 Y，绕它转 90° 把刀面转到侧面。
							passInfo.poseStack().mulPose(new Quaternionf().rotateY((float) Math.toRadians(90.0D)));
						}
					};
				}
				return this.renderer;
			}
		});
	}

	/** 供外部查询：某玩家此刻是否处于切换状态的格挡窗口。 */
	public static boolean guarding(Player player) {
		return isGuarding(player);
	}
}
