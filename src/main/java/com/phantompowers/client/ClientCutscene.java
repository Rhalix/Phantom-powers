package com.phantompowers.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;

/** Cutscenes (hold V to transform, hold U to suit up, or hold the wail too long): camera turns to face you, movie bars, a shout. */
public final class ClientCutscene {
	private static boolean active;
	private static Perspective previous;
	private static int shown;
	private static net.minecraft.util.math.Vec3d anchor;
	private static float baseYaw;

	private ClientCutscene() {}

	public static void tick(MinecraftClient mc) {
		boolean running = ClientGhost.cutscene > 0 && mc.player != null;
		GameOptions o = mc.options;
		if (running && !active) {
			active = true;
			shown = 0;
			previous = o.getPerspective();
			o.setPerspective(Perspective.THIRD_PERSON_BACK);
			anchor = mc.player.getPos();
			baseYaw = mc.player.getBodyYaw();
		}
		if (running) {
			shown++;
			holdStill(mc);
			o.forwardKey.setPressed(false);
			o.backKey.setPressed(false);
			o.leftKey.setPressed(false);
			o.rightKey.setPressed(false);
			o.jumpKey.setPressed(false);
			o.sneakKey.setPressed(false);
		} else if (active) {
			reset(mc);
		}
	}

	public static void reset(MinecraftClient mc) {
		if (active && previous != null && mc.options != null) mc.options.setPerspective(previous);
		active = false;
		previous = null;
		anchor = null;
	}

	/** Float smoothly in place on our side (no rubber-banding from the server). */
	private static void holdStill(MinecraftClient mc) {
		if (anchor == null) return;
		int total = ClientGhost.cutsceneKind == 2 ? com.phantompowers.GhostPowers.WAIL_FINALE_TICKS : com.phantompowers.GhostPowers.CUTSCENE_TICKS;
		int elapsed = Math.max(0, total - ClientGhost.cutscene);
		double lift;
		if (ClientGhost.cutsceneKind == 2) {
			lift = ClientGhost.cutscene > 30 ? Math.min(1.2, elapsed * 0.04) : 1.2 * ClientGhost.cutscene / 30.0;
		} else {
			lift = ClientGhost.cutsceneKind == 0 && ClientGhost.cutsceneToGhost ? Math.min(0.6, elapsed * 0.015) : 0;
		}
		var p = mc.player;
		var want = anchor.add(0, lift, 0);
		var now = p.getPos();
		var next = now.lerp(want, 0.35);
		p.setVelocity(0, 0, 0);
		p.setPosition(next.x, next.y, next.z);
		p.fallDistance = 0;
	}

	/**
	 * Where the cutscene camera sits: in front of you and a little to the side, slowly circling,
	 * framing your whole body. Pulled in if a wall is in the way. Returns {x, y, z, yaw, pitch} or null.
	 */
	public static float[] cameraShot(net.minecraft.entity.Entity e, float partial) {
		if (!active || e == null) return null;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (e != mc.player) return null;
		float t = shown + partial;
		boolean wail = ClientGhost.cutsceneKind == 2;
		// Start facing you, then drift around to a three-quarter angle
		double orbit = Math.toRadians(baseYaw + (wail ? 25 : 35) * Math.sin(t * 0.035) - 20);
		double dist = wail ? 5.2 : 3.6 + 0.4 * Math.min(1f, t / 30f);
		var pos = e.getLerpedPos(partial);
		var target = pos.add(0, e.getHeight() * 0.55, 0);
		double camY = target.y + (wail ? 0.6 : 0.15);
		var from = new net.minecraft.util.math.Vec3d(target.x - Math.sin(orbit) * dist, camY, target.z + Math.cos(orbit) * dist);
		// Don't put the camera inside a wall
		var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(target, from,
				net.minecraft.world.RaycastContext.ShapeType.VISUAL, net.minecraft.world.RaycastContext.FluidHandling.NONE, e));
		if (hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
			var back = from.subtract(target).normalize();
			from = hit.getPos().subtract(back.multiply(0.3));
		}
		var d = target.subtract(from);
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new float[]{(float) from.x, (float) from.y, (float) from.z, yaw, pitch};
	}

	public static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
		if (!active) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();
		float slide = Math.min(1f, (shown + tickCounter.getTickDelta(true)) / 6f);
		int bar = (int) (h * 0.08f * slide);
		ctx.fill(0, 0, w, bar, 0xFF000000);
		ctx.fill(0, h - bar, w, h, 0xFF000000);

		if (ClientGhost.cutsceneKind == 2) {
			// Wail finale: the screen pulses green while you scream, then fades as you drain
			boolean screaming = ClientGhost.cutscene > 30;
			if (screaming) {
				int a = (int) (40 + 40 * Math.abs(Math.sin((shown + tickCounter.getTickDelta(true)) * 0.6)));
				ctx.fill(0, bar, w, h - bar, (a << 24) | 0x33FF77);
			} else {
				int a = (int) (120 * (1 - ClientGhost.cutscene / 30f));
				ctx.fill(0, bar, w, h - bar, (Math.max(0, a) << 24));
			}
			String line = screaming ? "AAAAAAAAAAHHHHHH!" : "...completely drained...";
			int color = screaming ? 0xFF66FF99 : 0xFFBBBBBB;
			float jitter = screaming ? (float) Math.sin(shown * 3.1) * 2f : 0f;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f + jitter, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(screaming ? 2.4f : 1.6f, screaming ? 2.4f : 1.6f, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
			return;
		}
		if (shown > 6) {
			String line;
			int color;
			if (ClientGhost.cutsceneKind == 1) {
				line = ClientGhost.cutsceneToGhost ? "SUIT UP!" : "Suit down...";
				color = ClientGhost.cutsceneToGhost ? 0xFF66E8FF : 0xFFDDDDDD;
			} else {
				line = ClientGhost.cutsceneToGhost ? "I'M GOING GHOST!" : "Going human...";
				color = ClientGhost.cutsceneToGhost ? 0xFFFFFFFF : 0xFFDDDDDD;
			}
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(2f, 2f, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
		}
	}
}
