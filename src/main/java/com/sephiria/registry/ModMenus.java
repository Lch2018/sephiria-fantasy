package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.ChestMenu;
import com.sephiria.backpack.EnchantMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.MenuType;

/**
 * 菜单注册表。
 *
 * <p>26.2 起 {@code MenuType} 的构造函数与 {@code MenuScreens.register} 都成了私有/包私有，
 * 所以菜单类型要用 Fabric 的 {@link ExtendedMenuType}（它的构造函数是公开的），
 * 客户端挂屏幕则走访问拓宽（见 {@code sephiria.accesswidener}）。
 *
 * <p>这里不需要额外的开屏数据，所以泛型参数用 {@code Unit} 占位。
 */
public final class ModMenus {
	public static final MenuType<ArtifactBackpackMenu> ARTIFACT_BACKPACK = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_backpack"),
			new ExtendedMenuType<ArtifactBackpackMenu, Unit>(
					(id, inventory, data) -> new ArtifactBackpackMenu(id, inventory),
					Unit.STREAM_CODEC));

	/** 神器附魔面板（神器附魔币右键打开）。 */
	public static final MenuType<EnchantMenu> ARTIFACT_ENCHANT = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_enchant"),
			new ExtendedMenuType<EnchantMenu, Unit>(
					(id, inventory, data) -> new EnchantMenu(id, inventory),
					Unit.STREAM_CODEC));

	/** 宝箱页面（神器 / 石板 / 升级宝箱右键打开）。 */
	public static final MenuType<ChestMenu> CHEST = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "chest"),
			new ExtendedMenuType<ChestMenu, Unit>(
					(id, inventory, data) -> new ChestMenu(id, inventory),
					Unit.STREAM_CODEC));

	private ModMenus() {
	}

	public static void initialize() {
	}
}
