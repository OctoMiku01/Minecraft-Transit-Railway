package org.mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Draws into the world by submitting to the {@link SubmitNodeCollector} of the current frame.
 * The recorded vertices are already transformed, so they are submitted with an identity matrix stack.
 */
public final class WorldDrawTarget extends DrawTarget {

	private final SubmitNodeCollector submitNodeCollector;

	public WorldDrawTarget(SubmitNodeCollector submitNodeCollector) {
		this.submitNodeCollector = submitNodeCollector;
	}

	@Override
	public void drawText(PoseStack matrixStack, FormattedCharSequence text, float x, float y, int color, boolean shadow, int light) {
		submitNodeCollector.submitText(matrixStack, x, y, text, shadow, Font.DisplayMode.NORMAL, light, color, 0, 0);
	}

	@Override
	protected void drawQuads(@Nullable Identifier texture, RecordingVertexConsumer recordingVertexConsumer) {
		submitNodeCollector.submitCustomGeometry(new PoseStack(), texture == null ? RenderTypes.textBackground() : RenderTypes.text(texture), (pose, vertexConsumer) -> recordingVertexConsumer.replay(vertexConsumer));
	}
}
