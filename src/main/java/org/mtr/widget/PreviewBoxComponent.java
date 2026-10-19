package org.mtr.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.render.ScissorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;
import org.mtr.core.tool.Utilities;
import org.mtr.registry.UConverters;
import org.mtr.tool.Drawing;

import java.awt.*;
import java.util.function.Consumer;

public final class PreviewBoxComponent extends SlotBackgroundComponent {

	@Nullable
	private Double panClickX;
	@Nullable
	private Double panClickY;
	private double panX;
	private double panY;

	@Nullable
	private Float rotationClickX;
	@Nullable
	private Float rotationClickY;
	private float rotationY = getPlayerPitch();
	private float rotationX = getPlayerYaw();

	private float zoom = 1;

	private final Consumer<PoseStack> onDraw;

	private static final int PAN_MULTIPLIER = 32;
	private static final int ROTATION_MULTIPLIER = 1;
	/**
	 * How many blocks fit into the height of the preview at the default zoom level.
	 */
	private static final float DEFAULT_VISIBLE_BLOCKS = 4;
	private static final float ZOOM_STEP = 0.1F;

	public PreviewBoxComponent(boolean allowPan, boolean allowRotation, boolean allowZoom, Consumer<PoseStack> onDraw) {
		setBackgroundColor(Color.BLACK);
		this.onDraw = onDraw;

		onMouseClickConsumer(clickEvent -> {
			if (allowPan && (clickEvent.getMouseButton() == 1 || !allowRotation)) {
				panClickX = panX - clickEvent.getRelativeX() / PAN_MULTIPLIER;
				panClickY = panY - clickEvent.getRelativeY() / PAN_MULTIPLIER;
			}

			if (allowRotation && (clickEvent.getMouseButton() == 0 || !allowPan)) {
				rotationClickX = rotationX - clickEvent.getRelativeX() / ROTATION_MULTIPLIER;
				rotationClickY = rotationY - clickEvent.getRelativeY() / ROTATION_MULTIPLIER;
			}
		});

		onMouseDragConsumer((x, y, mouseButton) -> {
			if (panClickX != null && panClickY != null) {
				panX = x / PAN_MULTIPLIER + panClickX;
				panY = y / PAN_MULTIPLIER + panClickY;
			}

			if (rotationClickX != null && rotationClickY != null) {
				rotationX = x / ROTATION_MULTIPLIER + rotationClickX;
				rotationY = Utilities.clampSafe(y / ROTATION_MULTIPLIER + rotationClickY, -90, 90);
			}
		});

		onMouseReleaseRunnable(() -> {
			panClickX = null;
			panClickY = null;
			rotationClickX = null;
			rotationClickY = null;
		});

		onMouseScrollConsumer(mouseScrollEvent -> {
			if (allowZoom) {
				zoom += (float) mouseScrollEvent.getDelta();
			}
		});
	}

	/**
	 * TODO 26.2 port: older versions rendered the preview with a perspective projection into a separate framebuffer.
	 * The preview is currently drawn directly into the GUI with an orthographic projection, clipped to the preview area.
	 */
	@Override
	public void draw(UMatrixStack matrixStack) {
		super.draw(matrixStack);

		final double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		final ScissorState previousScissorState = ScissorState.Companion.active();
		new ScissorState(
			true,
			(int) Math.round((getLeft() + 1) * guiScale),
			(int) Math.round(Minecraft.getInstance().getWindow().getHeight() - (getBottom() - 1) * guiScale),
			(int) Math.round((getWidth() - 2) * guiScale),
			(int) Math.round((getHeight() - 2) * guiScale)
		).activate();

		final float scale = (getHeight() - 2) / DEFAULT_VISIBLE_BLOCKS * Math.max(ZOOM_STEP, 1 + zoom * ZOOM_STEP);
		final PoseStack poseStack = UConverters.convert(matrixStack);
		poseStack.pushPose();
		poseStack.translate((getLeft() + getRight()) / 2 + panX * scale, (getTop() + getBottom()) / 2 + panY * scale, 0);
		poseStack.scale(scale, -scale, scale);
		Drawing.rotateXDegrees(poseStack, rotationY);
		Drawing.rotateYDegrees(poseStack, rotationX);
		onDraw.accept(poseStack);
		poseStack.popPose();

		previousScissorState.activate();
	}

	public void updateFrom(PreviewBoxComponent previewBoxComponent) {
		panX = previewBoxComponent.panX;
		panY = previewBoxComponent.panY;
		rotationY = previewBoxComponent.rotationY;
		rotationX = previewBoxComponent.rotationX;
		zoom = previewBoxComponent.zoom;
	}

	private static float getPlayerYaw() {
		final LocalPlayer clientPlayerEntity = Minecraft.getInstance().player;
		return clientPlayerEntity == null ? 0 : clientPlayerEntity.getYRot() + 180;
	}

	private static float getPlayerPitch() {
		final LocalPlayer clientPlayerEntity = Minecraft.getInstance().player;
		return clientPlayerEntity == null ? 0 : clientPlayerEntity.getXRot();
	}
}
