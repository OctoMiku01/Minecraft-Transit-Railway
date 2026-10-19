package org.mtr.model;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.resource.RenderStage;
import org.mtr.tool.Drawing;

import java.util.function.Consumer;

/**
 * The lift car model, built from the pieces of the old {@code ModelLift1} for any car size.
 *
 * <p>Coordinates are in pixels in the model space of vehicles (y pointing down, floor at {@code y = 0}),
 * so the models can be drawn with the transformations from
 * {@link org.mtr.render.RenderVehicles#getStoredMatrixTransformations}. The car itself is built once per size
 * and texture; the door panels are separate models that are moved at draw time.</p>
 */
public final class LiftModel {

	private static final int TEXTURE_SIZE = 128;
	private static final float DOOR_OPEN_DISTANCE = 12;
	private static final float QUARTER_TURN = 1.5708F;

	private static final Part MAIN = new Part(new Group(0, cube(0, 34, -8, 0, -8, 16, 0, 16)));
	private static final Part MAIN_CEILING = new Part(new Group(0, cube(79, 44, -8, -32, -8, 16, 0, 16)));
	private static final Part MAIN_EDGE = new Part(new Group(0,
		cube(18, 44, -4, 0, -8, 8, 0, 6),
		cube(76, 33, -4, -32, -3, 8, 32, 1),
		cube(28, 52, -4, -13, -4, 8, 1, 1),
		cube(26, 50, -4, -2, -4, 8, 1, 1)
	));
	private static final Part MAIN_EDGE_WALL = new Part(new Group(0, cube(76, 33, -4, -40, -3, 8, 8, 1)));
	private static final Part MAIN_EDGE_CEILING = new Part(new Group(0, cube(97, 44, -4, -32, -8, 8, 0, 6)));
	private static final Part MAIN_CORNER = new Part(new Group(0,
		cube(20, 44, 2, 0, -8, 6, 0, 6),
		cube(112, 62, 3, -32, -3, 5, 32, 1),
		cube(29, 52, 5, -13, -4, 3, 1, 1),
		cube(27, 50, 5, -2, -4, 3, 1, 1),
		cube(104, 68, 2, -32, -3, 1, 32, 1)
	), new Group(-QUARTER_TURN,
		cube(36, 50, -8, -2, -4, 3, 1, 1),
		cube(38, 52, -8, -13, -4, 3, 1, 1),
		cube(112, 62, -8, -32, -3, 5, 32, 1)
	));
	private static final Part MAIN_CORNER_WALL = new Part(new Group(0,
		cube(112, 62, 3, -40, -3, 5, 8, 1),
		cube(104, 68, 2, -40, -3, 1, 8, 1)
	), new Group(-QUARTER_TURN, cube(112, 62, -8, -40, -3, 5, 8, 1)));
	private static final Part MAIN_CORNER_CEILING = new Part(new Group(0, cube(99, 44, 2, -32, -8, 6, 0, 6)));
	private static final Part MAIN_EXTERIOR = new Part(new Group(0, cube(0, 17, -8, 0, -8, 16, 1, 16)));
	private static final Part MAIN_EXTERIOR_CEILING = new Part(new Group(0, cube(0, 0, -8, -33, -8, 16, 1, 16)));
	private static final Part MAIN_EXTERIOR_EDGE = new Part(new Group(0, cube(18, 27, -4, 0, -8, 8, 1, 6)));
	private static final Part MAIN_EXTERIOR_EDGE_CEILING = new Part(new Group(0, cube(18, 10, -4, -33, -8, 8, 1, 6)));
	private static final Part MAIN_EXTERIOR_CORNER = new Part(new Group(0, cube(20, 27, 2, 0, -8, 6, 1, 6)));
	private static final Part MAIN_EXTERIOR_CORNER_WALL = new Part(new Group(0, cube(108, 68, 2, -40, -3, 1, 8, 1)));
	private static final Part MAIN_EXTERIOR_CORNER_CEILING = new Part(new Group(0, cube(20, 10, 2, -33, -8, 6, 1, 6)));
	private static final Part MAIN_LIGHT = new Part(new Group(0, cube(79, 28, -8, -32.5F, -8, 16, 0, 16)));
	private static final Part DOOR = new Part(new Group(0,
		cube(90, 66, -16, -32, 4, 4, 32, 3),
		cube(14, 84, 12, -32, 4, 4, 32, 3),
		cube(20, 101, -16, 0, 0, 32, 0, 8)
	));
	private static final Part DOOR_LEFT = new Part(new Group(0, cube(52, 68, -12, -32, 6, 12, 32, 0)));
	private static final Part DOOR_RIGHT = new Part(new Group(0, cube(28, 68, 0, -32, 6, 12, 32, 0)));
	private static final Part DOOR_WALL = new Part(new Group(0, cube(48, 0, -16, -40, 4, 32, 8, 3)));
	private static final Part DOOR_CEILING = new Part(new Group(0, cube(20, 101, -16, -32, 0, 32, 0, 8)));
	private static final Part DOOR_EXTERIOR = new Part(new Group(0,
		cube(0, 84, -16, -32, 4, 4, 32, 3),
		cube(76, 66, 12, -32, 4, 4, 32, 3),
		cube(28, 109, -16, 0, 0, 32, 1, 8)
	));
	private static final Part DOOR_LEFT_EXTERIOR = new Part(new Group(0, cube(0, 50, -12, -32, 6, 12, 32, 2)));
	private static final Part DOOR_RIGHT_EXTERIOR = new Part(new Group(0, cube(48, 34, 0, -32, 6, 12, 32, 2)));
	private static final Part DOOR_WALL_EXTERIOR = new Part(new Group(0, cube(48, 17, -16, -40, 5, 32, 8, 3)));
	private static final Part DOOR_CEILING_EXTERIOR = new Part(new Group(0, cube(0, 119, -16, -33, 0, 32, 1, 8)));
	private static final Part WALL_PATCH = new Part(new Group(-QUARTER_TURN,
		cube(108, 95, 0, -32, 13, 4, 32, 1),
		cube(30, 50, 0, -2, 12, 4, 1, 1),
		cube(32, 52, 0, -13, 12, 4, 1, 1)
	), new Group(QUARTER_TURN,
		cube(108, 95, -4, -32, 13, 4, 32, 1),
		cube(30, 50, -4, -2, 12, 4, 1, 1),
		cube(32, 52, -4, -13, 12, 4, 1, 1)
	));
	private static final Part WALL_PATCH_WALL = new Part(
		new Group(-QUARTER_TURN, cube(108, 95, 0, -40, 13, 4, 8, 1)),
		new Group(QUARTER_TURN, cube(108, 95, -4, -40, 13, 4, 8, 1))
	);

	private static final Object2ObjectOpenHashMap<CarKey, Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>>> CAR_MODELS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>>> DOOR_LEFT_MODELS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>>> DOOR_RIGHT_MODELS = new Object2ObjectOpenHashMap<>();

	/**
	 * Iterate the models making up a lift car. Must be called on the render thread.
	 *
	 * @param height        the height of the car in half blocks
	 * @param width         the width of the car in blocks
	 * @param depth         the depth of the car in blocks
	 * @param isDoubleSided whether the car has a second door at the back
	 * @param texture       the lift texture
	 * @param door1Value    how far the front door is open, from {@code 0} to {@code 1}
	 * @param door2Value    how far the back door is open, from {@code 0} to {@code 1}
	 * @param consumer      called with each group of models and the extra transformation it needs (or {@code null})
	 */
	public static void iterateModels(int height, int width, int depth, boolean isDoubleSided, Identifier texture, float door1Value, float door2Value, ModelConsumer consumer) {
		consumer.accept(CAR_MODELS.computeIfAbsent(new CarKey(height, width, depth, isDoubleSided, texture), key -> buildCar(height, width, depth, isDoubleSided, texture)), null);
		iterateDoorModels(texture, depth, door1Value, true, consumer);
		if (isDoubleSided) {
			iterateDoorModels(texture, depth, door2Value, false, consumer);
		}
	}

	private static void iterateDoorModels(Identifier texture, int depth, float doorValue, boolean flipped, ModelConsumer consumer) {
		final float z = flipped ? 8 - depth * 8 : -8 + depth * 8;
		// Each door panel slides away from the centre of the doorway
		final float doorLeftX = Math.clamp(doorValue, 0, 1) * DOOR_OPEN_DISTANCE * (flipped ? 1 : -1);
		consumer.accept(DOOR_LEFT_MODELS.computeIfAbsent(texture, key -> buildDoor(DOOR_LEFT, DOOR_LEFT_EXTERIOR, texture)), matrixStack -> placeDoor(matrixStack, doorLeftX, z, flipped));
		consumer.accept(DOOR_RIGHT_MODELS.computeIfAbsent(texture, key -> buildDoor(DOOR_RIGHT, DOOR_RIGHT_EXTERIOR, texture)), matrixStack -> placeDoor(matrixStack, -doorLeftX, z, flipped));
	}

	private static void placeDoor(PoseStack matrixStack, float x, float z, boolean flipped) {
		matrixStack.translate(x / 16, 0, z / 16);
		if (flipped) {
			Drawing.rotateYRadians(matrixStack, (float) Math.PI);
		}
	}

	private static Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> buildCar(int height, int width, int depth, boolean isDoubleSided, Identifier texture) {
		final int heightCount = height - 4;
		final int heightOffset = -heightCount * 8;
		final Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models = new Object2ObjectOpenHashMap<>();

		for (final RenderStage renderStage : new RenderStage[]{RenderStage.LIGHT, RenderStage.INTERIOR, RenderStage.EXTERIOR}) {
			final MeshDefinition meshDefinition = new MeshDefinition();
			final Placer placer = new Placer(meshDefinition.getRoot(), heightCount);

			for (int i = 0; i <= width; i++) {
				for (int j = 0; j <= depth; j++) {
					final float x = (i - width / 2F) * 16;
					final float z = (j - depth / 2F) * 16;

					final boolean edge1X = i == 0;
					final boolean edge2X = i == width;
					final boolean edge1Z = j == 0;
					final boolean edge2Z = j == depth;

					if (renderStage == RenderStage.LIGHT) {
						if (!edge1X && !edge2X && !edge1Z && !edge2Z) {
							placer.place(MAIN_LIGHT, x, heightOffset, z, 0);
						}
						continue;
					}

					final boolean interior = renderStage == RenderStage.INTERIOR;
					final Part mainPiece = interior ? MAIN : MAIN_EXTERIOR;
					final Part mainCeilingPiece = interior ? MAIN_CEILING : MAIN_EXTERIOR_CEILING;
					final Part mainEdgePiece = interior ? MAIN_EDGE : MAIN_EXTERIOR_EDGE;
					final Part mainEdgeCeilingPiece = interior ? MAIN_EDGE_CEILING : MAIN_EXTERIOR_EDGE_CEILING;
					final Part mainCornerPiece = interior ? MAIN_CORNER : MAIN_EXTERIOR_CORNER;
					final Part mainCornerWallPiece = interior ? MAIN_CORNER_WALL : MAIN_EXTERIOR_CORNER_WALL;
					final Part mainCornerCeilingPiece = interior ? MAIN_CORNER_CEILING : MAIN_EXTERIOR_CORNER_CEILING;

					if (!edge1X && !edge2X && !edge1Z && !edge2Z) {
						placer.place(mainPiece, x, 0, z, 0);
						placer.place(mainCeilingPiece, x, heightOffset, z, 0);
					}

					if (edge1X && !edge2X && !edge1Z && !edge2Z) {
						placeEdge(placer, interior, mainEdgePiece, mainEdgeCeilingPiece, x, z, heightOffset, (float) -Math.PI / 2);
					}
					if (!edge1X && edge2X && !edge1Z && !edge2Z) {
						placeEdge(placer, interior, mainEdgePiece, mainEdgeCeilingPiece, x, z, heightOffset, (float) Math.PI / 2);
					}
					if (!edge1X && !edge2X && !edge1Z && edge2Z && !isDoubleSided) {
						for (final int offset : new int[]{-4, 4}) {
							placer.place(mainEdgePiece, x + offset, 0, z, 0);
							if (interior) {
								placer.placeWall(MAIN_EDGE_WALL, x + offset, z, 0);
							}
							placer.place(mainEdgeCeilingPiece, x + offset, heightOffset, z, 0);
						}
					}

					if (edge1X && !edge2X && !edge1Z && edge2Z && (width > 2 || !isDoubleSided)) {
						placeCorner(placer, interior, mainCornerPiece, mainCornerWallPiece, mainCornerCeilingPiece, x, z, heightOffset, 0);
					}
					if (!edge1X && edge2X && !edge1Z && edge2Z && (width > 2 || !isDoubleSided)) {
						placeCorner(placer, interior, mainCornerPiece, mainCornerWallPiece, mainCornerCeilingPiece, x, z, heightOffset, (float) Math.PI / 2);
					}
					if (edge1X && !edge2X && edge1Z && !edge2Z && width > 2) {
						placeCorner(placer, interior, mainCornerPiece, mainCornerWallPiece, mainCornerCeilingPiece, x, z, heightOffset, (float) -Math.PI / 2);
					}
					if (!edge1X && edge2X && edge1Z && !edge2Z && width > 2) {
						placeCorner(placer, interior, mainCornerPiece, mainCornerWallPiece, mainCornerCeilingPiece, x, z, heightOffset, (float) Math.PI);
					}
				}
			}

			if (renderStage != RenderStage.LIGHT) {
				final boolean interior = renderStage == RenderStage.INTERIOR;
				final Part doorPiece = interior ? DOOR : DOOR_EXTERIOR;
				final Part doorWallPiece = interior ? DOOR_WALL : DOOR_WALL_EXTERIOR;
				final Part doorCeilingPiece = interior ? DOOR_CEILING : DOOR_CEILING_EXTERIOR;
				final Part mainEdgePiece = interior ? MAIN_EDGE : MAIN_EXTERIOR_EDGE;
				final Part mainEdgeCeilingPiece = interior ? MAIN_EDGE_CEILING : MAIN_EXTERIOR_EDGE_CEILING;

				// The front door is always there, the back door only for double-sided lifts
				for (int side = 0; side < (isDoubleSided ? 2 : 1); side++) {
					final boolean flipped = side == 0;
					final float rotation = flipped ? (float) Math.PI : 0;
					final float z = flipped ? 8 - depth * 8 : -8 + depth * 8;
					placer.place(doorPiece, 0, 0, z, rotation);
					placer.placeWall(doorWallPiece, 0, z, rotation);
					placer.place(doorCeilingPiece, 0, heightOffset, z, rotation);

					if (interior && width == 2) {
						placer.place(WALL_PATCH, 0, 0, z, rotation);
						placer.placeWall(WALL_PATCH_WALL, 0, z, rotation);
					}

					final float edgeZ = flipped ? -depth * 8 : depth * 8;
					for (int i = 1; i < width - 2; i++) {
						for (final float x : new float[]{i * 8 - width * 8 + 4, -i * 8 + width * 8 - 4}) {
							placer.place(mainEdgePiece, x, 0, edgeZ, rotation);
							if (interior) {
								placer.placeWall(MAIN_EDGE_WALL, x, edgeZ, rotation);
							}
							placer.place(mainEdgeCeilingPiece, x, heightOffset, edgeZ, rotation);
						}
					}
				}
			}

			models.put(renderStage, ObjectArrayList.of(createModel(meshDefinition, texture)));
		}

		return models;
	}

	private static void placeEdge(Placer placer, boolean interior, Part mainEdgePiece, Part mainEdgeCeilingPiece, float x, float z, int heightOffset, float rotation) {
		for (final int offset : new int[]{-4, 4}) {
			placer.place(mainEdgePiece, x, 0, z + offset, rotation);
			if (interior) {
				placer.placeWall(MAIN_EDGE_WALL, x, z + offset, rotation);
			}
			placer.place(mainEdgeCeilingPiece, x, heightOffset, z + offset, rotation);
		}
	}

	private static void placeCorner(Placer placer, boolean interior, Part mainCornerPiece, Part mainCornerWallPiece, Part mainCornerCeilingPiece, float x, float z, int heightOffset, float rotation) {
		placer.place(mainCornerPiece, x, 0, z, rotation);
		if (interior) {
			placer.placeWall(mainCornerWallPiece, x, z, rotation);
		}
		placer.place(mainCornerCeilingPiece, x, heightOffset, z, rotation);
	}

	private static Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> buildDoor(Part interiorPart, Part exteriorPart, Identifier texture) {
		final Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models = new Object2ObjectOpenHashMap<>();
		for (final RenderStage renderStage : new RenderStage[]{RenderStage.INTERIOR, RenderStage.EXTERIOR}) {
			final MeshDefinition meshDefinition = new MeshDefinition();
			new Placer(meshDefinition.getRoot(), 0).place(renderStage == RenderStage.INTERIOR ? interiorPart : exteriorPart, 0, 0, 0, 0);
			models.put(renderStage, ObjectArrayList.of(createModel(meshDefinition, texture)));
		}
		return models;
	}

	private static NewOptimizedModel createModel(MeshDefinition meshDefinition, Identifier texture) {
		final ObjectArrayList<StoredVertexData> storedVertexDataList = new ObjectArrayList<>();
		StoredVertexData.write(LayerDefinition.create(meshDefinition, TEXTURE_SIZE, TEXTURE_SIZE).bakeRoot(), storedVertexDataList);
		// Undo the flip of StoredVertexData.write, since the lift pieces are already in the model space of vehicles
		storedVertexDataList.replaceAll(storedVertexData -> new StoredVertexData(
			storedVertexData.x(), -storedVertexData.y(), -storedVertexData.z(),
			storedVertexData.color(),
			storedVertexData.u(), storedVertexData.v(),
			storedVertexData.normalX(), -storedVertexData.normalY(), -storedVertexData.normalZ()
		));
		return new NewOptimizedModel(texture, PrimitiveTopology.QUADS, storedVertexDataList);
	}

	private static float[] cube(float textureX, float textureY, float x, float y, float z, float sizeX, float sizeY, float sizeZ) {
		return new float[]{textureX, textureY, x, y, z, sizeX, sizeY, sizeZ};
	}

	@FunctionalInterface
	public interface ModelConsumer {
		/**
		 * @param models         the models keyed by render stage
		 * @param transformation an extra transformation to apply before drawing the models, or {@code null}
		 */
		void accept(Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models, @Nullable Consumer<PoseStack> transformation);
	}

	/**
	 * Cuboids sharing one rotation around the Y axis.
	 */
	private record Group(float rotationY, float[]... cubes) {
	}

	/**
	 * A piece of the lift that can be placed several times.
	 */
	private record Part(Group... groups) {
	}

	private record CarKey(int height, int width, int depth, boolean isDoubleSided, Identifier texture) {
	}

	private static final class Placer {

		private final PartDefinition root;
		private final int heightCount;
		private int partCount;

		private Placer(PartDefinition root, int heightCount) {
			this.root = root;
			this.heightCount = heightCount;
		}

		private void place(Part part, float x, float y, float z, float rotationY) {
			final PartDefinition partDefinition = root.addOrReplaceChild(String.valueOf(partCount++), CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, 0, rotationY, 0));
			for (int i = 0; i < part.groups.length; i++) {
				final Group group = part.groups[i];
				final CubeListBuilder cubeListBuilder = CubeListBuilder.create();
				for (final float[] cube : group.cubes) {
					cubeListBuilder.texOffs((int) cube[0], (int) cube[1]).addBox(cube[2], cube[3], cube[4], cube[5], cube[6], cube[7]);
				}
				partDefinition.addOrReplaceChild(String.valueOf(i), cubeListBuilder, PartPose.rotation(0, group.rotationY, 0));
			}
		}

		/**
		 * Place a wall piece once for every half block above the default height of two blocks.
		 */
		private void placeWall(Part part, float x, float z, float rotationY) {
			for (int i = 0; i < heightCount; i++) {
				place(part, x, -i * 8, z, rotationY);
			}
		}
	}
}
