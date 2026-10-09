package com.sephiria.mixin.client;

import com.sephiria.client.DebuffLabelHolder;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** 给每个实体渲染状态挂一个实体 id（见 {@link DebuffLabelHolder}）。 */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements DebuffLabelHolder {
	@Unique
	private int sephiria$entityId = -1;

	@Override
	public int sephiria$entityId() {
		return this.sephiria$entityId;
	}

	@Override
	public void sephiria$setEntityId(int entityId) {
		this.sephiria$entityId = entityId;
	}
}
