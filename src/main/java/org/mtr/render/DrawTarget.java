package org.mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/**
 * Destination for drawing code that is shared between the world and GUI screens, such as railway signs.
 * <p>
 * Minecraft 26.2 has no immediate buffer source any more; world geometry is submitted to a collector and GUI geometry needs explicit draw calls.
 * {@link #getBuffer(Identifier)} hands out recording buffers instead, and {@link #flush()} sends everything recorded so far to the real destination.
 * Quads drawn this way are not affected by world lighting.
 */
public abstract class DrawTarget {

	private final Object2ObjectLinkedOpenHashMap<@Nullable Identifier, RecordingVertexConsumer> buffers = new Object2ObjectLinkedOpenHashMap<>();

	/**
	 * @param texture the texture to draw with, or {@code null} for plain coloured quads
	 * @return a buffer accepting quads already transformed into the space of the target
	 */
	public final VertexConsumer getBuffer(@Nullable Identifier texture) {
		return buffers.computeIfAbsent(texture, key -> new RecordingVertexConsumer());
	}

	/**
	 * Draw text at the given position of the current matrix stack.
	 */
	public abstract void drawText(PoseStack matrixStack, FormattedCharSequence text, float x, float y, int color, boolean shadow, int light);

	/**
	 * Send all recorded quads (in the order the textures were first used) and text to the destination.
	 */
	public final void flush() {
		buffers.forEach((texture, recordingVertexConsumer) -> {
			if (!recordingVertexConsumer.isEmpty()) {
				drawQuads(texture, recordingVertexConsumer);
			}
		});
		buffers.clear();
		flushText();
	}

	protected abstract void drawQuads(@Nullable Identifier texture, RecordingVertexConsumer recordingVertexConsumer);

	protected void flushText() {
	}
}
