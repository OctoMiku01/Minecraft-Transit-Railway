package org.mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * This implementation of a {@link BlockEntityRenderer} provides pre-transformed matrices and other helpful parameters.
 * <p>
 * The render state keeps a reference to the block entity. Extraction and submission both run on the render thread, so the existing
 * renderers can keep reading the block entity directly instead of copying every field into a render state.
 *
 * @param <T> the block entity type
 */
public abstract class BlockEntityRendererExtension<T extends BlockEntity> implements BlockEntityRenderer<T, BlockEntityRendererExtension.RenderState<T>> {

	@Override
	public final RenderState<T> createRenderState() {
		return new RenderState<>();
	}

	@Override
	public final void extractRenderState(T blockEntity, RenderState<T> state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.blockEntity = blockEntity;
		state.partialTicks = partialTicks;
	}

	@Override
	public final void submit(RenderState<T> state, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		final T blockEntity = state.blockEntity;
		final LocalPlayer clientPlayerEntity = Minecraft.getInstance().player;
		if (blockEntity != null && blockEntity.getLevel() instanceof ClientLevel clientWorld && clientPlayerEntity != null) {
			matrixStack.pushPose();
			matrixStack.translate(0.5, 0, 0.5);
			render(blockEntity, matrixStack, submitNodeCollector, clientWorld, clientPlayerEntity, state.partialTicks, state.lightCoords, OverlayTexture.NO_OVERLAY);
			matrixStack.popPose();
		}
		state.blockEntity = null;
	}

	/**
	 * A better implementation of the render method with helpful parameters.
	 *
	 * @param blockEntity         the {@link BlockEntity}
	 * @param matrixStack         a pre-transformed {@link PoseStack} centred at (0.5, 0, 0.5) of the block
	 * @param submitNodeCollector the provided {@link SubmitNodeCollector}
	 * @param clientWorld         the {@link ClientLevel} guaranteed to be non-null
	 * @param clientPlayerEntity  the {@link LocalPlayer} guaranteed to be non-null
	 * @param tickDelta           the number of ticks elapsed
	 * @param light               the light level of the block
	 * @param overlay             the texture overlay
	 */
	public abstract void render(T blockEntity, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, ClientLevel clientWorld, LocalPlayer clientPlayerEntity, float tickDelta, int light, int overlay);

	public static class RenderState<T extends BlockEntity> extends BlockEntityRenderState {

		@Nullable
		private T blockEntity;
		private float partialTicks;
	}
}
