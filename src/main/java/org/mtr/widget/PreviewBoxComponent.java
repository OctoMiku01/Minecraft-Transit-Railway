package org.mtr.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.render.ScissorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Matrix4f;
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
	/**
	 * Distance of the virtual camera from the centre of the preview in blocks. Larger values give a weaker perspective.
	 */
	private static final float CAMERA_DISTANCE = 8;
	/**
	 * The GUI only draws depths between 0 and 10000, so the preview is moved in front of the GUI at this depth.
	 */
	private static final float DEPTH_CENTER = 1000;
	private static final float DEPTH_PER_BLOCK = 100;

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
	 * Older versions rendered the preview with a perspective projection into a separate framebuffer.
	 * The preview is now drawn directly into the GUI, clipped to the preview area, with the perspective built into the pose (see {@link #createPerspectiveMatrix}).
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

		final PoseStack poseStack = UConverters.convert(matrixStack);
		poseStack.pushPose();
		poseStack.mulPose(createPerspectiveMatrix((getLeft() + getRight()) / 2, (getTop() + getBottom()) / 2, (getHeight() - 2) / DEFAULT_VISIBLE_BLOCKS));
		poseStack.translate(panX, -panY, 0);
		final float zoomScale = Math.max(ZOOM_STEP, 1 + zoom * ZOOM_STEP);
		poseStack.scale(zoomScale, zoomScale, zoomScale);
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

	/**
	 * Maps view space (in blocks, y up, camera looking towards negative z) to GUI coordinates with a perspective projection.
	 * The GUI projection is orthographic but keeps the w component, so the GPU's perspective division still applies.
	 * Points at depth 0 are scaled by {@code pixelsPerBlock}; closer points get bigger and further points smaller.
	 * The depth is mapped around {@link #DEPTH_CENTER} so that the whole preview stays inside the depth range of the GUI.
	 *
	 * @param centerX        the GUI x coordinate of the view centre
	 * @param centerY        the GUI y coordinate of the view centre
	 * @param pixelsPerBlock how many GUI pixels one block at depth 0 covers
	 */
	private static Matrix4f createPerspectiveMatrix(float centerX, float centerY, float pixelsPerBlock) {
		// Columns: x' = s * x + cx * w, y' = -s * y + cy * w, z' = d * z + dc * w, w = 1 - z / distance
		return new Matrix4f(
			pixelsPerBlock, 0, 0, 0,
			0, -pixelsPerBlock, 0, 0,
			-centerX / CAMERA_DISTANCE, -centerY / CAMERA_DISTANCE, DEPTH_PER_BLOCK - DEPTH_CENTER / CAMERA_DISTANCE, -1 / CAMERA_DISTANCE,
			centerX, centerY, DEPTH_CENTER, 1
		);
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
