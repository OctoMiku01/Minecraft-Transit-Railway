package org.mtr.tool;

import gg.essential.universal.UMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.model.NewOptimizedModel;
import org.mtr.widget.ImageComponentBase;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class BlockRendererHelper {

	private static final Object2ObjectOpenHashMap<String, ReleasedDynamicTextureRegistry.Holder> BLOCK_TEXTURE_MAP = new Object2ObjectOpenHashMap<>();
	public static void renderBlock(UMatrixStack matrixStack, BlockState blockState, long renderKey, double x, double y, double z, double brightness) {
		final List<BlockStateModelPart> blockStateModelParts = new ArrayList<>();
		Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState).collectParts(RandomSource.create(), blockStateModelParts);
		final int shade = (int) Math.floor(0xFF * brightness);
		final Color color = new Color(shade, shade, shade);

		for (int i = -1; i < Direction.values().length; i++) {
			final Direction direction = i < 0 ? null : Direction.values()[i];
			for (final BlockStateModelPart blockStateModelPart : blockStateModelParts) {
				for (final BakedQuad bakedQuad : blockStateModelPart.getQuads(direction)) {
					final TextureAtlasSprite sprite = bakedQuad.materialInfo().sprite();
					final Identifier tempIdentifier = sprite.contents().name();
					final Identifier newIdentifier = Identifier.fromNamespaceAndPath(tempIdentifier.getNamespace(), String.format("textures/%s.png", tempIdentifier.getPath()));

					ImageComponentBase.drawTexture(BLOCK_TEXTURE_MAP.computeIfAbsent(newIdentifier.toString(), key -> ReleasedDynamicTextureRegistry.INSTANCE.create(newIdentifier)).get(), vertexConsumer -> {
						for (int k = 0; k < BakedQuad.VERTEX_COUNT; k++) {
							final Vector3fc position = bakedQuad.position(k);
							final long packedUV = bakedQuad.packedUV(k);
							vertexConsumer.pos(
								matrixStack,
								x + position.x(),
								y + position.y(),
								z + position.z()
							).tex(
								(UVPair.unpackU(packedUV) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
								(UVPair.unpackV(packedUV) - sprite.getV0()) / (sprite.getV1() - sprite.getV0())
							).color(color).endVertex();
						}
					});
				}
			}
		}
	}

	/**
	 * Draw an optimised model in a GUI preview, using the same texture cache as the block previews.
	 */
	public static void renderModel(UMatrixStack matrixStack, NewOptimizedModel newOptimizedModel, double brightness) {
		final Identifier texture = newOptimizedModel.texture;
		ImageComponentBase.drawTexture(BLOCK_TEXTURE_MAP.computeIfAbsent(texture.toString(), key -> ReleasedDynamicTextureRegistry.INSTANCE.create(texture)).get(), vertexConsumer -> newOptimizedModel.forEachQuadVertex(storedVertexData -> {
			final Color color = storedVertexData.color();
			vertexConsumer.pos(matrixStack, storedVertexData.x(), storedVertexData.y(), storedVertexData.z())
				.tex(storedVertexData.u(), storedVertexData.v())
				.color((int) Math.floor(color.getRed() * brightness), (int) Math.floor(color.getGreen() * brightness), (int) Math.floor(color.getBlue() * brightness), color.getAlpha())
				.endVertex();
		}));
	}
}
