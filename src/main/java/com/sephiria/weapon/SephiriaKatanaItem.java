package com.sephiria.weapon;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.InteractionResultHolder;
import com.sephiria.Sephiria;
import com.sephiria.ability.Invulnerability;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * 1.21.1 移植版的刀：保留出鞘 / 入鞘属性与右键切换，暂时不接 GeckoLib 动画和剑意系统。
 *
 * <p>26.3 依赖 GeckoLib 5 的渲染/动画 API；1.21.1 的 GeckoLib 4 接口完全不同，
 * 先把核心战斗数值和状态切换迁过来，动画后续再接。
 */
public class SephiriaKatanaItem extends Item implements GeoItem, SephiriaWeapon {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public static final int SWITCH_TICKS = 13;
	public static final int BLOCK_TICKS = 6;

	private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0F;
	private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;
	private static final float VANILLA_SWORD_ATTACK_SPEED = 1.6F;
	private static final float UNSHEATHED_DAMAGE = 2.52F;
	private static final float UNSHEATHED_CHARGE_SCALE = 2.6F;
	private static final float SHEATHED_DAMAGE = 12.0F;
	private static final float SHEATHED_CHARGE_SCALE = 0.7F;

	public static final DataComponentType<Boolean> SHEATHED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "katana_sheathed"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	private static final ResourceLocation DAMAGE_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "katana_attack_damage");
	private static final ResourceLocation SPEED_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "katana_attack_speed");

	private final WeaponBranch branch;

	public SephiriaKatanaItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 0,
				state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle_unsheathed"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	public static boolean isSheathed(ItemStack stack) {
		return Boolean.TRUE.equals(stack.get(SHEATHED));
	}

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

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			boolean sheathed = !isSheathed(stack);
			stack.set(SHEATHED, sheathed);
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers(sheathed));
			serverPlayer.getCooldowns().addCooldown(stack.getItem(), SWITCH_TICKS);
			Invulnerability.grant(serverPlayer, BLOCK_TICKS);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, sheathed ? 0.8F : 1.2F);
		}

		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	/** 1.21.1 暂时不接刀的横扫 / 剑意事件；保留入口，后续移植时在这里补。 */
	public static void registerEvents() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> InteractionResult.PASS);
	}

	public static void register() {
	}
}
