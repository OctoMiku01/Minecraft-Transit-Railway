package org.mtr.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4fc;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Records vertices so that they can be written into a real buffer later.
 * Attributes that were never set get defaults (white, full brightness, facing up), so the recorded vertices fit any vertex format.
 */
public final class RecordingVertexConsumer implements VertexConsumer {

	private final ObjectArrayList<Vertex> vertices = new ObjectArrayList<>();
	private Vertex currentVertex = new Vertex(0, 0, 0);

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		currentVertex = new Vertex(x, y, z);
		vertices.add(currentVertex);
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		currentVertex.color = ARGB.color(a, r, g, b);
		return this;
	}

	@Override
	public VertexConsumer setColor(int color) {
		currentVertex.color = color;
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		currentVertex.u = u;
		currentVertex.v = v;
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		currentVertex.light = u & 0xFFFF | v << 16;
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		currentVertex.normalX = x;
		currentVertex.normalY = y;
		currentVertex.normalZ = z;
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		return this;
	}

	public boolean isEmpty() {
		return vertices.isEmpty();
	}

	/**
	 * @return the 2D bounding box of all recorded vertices as {@code {minX, minY, maxX, maxY}}
	 */
	public float[] getBounds2D() {
		final float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		vertices.forEach(vertex -> {
			bounds[0] = Math.min(bounds[0], vertex.x);
			bounds[1] = Math.min(bounds[1], vertex.y);
			bounds[2] = Math.max(bounds[2], vertex.x);
			bounds[3] = Math.max(bounds[3], vertex.y);
		});
		return bounds;
	}

	/**
	 * Write every recorded vertex into the given buffer. Elements missing from the buffer's vertex format are skipped by the buffer.
	 */
	public void replay(VertexConsumer vertexConsumer) {
		vertices.forEach(vertex -> vertexConsumer.addVertex(vertex.x, vertex.y, vertex.z)
			.setColor(vertex.color)
			.setUv(vertex.u, vertex.v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(vertex.light)
			.setNormal(vertex.normalX, vertex.normalY, vertex.normalZ)
		);
	}

	/**
	 * Write every recorded vertex into the given buffer, transforming positions and scaling the RGB channels of the colours.
	 */
	public void replay(VertexConsumer vertexConsumer, Matrix4fc transform, float brightness) {
		vertices.forEach(vertex -> vertexConsumer.addVertex(transform, vertex.x, vertex.y, vertex.z)
			.setColor(ARGB.scaleRGB(vertex.color, brightness))
			.setUv(vertex.u, vertex.v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(vertex.light)
			.setNormal(vertex.normalX, vertex.normalY, vertex.normalZ)
		);
	}

	private static final class Vertex {

		private final float x, y, z;
		private int color = 0xFFFFFFFF;
		private float u, v;
		private int light = LightCoordsUtil.FULL_BRIGHT;
		private float normalX, normalY = 1, normalZ;

		private Vertex(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
