package org.mtr.tool;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import gg.essential.universal.UGraphics;
import gg.essential.universal.render.UGpuSampler;
import gg.essential.universal.render.URenderPipeline;
import gg.essential.universal.vertex.UBuiltBuffer;
import kotlin.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.render.DrawTarget;
import org.mtr.render.RecordingVertexConsumer;

import java.util.function.Consumer;

/**
 * Draws immediately inside a GUI screen through UniversalCraft, which takes care of the projection and render pass setup.
 * Must only be used while a screen or component is drawing.
 */
public final class GuiDrawTarget extends DrawTarget {

	private final ObjectArrayList<ObjectObjectImmutablePair<Matrix4f, TextRenderable>> textRenderables = new ObjectArrayList<>();

	private static final UGpuSampler SAMPLER = UGpuSampler.Companion.get(UGpuSampler.AddressMode.CLAMP_TO_EDGE, UGpuSampler.AddressMode.CLAMP_TO_EDGE, UGpuSampler.FilterMode.NEAREST, UGpuSampler.FilterMode.NEAREST, false);
	private static final String SAMPLER_NAME = "Sampler0";

	@Override
	public void drawText(PoseStack matrixStack, FormattedCharSequence text, float x, float y, int color, boolean shadow, int light) {
		final Matrix4f pose = new Matrix4f(matrixStack.last().pose());
		Minecraft.getInstance().font.prepareText(text, x, y, color, shadow, false, 0).visit(new Font.GlyphVisitor() {
			@Override
			public void acceptRenderable(TextRenderable renderable) {
				textRenderables.add(new ObjectObjectImmutablePair<>(pose, renderable));
			}
		});
	}

	@Override
	protected void drawQuads(@Nullable Identifier texture, RecordingVertexConsumer recordingVertexConsumer) {
		draw(
			texture == null ? RenderPipelines.GUI : RenderPipelines.GUI_TEXTURED,
			texture == null ? null : Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView(),
			recordingVertexConsumer::replay
		);
	}

	@Override
	protected void flushText() {
		textRenderables.forEach(textRenderable -> draw(
			textRenderable.right().guiPipeline(),
			textRenderable.right().textureView(),
			bufferBuilder -> textRenderable.right().render(textRenderable.left(), bufferBuilder, LightCoordsUtil.FULL_BRIGHT, false)
		));
		textRenderables.clear();
	}

	private static void draw(RenderPipeline renderPipeline, @Nullable GpuTextureView textureView, Consumer<BufferBuilder> consumer) {
		final VertexFormat vertexFormat = renderPipeline.getVertexFormatBinding(0);
		if (vertexFormat == null) {
			return;
		}

		try (final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * 4)) {
			final BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, vertexFormat);
			consumer.accept(bufferBuilder);
			final MeshData meshData = bufferBuilder.build();
			if (meshData != null) {
				UBuiltBuffer.wrap(meshData).drawAndClose(URenderPipeline.wrap(renderPipeline), drawCallBuilder -> {
					if (textureView != null) {
						drawCallBuilder.texture(SAMPLER_NAME, UGraphics.getPlatformAdapter().textureView(textureView), SAMPLER);
					}
					return Unit.INSTANCE;
				});
			}
		}
	}
}
