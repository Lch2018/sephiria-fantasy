package com.sephiria.client;

/**
 * 渲染状态上的「这是哪个实体」：脚下层数标签要按实体 id 查客户端的减益层数，
 * 而 {@code EntityRenderState} 本身不带实体 id（26.3 的渲染拆成了
 * 「提取状态 → 提交绘制」两步，提交那一步已经拿不到实体）。
 *
 * <p>由 {@code mixin/client/EntityRenderStateMixin} 实现、{@code EntityRendererMixin}
 * 在提取阶段写入（那时还拿得到实体），提交阶段再读出来。
 */
public interface DebuffLabelHolder {
	int sephiria$entityId();

	void sephiria$setEntityId(int entityId);
}
