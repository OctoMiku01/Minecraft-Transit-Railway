package org.mtr.model;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * One drawable mesh — a single texture and a single list of vertices.
 *
 * <p>Instances are created during model build (see
 * {@link NewOptimizedModelGroup#build(PrimitiveTopology)}) and live for the rest of
 * the parent loader's lifetime. Every frame, {@link #render} writes the stored vertices into the
 * vertex consumer handed out by the submit node collector.</p>
 *
 * <p>TODO 26.2 port: older versions uploaded the mesh to the GPU once and only re-issued the draw call.
 * Minecraft 26.2 replaced that rendering layer, so the vertices are currently re-submitted every frame.
 * Restore the GPU-resident path with the new GPU buffer API once the port works in game.</p>
 *
 * <p><b>Naming note:</b> the "New" prefix is historical — there is no non-{@code New}
 * counterpart any more. See {@code docs/MIGRATIONS.md} §4 for the planned rename.</p>
 */
public final class NewOptimizedModel {

	public final Identifier texture;
	@Nullable
	private final ObjectArrayList<StoredVertexData> storedVertexDataList;
	private final PrimitiveTopology drawMode;

	/**
	 * @param texture              the texture this mesh draws with
	 * @param drawMode             the primitive type ({@code TRIANGLES} for OBJ, {@code QUADS} for Blockbench)
	 * @param storedVertexDataList the vertices of this mesh, or {@code null} to skip rendering entirely
	 */
	public NewOptimizedModel(Identifier texture, PrimitiveTopology drawMode, @Nullable ObjectArrayList<StoredVertexData> storedVertexDataList) {
		this.texture = texture;
		this.drawMode = drawMode;
		this.storedVertexDataList = storedVertexDataList;
	}

	/**
	 * Write this mesh into a quad-based vertex consumer. Triangles are written as quads with a repeated last vertex.
	 *
	 * @param vertexConsumer the vertex consumer of the render type matching this mesh
	 * @param pose           the per-instance transformation
	 * @param light          the packed lightmap coordinates
	 */
	public void render(VertexConsumer vertexConsumer, PoseStack.Pose pose, int light) {
		if (storedVertexDataList == null) {
			return;
		}

		if (drawMode == PrimitiveTopology.TRIANGLES) {
			for (int i = 0; i + 2 < storedVertexDataList.size(); i += 3) {
				writeVertex(vertexConsumer, pose, storedVertexDataList.get(i), light);
				writeVertex(vertexConsumer, pose, storedVertexDataList.get(i + 1), light);
				writeVertex(vertexConsumer, pose, storedVertexDataList.get(i + 2), light);
				writeVertex(vertexConsumer, pose, storedVertexDataList.get(i + 2), light);
			}
		} else {
			storedVertexDataList.forEach(storedVertexData -> writeVertex(vertexConsumer, pose, storedVertexData, light));
		}
	}

	private static void writeVertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, StoredVertexData storedVertexData, int light) {
		vertexConsumer.addVertex(pose, storedVertexData.x(), storedVertexData.y(), storedVertexData.z())
			.setColor(storedVertexData.color().getRGB())
			.setUv(storedVertexData.u(), storedVertexData.v())
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, storedVertexData.normalX(), storedVertexData.normalY(), storedVertexData.normalZ());
	}
}
