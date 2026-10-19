package org.mtr.tool;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.render.DrawTarget;
import org.mtr.render.RecordingVertexConsumer;

/**
 * Draws into a vanilla screen or the HUD by adding render states to a {@link GuiGraphicsExtractor}, which Minecraft draws later in the frame.
 * Text is added after all quads when flushing, so that it stays on top.
 */
public final class GuiExtractorDrawTarget extends DrawTarget {

	private final GuiGraphicsExtractor guiGraphics;
	private final ObjectArrayList<Runnable> texts = new ObjectArrayList<>();

	public GuiExtractorDrawTarget(GuiGraphicsExtractor guiGraphics) {
		this.guiGraphics = guiGraphics;
	}

	/**
	 * Create a matrix stack matching the current 2D pose of the GUI, so that 3D drawing helpers can be used in a GUI.
	 */
	public PoseStack createMatrixStack() {
		final Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());
		final PoseStack matrixStack = new PoseStack();
		matrixStack.last().pose().set(
			pose.m00(), pose.m01(), 0, 0,
			pose.m10(), pose.m11(), 0, 0,
			0, 0, 1, 0,
			pose.m20(), pose.m21(), 0, 1
		);
		return matrixStack;
	}

	@Override
	public void drawText(PoseStack matrixStack, FormattedCharSequence text, float x, float y, int color, boolean shadow, int light) {
		final Matrix3x2f pose = toMatrix3x2(matrixStack.last().pose()).translate(x, y);
		texts.add(() -> {
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().set(pose);
			guiGraphics.text(Minecraft.getInstance().font, text, 0, 0, color, shadow);
			guiGraphics.pose().popMatrix();
		});
	}

	@Override
	protected void drawQuads(@Nullable Identifier texture, RecordingVertexConsumer recordingVertexConsumer) {
		final float[] bounds = recordingVertexConsumer.getBounds2D();
		final ScreenRectangle screenRectangle = new ScreenRectangle((int) Math.floor(bounds[0]), (int) Math.floor(bounds[1]), (int) Math.ceil(bounds[2] - Math.floor(bounds[0])), (int) Math.ceil(bounds[3] - Math.floor(bounds[1])));
		final ScreenRectangle scissorArea = guiGraphics.scissorStack.peek();
		final TextureSetup textureSetup;
		if (texture == null) {
			textureSetup = TextureSetup.noTexture();
		} else {
			final AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
			textureSetup = TextureSetup.singleTexture(abstractTexture.getTextureView(), abstractTexture.getSampler());
		}

		guiGraphics.guiRenderState.addGuiElement(new RecordedGuiElementRenderState(
			texture == null ? RenderPipelines.GUI : RenderPipelines.GUI_TEXTURED,
			textureSetup,
			recordingVertexConsumer,
			scissorArea,
			scissorArea == null ? screenRectangle : scissorArea.intersection(screenRectangle)
		));
	}

	@Override
	protected void flushText() {
		texts.forEach(Runnable::run);
		texts.clear();
	}

	private static Matrix3x2f toMatrix3x2(Matrix4fc matrix4f) {
		return new Matrix3x2f(matrix4f.m00(), matrix4f.m01(), matrix4f.m10(), matrix4f.m11(), matrix4f.m30(), matrix4f.m31());
	}

	private record RecordedGuiElementRenderState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		RecordingVertexConsumer recordingVertexConsumer,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
	) implements GuiElementRenderState {

		@Override
		public void buildVertices(VertexConsumer vertexConsumer) {
			recordingVertexConsumer.replay(vertexConsumer);
		}
	}
}
