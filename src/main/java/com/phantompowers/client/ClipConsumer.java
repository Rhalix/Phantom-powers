package com.phantompowers.client;

import net.minecraft.client.render.VertexConsumer;

/**
 * Wraps a vertex consumer and cuts every quad down to the part between two heights (camera-relative).
 * Used to draw the new look only where the transformation rings have already passed.
 */
public final class ClipConsumer implements VertexConsumer {
	private final VertexConsumer out;
	private final float lo, hi;
	// pending vertex
	private float x, y, z, u, v, nx, ny, nz;
	private int color = -1, overlay, light;
	// collected quad: x y z u v nx ny nz
	private final float[][] quad = new float[4][8];
	private final int[] qColor = new int[4], qOverlay = new int[4], qLight = new int[4];
	private int n;

	public ClipConsumer(VertexConsumer out, double lo, double hi) {
		this.out = out;
		this.lo = (float) lo;
		this.hi = (float) hi;
	}

	@Override
	public VertexConsumer vertex(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		this.color = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
		return this;
	}

	@Override
	public VertexConsumer color(int argb) {
		this.color = argb;
		return this;
	}

	@Override
	public VertexConsumer texture(float u, float v) {
		this.u = u;
		this.v = v;
		return this;
	}

	@Override
	public VertexConsumer overlay(int u, int v) {
		this.overlay = (u & 0xFFFF) | (v & 0xFFFF) << 16;
		return this;
	}

	@Override
	public VertexConsumer overlay(int uv) {
		this.overlay = uv;
		return this;
	}

	@Override
	public VertexConsumer light(int u, int v) {
		this.light = (u & 0xFFFF) | (v & 0xFFFF) << 16;
		return this;
	}

	@Override
	public VertexConsumer light(int uv) {
		this.light = uv;
		return this;
	}

	/** The normal is the last element of every entity vertex, so it completes the vertex. */
	@Override
	public VertexConsumer normal(float nx, float ny, float nz) {
		this.nx = nx;
		this.ny = ny;
		this.nz = nz;
		float[] q = quad[n];
		q[0] = x; q[1] = y; q[2] = z; q[3] = u; q[4] = v; q[5] = nx; q[6] = ny; q[7] = nz;
		qColor[n] = color;
		qOverlay[n] = overlay;
		qLight[n] = light;
		if (++n == 4) {
			n = 0;
			emit();
		}
		return this;
	}

	@Override
	public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
		vertex(x, y, z);
		color(color);
		texture(u, v);
		overlay(overlay);
		light(light);
		normal(nx, ny, nz);
	}

	private void emit() {
		boolean allIn = true, allOut = true;
		for (int i = 0; i < 4; i++) {
			float yy = quad[i][1];
			boolean in = yy >= lo && yy <= hi;
			allIn &= in;
			if (in) allOut = false;
		}
		if (allIn) {
			for (int i = 0; i < 4; i++) put(quad[i]);
			return;
		}
		// Quads that straddle (or span) the band get clipped
		float[][] poly = new float[4][];
		for (int i = 0; i < 4; i++) poly[i] = quad[i];
		poly = clip(poly, 4, lo, true);
		if (poly.length < 3) return;
		poly = clip(poly, poly.length, hi, false);
		if (poly.length < 3) return;
		if (allOut && poly.length == 0) return;
		// Emit as quads (fan): 3 -> 1 quad (repeat last), 4 -> 1, 5/6 -> 2
		int k = poly.length;
		putQuad(poly[0], poly[1], poly[2], k > 3 ? poly[3] : poly[2]);
		if (k > 4) putQuad(poly[0], poly[3], poly[4], k > 5 ? poly[5] : poly[4]);
	}

	/** Sutherland-Hodgman against one horizontal plane: keep y >= limit (above) or y <= limit. */
	private static float[][] clip(float[][] in, int count, float limit, boolean keepAbove) {
		float[][] res = new float[count + 2][];
		int m = 0;
		for (int i = 0; i < count; i++) {
			float[] a = in[i], b = in[(i + 1) % count];
			boolean ain = keepAbove ? a[1] >= limit : a[1] <= limit;
			boolean bin = keepAbove ? b[1] >= limit : b[1] <= limit;
			if (ain) res[m++] = a;
			if (ain != bin) {
				float t = (limit - a[1]) / (b[1] - a[1]);
				float[] c = new float[8];
				for (int j = 0; j < 8; j++) c[j] = a[j] + (b[j] - a[j]) * t;
				res[m++] = c;
			}
		}
		float[][] trimmed = new float[m][];
		System.arraycopy(res, 0, trimmed, 0, m);
		return trimmed;
	}

	private void putQuad(float[] a, float[] b, float[] c, float[] d) {
		put(a);
		put(b);
		put(c);
		put(d);
	}

	private void put(float[] q) {
		out.vertex(q[0], q[1], q[2], qColor[0], q[3], q[4], qOverlay[0], qLight[0], q[5], q[6], q[7]);
	}
}
