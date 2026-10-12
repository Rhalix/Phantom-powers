package com.phantompowers.client;

import com.phantompowers.network.EntityFxPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Possessed bodies get the possessor's glowing ghost eyes; frozen enemies are encased in a block of ice.
 */
public final class ClientEntityFx {
	private static final Identifier ICE = Identifier.ofVanilla("textures/block/ice.png");
	private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(1f, 0.15f, 0.1f), 0.6f);
	private static int[] hosts = new int[0];
	private static int[] frozen = new int[0];

	private ClientEntityFx() {}

	public static void update(EntityFxPayload p) {
		hosts = p.hosts();
		frozen = p.frozen();
	}

	public static void clear() {
		hosts = new int[0];
		frozen = new int[0];
	}

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || hosts.length == 0) return;
		// A faint red wisp trails from possessed eyes
		for (int id : hosts) {
			Entity e = mc.world.getEntityById(id);
			if (!(e instanceof LivingEntity le) || mc.world.random.nextInt(3) != 0) continue;
			Vec3d[] eyes = eyePositions(le, 1f);
			Vec3d at = eyes[mc.world.random.nextInt(2)];
			mc.world.addParticle(RED, at.x, at.y, at.z, 0, 0.01, 0);
		}
	}

	public static void render(WorldRenderContext ctx) {
		if (hosts.length == 0 && frozen.length == 0) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (mc.world == null || consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f m = ms.peek().getPositionMatrix();

		if (frozen.length > 0) {
			VertexConsumer ice = consumers.getBuffer(RenderLayer.getEntityTranslucent(ICE));
			for (int id : frozen) {
				Entity e = mc.world.getEntityById(id);
				if (e == null) continue;
				Vec3d pos = e.getLerpedPos(partial);
				Box b = e.getBoundingBox().offset(pos.subtract(e.getPos())).expand(0.18, 0.12, 0.18);
				int light = WorldRenderer.getLightmapCoordinates(mc.world, BlockPos.ofFloored(e.getEyePos()));
				iceBox(ice, ms.peek(), b, light);
			}
		}

		if (hosts.length > 0) {
			VertexConsumer glow = consumers.getBuffer(RenderLayer.getLightning());
			for (int id : hosts) {
				Entity e = mc.world.getEntityById(id);
				if (!(e instanceof LivingEntity le) || (e == mc.player && mc.options.getPerspective().isFirstPerson())) continue;
				Vec3d[] eyes = eyePositions(le, partial);
				Vec3d fwd = facing(le, partial);
				float size = e instanceof PlayerEntity ? 0.055f : MathHelper.clamp(e.getWidth() * 0.07f, 0.04f, 0.12f);
				float pulse = 0.85f + 0.15f * MathHelper.sin((e.age + partial) * 0.4f);
				for (Vec3d eye : eyes) {
					square(glow, m, eye, fwd, size * 2.6f, 255, 40, 30, (int) (90 * pulse));   // halo
					square(glow, m, eye, fwd, size, 255, 70, 60, (int) (255 * pulse));         // eye
					square(glow, m, eye.add(fwd.multiply(0.002)), fwd, size * 0.45f, 255, 230, 220, 255); // hot core
				}
			}
		}
		ms.pop();
	}

	private static Vec3d facing(LivingEntity e, float partial) {
		float yaw = MathHelper.lerpAngleDegrees(partial, e.prevHeadYaw, e.headYaw) * MathHelper.RADIANS_PER_DEGREE;
		float pitch = MathHelper.lerp(partial, e.prevPitch, e.getPitch()) * MathHelper.RADIANS_PER_DEGREE;
		return new Vec3d(-MathHelper.sin(yaw) * MathHelper.cos(pitch), -MathHelper.sin(pitch), MathHelper.cos(yaw) * MathHelper.cos(pitch));
	}

	/** Where the two eyes are: on a player's face exactly, on mobs a good guess from their size and head direction. */
	private static Vec3d[] eyePositions(LivingEntity e, float partial) {
		Vec3d pos = e.getLerpedPos(partial);
		Vec3d fwd = facing(e, partial);
		Vec3d flat = new Vec3d(fwd.x, 0, fwd.z);
		flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : flat.normalize();
		Vec3d side = new Vec3d(-flat.z, 0, flat.x);
		double eyeY;
		double ahead;
		double apart;
		if (e instanceof PlayerEntity) {
			eyeY = e.getHeight() * (1.72 / 1.8);
			ahead = 0.26;
			apart = 0.125;
		} else {
			boolean fourLegs = e.getHeight() < e.getWidth() * 1.25;
			eyeY = e.getStandingEyeHeight() + 0.02;
			ahead = fourLegs ? e.getWidth() * 0.82 : e.getWidth() * 0.5 + 0.02;
			apart = MathHelper.clamp(e.getWidth() * 0.18, 0.08, 0.35);
		}
		Vec3d base = pos.add(0, eyeY, 0).add(fwd.multiply(ahead));
		return new Vec3d[]{base.add(side.multiply(apart)), base.subtract(side.multiply(apart))};
	}

	/** A small glowing square facing along {@code fwd}. */
	private static void square(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d fwd, float s, int r, int g, int b, int a) {
		Vec3d u = fwd.crossProduct(new Vec3d(0, 1, 0));
		if (u.lengthSquared() < 1.0E-4) u = new Vec3d(1, 0, 0);
		u = u.normalize().multiply(s);
		Vec3d v = u.crossProduct(fwd).normalize().multiply(s);
		Vec3d p1 = c.add(u).add(v), p2 = c.subtract(u).add(v), p3 = c.subtract(u).subtract(v), p4 = c.add(u).subtract(v);
		vtx(vc, m, p1, r, g, b, a); vtx(vc, m, p2, r, g, b, a); vtx(vc, m, p3, r, g, b, a); vtx(vc, m, p4, r, g, b, a);
		vtx(vc, m, p4, r, g, b, a); vtx(vc, m, p3, r, g, b, a); vtx(vc, m, p2, r, g, b, a); vtx(vc, m, p1, r, g, b, a);
	}

	private static void vtx(VertexConsumer vc, Matrix4f m, Vec3d p, int r, int g, int b, int a) {
		vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
	}

	/** A translucent block of ice around a box. */
	private static void iceBox(VertexConsumer vc, MatrixStack.Entry entry, Box b, int light) {
		float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ, x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
		float h = y1 - y0;
		// north (-z), south (+z), west (-x), east (+x), up, down
		face(vc, entry, light, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, x1 - x0, h);
		face(vc, entry, light, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, x1 - x0, h);
		face(vc, entry, light, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, z1 - z0, h);
		face(vc, entry, light, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, z1 - z0, h);
		face(vc, entry, light, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, x1 - x0, z1 - z0);
		face(vc, entry, light, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, x1 - x0, z1 - z0);
	}

	private static void face(VertexConsumer vc, MatrixStack.Entry e, int light,
			float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
			float nx, float ny, float nz, float uw, float vh) {
		Matrix4f m = e.getPositionMatrix();
		float u = Math.max(1f, uw), v = Math.max(1f, vh);
		int a = 175;
		vc.vertex(m, ax, ay, az).color(200, 230, 255, a).texture(0, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
		vc.vertex(m, bx, by, bz).color(200, 230, 255, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
		vc.vertex(m, cx, cy, cz).color(200, 230, 255, a).texture(u, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
		vc.vertex(m, dx, dy, dz).color(200, 230, 255, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
	}
}
