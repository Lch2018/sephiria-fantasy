package com.sephiria.util;

import com.sephiria.util.ParticleColors;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

/** 1.21.1 的 DustParticleOptions 收 Vector3f，26.3 版是打包 int；这里统一转换。 */
public final class ParticleColors {
	private ParticleColors() {
	}

	public static DustParticleOptions dust(int rgb, float scale) {
		return new DustParticleOptions(new Vector3f(
				((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F,
				(rgb & 0xFF) / 255.0F), scale);
	}
}
