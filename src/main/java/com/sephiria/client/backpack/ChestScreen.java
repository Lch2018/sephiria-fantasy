package com.sephiria.client.backpack;

import com.sephiria.backpack.ChestMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.ChestRerollPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 宝箱页面（移植 26.3 的样子）：上面 5 格随机奖励（点一格直接拿走），下面是玩家背包。
 *
 * <p>只画这一箱实际开出来的奖励格（天赋可能多给一格）；骰子格与刷新按钮单独留着。
 * 奖励格下方两行提示：一行说明「点一件直接拿走」，一行说明刷新次数 / 刷新代价。
 *
 * <p>没有标签栏——它是一次性的选择界面，选完就关（服务端那边也是这么处理的）。
 */
public class ChestScreen extends SephiriaMenuScreen<ChestMenu> {
	/** 「刷新」按钮的宽度（26.3 的样式；高度沿用 18）。 */
	private static final int REROLL_BUTTON_WIDTH = 28;
	private static final int REROLL_BUTTON_HEIGHT = 18;

	private Button rerollButton;

	public ChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ChestMenu.IMAGE_WIDTH, ChestMenu.imageHeight(), true);
		// 两行提示放在奖励格与玩家背包之间（41 / 51），「物品栏」标签再往下一点，别压住第二行
		this.inventoryLabelY = ChestMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		// 「刷新」：天赋「智慧」三阶段每个宝箱 1 次免费；没有免费次数时要有骰子才亮
		this.rerollButton = Button.builder(
				Component.translatable("screen.sephiria_fantasy.chest.reroll"),
				pressed -> ClientPlayNetworking.send(ChestRerollPayload.INSTANCE))
				.bounds(this.leftPos + ChestMenu.REROLL_BUTTON_X, this.topPos + ChestMenu.REROLL_BUTTON_Y,
						REROLL_BUTTON_WIDTH, REROLL_BUTTON_HEIGHT).build();
		this.addRenderableWidget(this.rerollButton);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.rerollButton != null) {
			this.rerollButton.active = this.menu.canReroll();
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);

		for (int index = 0; index < this.menu.slots.size(); index++) {
			// 这一箱没开到的奖励格不画（天赋没点亮时就是 5 格而不是 6 格）
			if (index < ChestMenu.REWARD_SLOTS && index >= this.menu.rewardCount()) {
				continue;
			}

			drawSlotBackground(graphics, this.menu.slots.get(index));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		graphics.drawString(this.font, Component.translatable("screen.sephiria_fantasy.chest.hint"),
				8, ChestMenu.SLOT_Y + 19, HINT, false);
		graphics.drawString(this.font, rerollHint(), 8, ChestMenu.SLOT_Y + 29, HINT, false);
	}

	/** 刷新提示：还有免费次数就显示剩余次数，否则说明要消耗骰子。 */
	private Component rerollHint() {
		int free = this.menu.freeRerolls();
		return free > 0
				? Component.translatable("screen.sephiria_fantasy.chest.free_rerolls", free)
				: Component.translatable("screen.sephiria_fantasy.chest.reroll_cost");
	}
}
