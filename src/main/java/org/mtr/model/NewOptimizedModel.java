package org.mtr.model;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * One drawable mesh — a single texture and a single GPU vertex buffer per vertex format.
 *
 * <p>Instances are created during model build (see
 * {@link NewOptimizedModelGroup#build(PrimitiveTopology)}) and live for the rest of
 * the parent loader's lifetime. The vertices are uploaded to the GPU the first time the mesh is drawn;
 * subsequent frames only issue one draw call per instance with its own transformation and brightness.</p>
 *
 * <p>The GPU buffers are released on the render thread once the mesh is no longer referenced, see
 * {@link #releaseUnusedBuffers()}.</p>
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
	 * Uploaded meshes keyed by vertex format, since different render stages use render types with different formats.
	 * Shared with the cleaner action, so it must not reference this object.
	 */
	private final Object2ObjectOpenHashMap<VertexFormat, UploadedMesh> uploadedMeshes = new Object2ObjectOpenHashMap<>();

	private static final Cleaner CLEANER = Cleaner.create();
	private static final ConcurrentLinkedQueue<UploadedMesh> UNUSED_MESHES = new ConcurrentLinkedQueue<>();

	/**
	 * @param texture              the texture this mesh draws with
	 * @param drawMode             the primitive type ({@code TRIANGLES} for OBJ, {@code QUADS} for Blockbench)
	 * @param storedVertexDataList the vertices of this mesh, or {@code null} to skip rendering entirely
	 */
	public NewOptimizedModel(Identifier texture, PrimitiveTopology drawMode, @Nullable ObjectArrayList<StoredVertexData> storedVertexDataList) {
		this.texture = texture;
		this.drawMode = drawMode;
		this.storedVertexDataList = storedVertexDataList;
		final Object2ObjectOpenHashMap<VertexFormat, UploadedMesh> meshes = uploadedMeshes;
		CLEANER.register(this, () -> {
			synchronized (meshes) {
				UNUSED_MESHES.addAll(meshes.values());
				meshes.clear();
			}
		});
	}

	/**
	 * Draw this mesh once per instance. Must be called on the render thread while the camera's model view matrix is active.
	 *
	 * @param renderType the render type providing the pipeline, textures and output target
	 * @param instances  called with a consumer accepting the model view matrix and the brightness in {@code [0, 1]} of each instance
	 */
	public void draw(RenderType renderType, Consumer<InstanceConsumer> instances) {
		final UploadedMesh uploadedMesh = getUploadedMesh(renderType.format());
		if (uploadedMesh == null) {
			return;
		}

		final PreparedRenderType preparedRenderType = renderType.prepare();
		final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		final GpuBuffer indexBuffer = indices.getBuffer(uploadedMesh.indexCount);

		instances.accept((modelViewMatrix, brightness) -> new PreparedRenderType(
			preparedRenderType.pipeline(),
			preparedRenderType.outputTarget(),
			RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, new Vector4f(brightness, brightness, brightness, 1)),
			preparedRenderType.scissorState(),
			preparedRenderType.textures()
		).drawFromBuffer(uploadedMesh.vertexBuffer, indexBuffer, indices.type(), 0, 0, uploadedMesh.indexCount));
	}

	@Nullable
	private UploadedMesh getUploadedMesh(VertexFormat vertexFormat) {
		if (storedVertexDataList == null || storedVertexDataList.isEmpty()) {
			return null;
		}

		synchronized (uploadedMeshes) {
			final UploadedMesh existingMesh = uploadedMeshes.get(vertexFormat);
			if (existingMesh != null) {
				return existingMesh;
			}

			try (final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * storedVertexDataList.size() * 2)) {
				final BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, vertexFormat);
				writeVertices(bufferBuilder);
				try (final MeshData meshData = bufferBuilder.build()) {
					if (meshData == null) {
						return null;
					}
					final UploadedMesh uploadedMesh = new UploadedMesh(
						RenderSystem.getDevice().createBuffer(() -> "MTR model " + texture, GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer()),
						meshData.drawState().indexCount()
					);
					uploadedMeshes.put(vertexFormat, uploadedMesh);
					return uploadedMesh;
				}
			}
		}
	}

	/**
	 * Visit the vertices of this mesh as quads, for example to draw it outside the world in a GUI preview.
	 * Triangles are visited as quads with a repeated last vertex.
	 */
	public void forEachQuadVertex(Consumer<StoredVertexData> consumer) {
		if (storedVertexDataList == null) {
			return;
		}

		if (drawMode == PrimitiveTopology.TRIANGLES) {
			for (int i = 0; i + 2 < storedVertexDataList.size(); i += 3) {
				consumer.accept(storedVertexDataList.get(i));
				consumer.accept(storedVertexDataList.get(i + 1));
				consumer.accept(storedVertexDataList.get(i + 2));
				consumer.accept(storedVertexDataList.get(i + 2));
			}
		} else {
			storedVertexDataList.forEach(consumer);
		}
	}

	/**
	 * Brightness is applied through the colour modulator at draw time, so the vertices are full-bright.
	 */
	private void writeVertices(VertexConsumer vertexConsumer) {
		forEachQuadVertex(storedVertexData -> writeVertex(vertexConsumer, storedVertexData));
	}

	/**
	 * Release the GPU buffers of meshes that were garbage collected. Must be called on the render thread.
	 */
	public static void releaseUnusedBuffers() {
		UploadedMesh uploadedMesh;
		while ((uploadedMesh = UNUSED_MESHES.poll()) != null) {
			uploadedMesh.vertexBuffer.close();
		}
	}

	private static void writeVertex(VertexConsumer vertexConsumer, StoredVertexData storedVertexData) {
		vertexConsumer.addVertex(storedVertexData.x(), storedVertexData.y(), storedVertexData.z())
			.setColor(storedVertexData.color().getRGB())
			.setUv(storedVertexData.u(), storedVertexData.v())
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(LightCoordsUtil.FULL_BRIGHT)
			.setNormal(storedVertexData.normalX(), storedVertexData.normalY(), storedVertexData.normalZ());
	}

	@FunctionalInterface
	public interface InstanceConsumer {
		/**
		 * @param modelViewMatrix the camera's model view matrix multiplied with the instance transformation
		 * @param brightness      brightness scale in {@code [0, 1]}; {@code 1} for full-bright stages
		 */
		void accept(Matrix4f modelViewMatrix, float brightness);
	}

	private record UploadedMesh(GpuBuffer vertexBuffer, int indexCount) {
	}
}
