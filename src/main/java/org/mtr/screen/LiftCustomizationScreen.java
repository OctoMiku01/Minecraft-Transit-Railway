package org.mtr.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.essential.elementa.components.ScrollComponent;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.components.UIWrappedText;
import gg.essential.elementa.constraints.*;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.mtr.MTR;
import org.mtr.client.CustomResourceLoader;
import org.mtr.client.MinecraftClientData;
import org.mtr.core.data.Lift;
import org.mtr.core.operation.UpdateDataRequest;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;
import org.mtr.generated.lang.TranslationProvider;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.model.LiftModel;
import org.mtr.packet.PacketUpdateData;
import org.mtr.registry.RegistryClient;
import org.mtr.registry.UConverters;
import org.mtr.render.RenderLifts;
import org.mtr.resource.LiftResource;
import org.mtr.tool.BlockRendererHelper;
import org.mtr.tool.Drawing;
import org.mtr.tool.GuiHelper;
import org.mtr.tool.ReleasedDynamicTextureRegistry;
import org.mtr.widget.*;

import java.awt.*;

/**
 * Elementa lift customization panel for dimensions, offsets, style and orientation.
 */
public final class LiftCustomizationScreen extends WindowBase {

	private Direction liftDirection;
	private final Lift lift;

	private final NumberInputComponent dimensionXInputComponent;
	private final NumberInputComponent dimensionYInputComponent;
	private final NumberInputComponent dimensionZInputComponent;
	private final NumberInputComponent offsetXInputComponent;
	private final NumberInputComponent offsetYInputComponent;
	private final NumberInputComponent offsetZInputComponent;
	private final CheckboxComponent isDoubleSidedCheckboxComponent;
	private final PreviewBoxComponent previewBoxComponent;

	private static final int MIN_DIMENSION = 2;
	private static final int MAX_DIMENSION = 16;
	private static final int MAX_OFFSET = 16;
	private static final int LEFT_WIDTH = 96;

	public LiftCustomizationScreen(Lift lift) {
		this.lift = lift;
		liftDirection = Direction.fromYRot(lift.getAngle().angleDegrees);

		final BackgroundComponent backgroundComponent = new BackgroundComponent(getWindow(), ObjectImmutableList.of(
			new ObjectObjectImmutablePair<>(ReleasedDynamicTextureRegistry.BRUSH_TEXTURE.get(), TranslationProvider.GUI_MTR_LIFT_CUSTOMIZATION.getString())
		));
		final UIContainer leftContainer = (UIContainer) new UIContainer()
			.setChildOf(backgroundComponent.containers[0])
			.setWidth(new CoerceAtMostConstraint(new RelativeConstraint(0.5F), new PixelConstraint(LEFT_WIDTH)))
			.setHeight(new RelativeConstraint());

		new UIWrappedText(TranslationProvider.GUI_MTR_LIFT_CUSTOMIZATION.getString(), false)
			.setChildOf(leftContainer)
			.setWidth(new RelativeConstraint())
			.setColor(new Color(GuiHelper.MINECRAFT_GUI_TITLE_TEXT_COLOR));

		final ScrollComponent scrollComponent = ((ScrollPanelComponent) new ScrollPanelComponent(true)
			.setChildOf(leftContainer)
			.setY(new SiblingConstraint(GuiHelper.DEFAULT_PADDING))
			.setWidth(new RelativeConstraint())
			.setHeight(new SubtractiveConstraint(new FillConstraint(), new PixelConstraint(GuiHelper.DEFAULT_PADDING)))).contentContainer;

		previewBoxComponent = (PreviewBoxComponent) new PreviewBoxComponent(true, true, true, this::onDrawPreview)
			.setChildOf(backgroundComponent.containers[0])
			.setX(new SiblingConstraint(GuiHelper.DEFAULT_PADDING))
			.setWidth(new SubtractiveConstraint(new FillConstraint(), new PixelConstraint(GuiHelper.DEFAULT_PADDING)))
			.setHeight(new RelativeConstraint());

		GuiHelper.createLabel(scrollComponent, TranslationProvider.GUI_MTR_DIMENSIONS.getString());
		dimensionXInputComponent = createNumberInput(scrollComponent, "X", lift.getHeight(), MIN_DIMENSION, MAX_DIMENSION);
		dimensionYInputComponent = createNumberInput(scrollComponent, "Y", lift.getWidth(), MIN_DIMENSION, MAX_DIMENSION);
		dimensionZInputComponent = createNumberInput(scrollComponent, "Z", lift.getDepth(), MIN_DIMENSION, MAX_DIMENSION);

		GuiHelper.createSpacing(scrollComponent);
		GuiHelper.createLabel(scrollComponent, TranslationProvider.GUI_MTR_OFFSET.getString());
		offsetXInputComponent = createNumberInput(scrollComponent, "X", lift.getOffsetX(), -MAX_OFFSET, MAX_OFFSET);
		offsetYInputComponent = createNumberInput(scrollComponent, "Y", lift.getOffsetY(), -MAX_OFFSET, MAX_OFFSET);
		offsetZInputComponent = createNumberInput(scrollComponent, "Z", lift.getOffsetZ(), -MAX_OFFSET, MAX_OFFSET);

		GuiHelper.createSpacing(scrollComponent);
		isDoubleSidedCheckboxComponent = (CheckboxComponent) new CheckboxComponent()
			.setChildOf(scrollComponent)
			.setY(new SiblingConstraint(GuiHelper.DEFAULT_PADDING))
			.setWidth(new RelativeConstraint());

		isDoubleSidedCheckboxComponent.setText(TranslationProvider.GUI_MTR_LIFT_IS_DOUBLE_SIDED.getString());
		isDoubleSidedCheckboxComponent.onClick(() -> lift.setIsDoubleSided(isDoubleSidedCheckboxComponent.isChecked()));

		GuiHelper.createSpacing(scrollComponent);
		GuiHelper.createLabel(scrollComponent, TranslationProvider.GUI_MTR_STYLES.getString());
		final ButtonComponent editStylesButtonComponent = (ButtonComponent) new ButtonComponent(true)
			.setChildOf(scrollComponent)
			.setY(new SiblingConstraint())
			.setWidth(new RelativeConstraint());

		editStylesButtonComponent.setText(Component.translatable("selectWorld.edit").getString());
		editStylesButtonComponent.onClick(() -> UMinecraft.setCurrentScreenObj(createLiftStyleSelectorScreen()));

		final ButtonComponent rotateAnticlockwiseButton = (ButtonComponent) new ButtonComponent(true)
			.setChildOf(scrollComponent)
			.setY(new SiblingConstraint())
			.setWidth(new RelativeConstraint());

		rotateAnticlockwiseButton.setText(TranslationProvider.GUI_MTR_ROTATE_ANTICLOCKWISE.getString());
		rotateAnticlockwiseButton.onClick(() -> {
			liftDirection = liftDirection.getCounterClockWise();
			lift.setAngle(Angle.fromAngle(liftDirection.toYRot()));
		});

		final ButtonComponent rotateClockwiseButton = (ButtonComponent) new ButtonComponent(true)
			.setChildOf(scrollComponent)
			.setY(new SiblingConstraint())
			.setWidth(new RelativeConstraint());

		rotateClockwiseButton.setText(TranslationProvider.GUI_MTR_ROTATE_CLOCKWISE.getString());
		rotateClockwiseButton.onClick(() -> {
			liftDirection = liftDirection.getClockWise();
			lift.setAngle(Angle.fromAngle(liftDirection.toYRot()));
		});
	}

	@Override
	public void onScreenClose() {
		lift.setDimensions(getHeight(), getWidth(), getDepth(), getOffsetX(), getOffsetY(), getOffsetZ());
		lift.setIsDoubleSided(isDoubleSidedCheckboxComponent.isChecked());

		RegistryClient.sendPacketToServer(new PacketUpdateData(new UpdateDataRequest(MinecraftClientData.getInstance()).addLift(lift)));
		super.onScreenClose();
	}

	/**
	 * Draw the lift car with the current settings next to the lift track of the current floor, centred on the car.
	 * Uses the same transformations as {@link RenderLifts}.
	 */
	private void onDrawPreview(PoseStack matrixStack) {
		final double height = getHeight();
		final double width = getWidth();
		final double depth = getDepth();
		final double offsetX = getOffsetX();
		final double offsetY = getOffsetY();
		final double offsetZ = getOffsetZ();
		// The corners of the car stick out towards the camera, so fit the diagonal of the car and the track block instead of the longest side
		previewBoxComponent.setVisibleBlocks((float) (Math.sqrt(height * height + width * width + depth * depth) + Math.max(Math.abs(offsetX), Math.max(Math.abs(offsetY), Math.abs(offsetZ))) * 2) * 1.5F);

		matrixStack.pushPose();
		matrixStack.translate(-offsetX, -offsetY - height / 2, -offsetZ);

		final ClientLevel clientWorld = Minecraft.getInstance().level;
		if (clientWorld != null) {
			BlockRendererHelper.renderBlock(UConverters.convert(matrixStack), clientWorld.getBlockState(MTR.positionToBlockPos(lift.getCurrentFloor().getPosition())), 0, 0, 0, 0, 1);
		}

		matrixStack.translate(offsetX, offsetY, offsetZ);
		Drawing.rotateYRadians(matrixStack, (float) (Math.PI / 2 - lift.getAngle().angleRadians));
		Drawing.rotateXRadians(matrixStack, (float) Math.PI);
		LiftModel.iterateModels(
			(int) Math.round(height * 2), (int) Math.round(width), (int) Math.round(depth), isDoubleSidedCheckboxComponent.isChecked(),
			RenderLifts.getLiftResource(lift.getStyle()).getTexture(),
			0, 0,
			(models, transformation) -> {
				matrixStack.pushPose();
				if (transformation != null) {
					transformation.accept(matrixStack);
				}
				final UMatrixStack modelMatrixStack = UConverters.convert(matrixStack);
				models.forEach((renderStage, newOptimizedModels) -> newOptimizedModels.forEach(newOptimizedModel -> BlockRendererHelper.renderModel(modelMatrixStack, newOptimizedModel, 1)));
				matrixStack.popPose();
			}
		);

		matrixStack.popPose();
	}

	private double getHeight() {
		return Utilities.clampSafe(dimensionXInputComponent.getValue(), MIN_DIMENSION, MAX_DIMENSION);
	}

	private double getWidth() {
		return Utilities.clampSafe(dimensionYInputComponent.getValue(), MIN_DIMENSION, MAX_DIMENSION);
	}

	private double getDepth() {
		return Utilities.clampSafe(dimensionZInputComponent.getValue(), MIN_DIMENSION, MAX_DIMENSION);
	}

	private double getOffsetX() {
		return Utilities.clampSafe(offsetXInputComponent.getValue(), -MAX_OFFSET, MAX_OFFSET);
	}

	private double getOffsetY() {
		return Utilities.clampSafe(offsetYInputComponent.getValue(), -MAX_OFFSET, MAX_OFFSET);
	}

	private double getOffsetZ() {
		return Utilities.clampSafe(offsetZInputComponent.getValue(), -MAX_OFFSET, MAX_OFFSET);
	}

	private LiftStyleSelectorScreen createLiftStyleSelectorScreen() {
		final ObjectImmutableList<LiftResource> allLiftResources = CustomResourceLoader.getLifts();
		final LiftStyleSelectorScreen liftStyleSelectorScreen = new LiftStyleSelectorScreen(liftStyles -> lift.setStyle(liftStyles.isEmpty() ? allLiftResources.getFirst().getId() : liftStyles.getFirst().getId()), this);
		liftStyleSelectorScreen.setAvailableList(allLiftResources);

		allLiftResources.forEach(liftResource -> {
			if (liftResource.getId().equals(lift.getStyle())) {
				liftStyleSelectorScreen.selectData(liftResource);
			}
		});

		return liftStyleSelectorScreen;
	}

	private static NumberInputComponent createNumberInput(UIContainer container, String axis, double value, int min, int max) {
		final NumberInputComponent numberInputComponent = (NumberInputComponent) new NumberInputComponent(min, max, 0.5, true, null)
			.setChildOf(container)
			.setY(new SiblingConstraint())
			.setWidth(new RelativeConstraint());

		numberInputComponent.setPrefix(axis + " ");
		numberInputComponent.setValue(value);
		return numberInputComponent;
	}
}
