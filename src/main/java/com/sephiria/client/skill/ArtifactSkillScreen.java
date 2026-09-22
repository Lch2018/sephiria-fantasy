package com.sephiria.client.skill;

import com.sephiria.artifact.skill.SkillSlots;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.ArtifactSkillsPayload;
import com.sephiria.network.AssignArtifactSkillPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器技能页：左边是「可用技能」格子（画的就是对应神器的图标），右边是 6 个技能栏格子。
 *
 * <p>操作用的是「拿起来 / 放下」那套：<b>点技能图标把它拿在手上</b>（图标跟着鼠标走），
 * <b>点技能栏就放进去、立刻生效</b>；栏位里已有技能时放上去就是替换；<b>右键</b>点栏位清空它；
 * 拿着技能点到空白处等于放回。技能格已经放进某个栏位时底色会变绿，方便看出还剩哪些没用上。
 *
 * <p>技能列表由服务端算好推过来（客户端手里没有背包内容），每次改动服务端会重推一次，页面据此重建。
 */
public class ArtifactSkillScreen extends Screen {
	/** 格子边长（与顶部标签栏一致）。 */
	private static final int CELL = 20;
	/** 行距。 */
	private static final int ROW = 24;
	private static final int LIST_X = 70;
	private static final int SLOT_X = 230;
	private static final int TOP = 54;

	/** 左列可用的技能（技能栏里存的那种字符串）。 */
	private final List<String> entries = new ArrayList<>();
	/** 每格左上角坐标，用于命中判定与绘制。 */
	private final List<int[]> listRects = new ArrayList<>();
	private final List<int[]> slotRects = new ArrayList<>();

	/** 拿在手上的技能；null = 手上没东西。 */
	private String held;

	public ArtifactSkillScreen() {
		super(Component.translatable("screen.sephiria.artifact_skills"));
	}

	@Override
	protected void init() {
		SephiriaTabs.add(this, this.width / 2 - 100, 14, SephiriaTab.ARTIFACT_SKILLS);
		this.entries.clear();
		this.listRects.clear();
		this.slotRects.clear();

		List<String> all = new ArrayList<>();

		for (ArtifactSkillsPayload.Entry entry : ClientSkills.skills()) {
			all.add(ClientSkills.encode(entry));
		}

		// 已经放进技能栏的技能不再出现在左边（同名多份时按份数扣，扣掉一份只等于少一格）
		for (String slotEntry : ClientSkills.slots()) {
			if (!slotEntry.isEmpty()) {
				all.remove(slotEntry);
			}
		}

		this.entries.addAll(all);

		addRenderableWidget(new StringWidget(LIST_X, TOP - 16, 150, 12,
				Component.translatable("screen.sephiria.artifact_skills.available"), this.font));
		addRenderableWidget(new StringWidget(SLOT_X, TOP - 16, 150, 12,
				Component.translatable("screen.sephiria.artifact_skills.slots"), this.font));

		if (this.entries.isEmpty()) {
			addRenderableWidget(new StringWidget(LIST_X, TOP, 150, 12,
					Component.translatable("screen.sephiria.artifact_skills.none"), this.font));
		}

		// 现有技能格：一行 3 个，技能多了往下长（3x3 → 3x4 → …）
		for (int index = 0; index < this.entries.size(); index++) {
			this.listRects.add(new int[]{ LIST_X + (index % 3) * ROW, TOP + (index / 3) * ROW });
		}

		for (int slot = 0; slot < SkillSlots.COUNT; slot++) {
			this.slotRects.add(new int[]{ SLOT_X, TOP + slot * ROW });
		}

		addRenderableWidget(new StringWidget(LIST_X, TOP + SkillSlots.COUNT * ROW + 18, this.width - 2 * LIST_X, 24,
				Component.translatable("screen.sephiria.artifact_skills.hint"), this.font));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);

		for (int index = 0; index < this.listRects.size(); index++) {
			String entry = this.entries.get(index);
			// 已经放进某个栏位的技能格底色变绿
			drawCell(extractor, this.listRects.get(index), ClientSkills.slots().contains(entry) ? 0xFF2F4A34 : 0xFF202030,
					inside(this.listRects.get(index), mouseX, mouseY));
			drawIcon(extractor, ClientSkills.itemOf(entry), this.listRects.get(index));

			if (inside(this.listRects.get(index), mouseX, mouseY)) {
				extractor.text(this.font, ClientSkills.slotLabel(entry), mouseX + 10, mouseY + 8, 0xFFFFFFFF);
			}
		}

		for (int slot = 0; slot < this.slotRects.size(); slot++) {
			String entry = ClientSkills.slots().size() > slot ? ClientSkills.slots().get(slot) : "";
			drawCell(extractor, this.slotRects.get(slot), entry.isEmpty() ? 0xFF202030 : 0xFF2A3A4A,
					inside(this.slotRects.get(slot), mouseX, mouseY));
			drawIcon(extractor, ClientSkills.itemOf(entry), this.slotRects.get(slot));
			extractor.text(this.font, Component.translatable("screen.sephiria.artifact_skills.slot", slot + 1),
					this.slotRects.get(slot)[0] + CELL + 6, this.slotRects.get(slot)[1] + 6, 0xFFAAAAAA);

			if (inside(this.slotRects.get(slot), mouseX, mouseY) && !entry.isEmpty()) {
				extractor.text(this.font, ClientSkills.slotLabel(entry), mouseX + 10, mouseY + 8, 0xFFFFFFFF);
			}
		}

		// 手上那个跟着鼠标
		if (this.held != null) {
			drawIcon(extractor, ClientSkills.itemOf(this.held), mouseX - 7, mouseY - 7);
		}
	}

	private void drawCell(GuiGraphicsExtractor extractor, int[] rect, int colour, boolean hovered) {
		extractor.fill(rect[0] - 1, rect[1] - 1, rect[0] + CELL - 3, rect[1] + CELL - 3,
				hovered ? 0xFFE8E8E8 : 0xFF707070);
		extractor.fill(rect[0], rect[1], rect[0] + CELL - 5, rect[1] + CELL - 5, colour);
	}

	/** 图标画在格子内沿（用原版物品绘制，走模型与 atlas；自己拼贴图路径会画成紫黑格甚至崩游戏）。 */
	private void drawIcon(GuiGraphicsExtractor extractor, Item item, int[] rect) {
		drawIcon(extractor, item, rect[0], rect[1]);
	}

	/**
	 * 画图标：技能图标都来自本模组物品，贴图就在 {@code textures/item/<id>.png}，直接 blit 是安全的。
	 *
	 * <p>曾经换成原版 {@code extractor.item(...)} 想统一走物品模型，结果在这个「收集渲染状态」的阶段
	 * 调用会崩游戏，所以这里保持直接画贴图（只画本模组物品，路径一定对）。
	 */
	private void drawIcon(GuiGraphicsExtractor extractor, Item item, int x, int y) {
		if (item != null) {
			extractor.blit(RenderPipelines.GUI_TEXTURED, ClientSkills.iconOf(item), x, y, 0.0F, 0.0F, CELL - 5, CELL - 5, 16, 16);
		}
	}

	private static boolean inside(int[] rect, double mouseX, double mouseY) {
		return mouseX >= rect[0] - 1 && mouseX < rect[0] + CELL - 3 && mouseY >= rect[1] - 1 && mouseY < rect[1] + CELL - 3;
	}

	/** 左键：拿起 / 放下；右键：清空那一格。 */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean rightClick = event.button() == 1;

		for (int index = 0; index < this.listRects.size(); index++) {
			if (inside(this.listRects.get(index), event.x(), event.y())) {
				this.held = this.entries.get(index);
				return true;
			}
		}

		for (int slot = 0; slot < this.slotRects.size(); slot++) {
			if (!inside(this.slotRects.get(slot), event.x(), event.y())) {
				continue;
			}

			String current = ClientSkills.slots().size() > slot ? ClientSkills.slots().get(slot) : "";

			if (rightClick) {
				ClientPlayNetworking.send(new AssignArtifactSkillPayload(slot, ""));
				this.held = null;
			} else if (this.held != null) {
				ClientPlayNetworking.send(new AssignArtifactSkillPayload(slot, this.held));
				this.held = null;
			} else if (!current.isEmpty()) {
				// 从栏位里拿起来：先清空这一格（技能立刻回到左边的现有技能格），再拿在手上
				ClientPlayNetworking.send(new AssignArtifactSkillPayload(slot, ""));
				this.held = current;
			}

			return true;
		}

		this.held = null;
		return super.mouseClicked(event, doubleClick);
	}

	/** 服务端回包后重建（栏位内容变了）。 */
	public void refresh() {
		this.clearWidgets();
		this.rebuildWidgets();
	}

	/** 按「背包键」（默认 E）关掉这一页回到游戏，与另外几页保持一致。 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (Minecraft.getInstance().options.keyInventory.matches(event)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(event);
	}
}
