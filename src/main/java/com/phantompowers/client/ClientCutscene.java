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
			int k = ClientGhost.cutsceneKind;
			baseYaw = k == 3 || k == 4 || k == 6 ? mc.player.getYaw() : mc.player.getBodyYaw();
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
		if (ClientGhost.cutsceneKind == 3) {
			// the prototype portal: lean in, then get blown backwards with a little hop
			int el = Math.max(0, com.phantompowers.DarkPowers.PORTAL_TICKS - ClientGhost.cutscene);
			var back = net.minecraft.util.math.Vec3d.fromPolar(0, baseYaw).multiply(-1);
			var want = anchor.add(back.multiply(com.phantompowers.DarkPowers.portalKnock(el))).add(0, com.phantompowers.DarkPowers.portalLift(el), 0);
			var p = mc.player;
			var next = p.getPos().lerp(want, 0.7);
			p.setVelocity(0, 0, 0);
			p.setPosition(next.x, next.y, next.z);
			p.setYaw(baseYaw);
			p.fallDistance = 0;
			return;
		}
		var pl = mc.player;
		var look0 = net.minecraft.util.math.Vec3d.fromPolar(0, baseYaw);
		if (ClientGhost.cutsceneKind == 4) {
			// Dark Tornado: fly round and round the spot in front of you
			int el = Math.max(0, com.phantompowers.DarkPowers.SPIN_TICKS - ClientGhost.cutscene);
			var center = anchor.add(look0.multiply(com.phantompowers.DarkPowers.SPIN_RADIUS));
			var want = com.phantompowers.DarkPowers.spinPos(center, el);
			var next = pl.getPos().lerp(want, 0.6);
			pl.setVelocity(0, 0, 0);
			pl.setPosition(next.x, next.y, next.z);
			// face along the circle
			var tangent = com.phantompowers.DarkPowers.spinPos(center, el + 1).subtract(want);
			float face = (float) Math.toDegrees(Math.atan2(-tangent.x, tangent.z));
			pl.setYaw(face);
			pl.setBodyYaw(face);
			pl.setHeadYaw(face);
			pl.fallDistance = 0;
			return;
		}
		int kk = ClientGhost.cutsceneKind;
		if (kk == 9 || kk == 10 || kk == 11) {
			int total = kk == 9 ? com.phantompowers.Cutscenes.HERO_OFF_TICKS : kk == 10 ? com.phantompowers.Cutscenes.DARK_OFF_TICKS : com.phantompowers.Cutscenes.SUIT_OFF_TICKS;
			int el = Math.max(0, total - ClientGhost.cutscene);
			double lift = kk == 9 ? com.phantompowers.Cutscenes.heroOffLift(el) : kk == 10 ? com.phantompowers.Cutscenes.darkOffLift(el) : 0;
			var next = pl.getPos().lerp(anchor.add(0, lift, 0), 0.5);
			pl.setVelocity(0, 0, 0);
			pl.setPosition(next.x, next.y, next.z);
			pl.fallDistance = 0;
			return;
		}
		if (ClientGhost.cutsceneKind == 7 || ClientGhost.cutsceneKind == 8) {
			boolean hero = ClientGhost.cutsceneKind == 7;
			int el = Math.max(0, (hero ? com.phantompowers.Cutscenes.HERO_TICKS : com.phantompowers.Cutscenes.SUIT_TICKS) - ClientGhost.cutscene);
			var want = hero ? anchor.add(0, com.phantompowers.Cutscenes.heroLift(el), 0) : anchor;
			var next = pl.getPos().lerp(want, 0.5);
			pl.setVelocity(0, 0, 0);
			pl.setPosition(next.x, next.y, next.z);
			pl.fallDistance = 0;
			return;
		}
		if (ClientGhost.cutsceneKind == 5 || ClientGhost.cutsceneKind == 6) {
			boolean dark = ClientGhost.cutsceneKind == 5;
			int total = dark ? com.phantompowers.Cutscenes.DARK_TICKS : com.phantompowers.Cutscenes.ORIGIN_TICKS;
			int el = Math.max(0, total - ClientGhost.cutscene);
			if (!dark && el >= com.phantompowers.Cutscenes.ORIGIN_BLAST) {   // blasted out of the portal: let the launch carry you
				pl.fallDistance = 0;
				return;
			}
			var want = dark ? anchor.add(0, com.phantompowers.Cutscenes.darkLift(el), 0)
					: anchor.add(look0.multiply(com.phantompowers.Cutscenes.originForward(el))).add(0, com.phantompowers.Cutscenes.originLift(el), 0);
			var next = pl.getPos().lerp(want, 0.5);
			pl.setVelocity(0, 0, 0);
			pl.setPosition(next.x, next.y, next.z);
			pl.fallDistance = 0;
			return;
		}
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
		if (ClientGhost.cutsceneKind == 3) return portalShot(mc, e, t, partial);
		if (ClientGhost.cutsceneKind == 4) return spinShot(mc, e, t, partial);
		if (ClientGhost.cutsceneKind == 5) return darkShot(mc, e, t, partial);
		if (ClientGhost.cutsceneKind == 6) return originShot(mc, e, t, partial);
		if (ClientGhost.cutsceneKind == 7) return heroShot(mc, e, t, partial);
		if (ClientGhost.cutsceneKind == 8) return suitShot(mc, e, t, partial);
		if (ClientGhost.cutsceneKind == 9) return offShot(mc, e, t, partial, 160, 3.2, 0.9, 1.4);
		if (ClientGhost.cutsceneKind == 10) return offShot(mc, e, t, partial, 200, 2.4, 0.0, 2.2);
		if (ClientGhost.cutsceneKind == 11) return offShot(mc, e, t, partial, 120, 2.8, 1.0, 0.9);
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

	private static float[] aim(MinecraftClient mc, net.minecraft.entity.Entity e, net.minecraft.util.math.Vec3d from, net.minecraft.util.math.Vec3d target) {
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

	/** Dark Tornado: a high wide shot of you circling and the twister rising in the middle. */
	private static float[] spinShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial) {
		var look = net.minecraft.util.math.Vec3d.fromPolar(0, baseYaw);
		var center = anchor.add(look.multiply(com.phantompowers.DarkPowers.SPIN_RADIUS));
		double back = 9 + t * 0.03;
		var from = center.add(look.multiply(-back)).add(0, 4.5 + t * 0.02, 0);
		var target = center.add(0, 2.2 + t * 0.03, 0);
		return aim(mc, e, from, target);
	}

	/** The evil transformation: a low-angle shot slowly orbiting up and around you (pure aura farming). */
	private static float[] darkShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial) {
		var pos = e.getLerpedPos(partial);
		double ang = Math.toRadians(baseYaw + 180 - 50 + t * 1.1);
		double dist = 4.4 - 1.4 * smooth(Math.min(1f, t / 70f));
		double low = -0.4 + 0.9 * smooth(Math.min(1f, t / 80f));
		var target = pos.add(0, 1.1, 0);
		var from = new net.minecraft.util.math.Vec3d(target.x - Math.sin(ang) * dist, pos.y + 0.3 + low, target.z + Math.cos(ang) * dist);
		float shake = t > com.phantompowers.Cutscenes.DARK_CHANGE && t < com.phantompowers.Cutscenes.DARK_CHANGE + 10 ? 0.15f : 0f;
		from = from.add(Math.sin(t * 3.7) * shake, Math.cos(t * 4.1) * shake, 0);
		return aim(mc, e, from, target);
	}

	/** The origin story: looking into the portal at you as it surges, then you get blown out toward the camera. */
	private static float[] originShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial) {
		var look = net.minecraft.util.math.Vec3d.fromPolar(0, baseYaw);
		var side = new net.minecraft.util.math.Vec3d(-look.z, 0, look.x);
		var pos = e.getLerpedPos(partial);
		var target = pos.add(0, 1.2, 0);
		float blast = com.phantompowers.Cutscenes.ORIGIN_BLAST;
		double dist = 5.2 - 1.6 * smooth(Math.min(1f, t / blast));
		var front = anchor.add(look.multiply(dist)).add(side.multiply(0.8)).add(0, 1.3, 0);
		float later = smooth(clamp01((t - blast - 8) / 20f));
		var side2 = anchor.add(look.multiply(4.0)).add(side.multiply(3.5)).add(0, 1.6, 0);
		var from = front.lerp(side2, later);
		float shake = t > blast - 30 && t < blast + 12 ? (t < blast ? 0.05f : 0.2f * Math.max(0f, 1f - (t - blast) / 12f)) : 0f;
		from = from.add(Math.sin(t * 3.7) * shake, Math.cos(t * 4.3) * shake, Math.sin(t * 5.1) * shake);
		return aim(mc, e, from, target);
	}

	/** The hero's transformation: starts low at your feet looking up, slowly rises and circles round to face you. */
	private static float[] heroShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial) {
		var pos = e.getLerpedPos(partial);
		float change = com.phantompowers.Cutscenes.HERO_CHANGE;
		double ang = Math.toRadians(baseYaw + 140 - 140 * smooth(Math.min(1f, t / change)) + 18 * Math.sin(t * 0.03));
		double dist = 4.2 - 1.2 * smooth(Math.min(1f, t / change)) + 1.2 * smooth(clamp01((t - change) / 10f));
		double camY = pos.y + 0.2 + 1.3 * smooth(Math.min(1f, t / change));
		var target = pos.add(0, 1.1, 0);
		var from = new net.minecraft.util.math.Vec3d(target.x - Math.sin(ang) * dist, camY, target.z + Math.cos(ang) * dist);
		float shake = t > change && t < change + 8 ? 0.1f : 0f;
		from = from.add(Math.sin(t * 3.7) * shake, Math.cos(t * 4.1) * shake, 0);
		return aim(mc, e, from, target);
	}

	/** Suiting up: behind you watching the pack come alive, then sweeping round to the front as the plates lock on. */
	private static float[] suitShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial) {
		var pos = e.getLerpedPos(partial);
		float plates = com.phantompowers.Cutscenes.SUIT_PLATES, done = com.phantompowers.Cutscenes.SUIT_DONE;
		float k = smooth(clamp01((t - plates) / (done - plates)));
		double ang = Math.toRadians(baseYaw + 180 * k);        // 0 = behind you, 180 = in front
		double dist = 2.6 + 0.9 * k;
		var target = pos.add(0, 1.2 - 0.3 * (1 - k), 0);
		double camY = pos.y + 1.5 - 0.3 * k + 0.4 * smooth(clamp01((t - done) / 10f));
		var from = new net.minecraft.util.math.Vec3d(target.x + Math.sin(ang) * dist, camY, target.z - Math.cos(ang) * dist);
		return aim(mc, e, from, target);
	}

	/** Changing back: a slow arc from one side round to the front, easing out as you settle. */
	private static float[] offShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial, double startDeg, double dist, double height, double pullOut) {
		var pos = e.getLerpedPos(partial);
		float k = smooth(Math.min(1f, t / 60f));
		double ang = Math.toRadians(baseYaw + startDeg * (1 - k) + 25 * Math.sin(t * 0.02));
		double d = dist + pullOut * k;
		var target = pos.add(0, 1.15, 0);
		var from = new net.minecraft.util.math.Vec3d(target.x - Math.sin(ang) * d, pos.y + 0.6 + height * k + (ClientGhost.cutsceneKind == 10 ? 0.2 : 0.4), target.z + Math.cos(ang) * d);
		if (ClientGhost.cutsceneKind == 10) {
			float el = t;
			float sh = el > com.phantompowers.Cutscenes.DARK_OFF_CHANGE - 4 && el < com.phantompowers.Cutscenes.DARK_OFF_CHANGE + 8 ? 0.14f : 0.02f;
			from = from.add(Math.sin(t * 3.7) * sh, Math.cos(t * 4.3) * sh, Math.sin(t * 5.1) * sh);
		}
		return aim(mc, e, from, target);
	}

	public static float offTime(net.minecraft.entity.Entity e, float partial, int kind) {
		if (!active || ClientGhost.cutsceneKind != kind || e != MinecraftClient.getInstance().player) return -1f;
		int total = kind == 9 ? com.phantompowers.Cutscenes.HERO_OFF_TICKS : kind == 10 ? com.phantompowers.Cutscenes.DARK_OFF_TICKS : com.phantompowers.Cutscenes.SUIT_OFF_TICKS;
		return total - ClientGhost.cutscene + partial;
	}

	public static float heroTime(net.minecraft.entity.Entity e, float partial) {
		if (!active || ClientGhost.cutsceneKind != 7 || e != MinecraftClient.getInstance().player) return -1f;
		return com.phantompowers.Cutscenes.HERO_TICKS - ClientGhost.cutscene + partial;
	}

	public static float suitTime(net.minecraft.entity.Entity e, float partial) {
		if (!active || ClientGhost.cutsceneKind != 8 || e != MinecraftClient.getInstance().player) return -1f;
		return com.phantompowers.Cutscenes.SUIT_TICKS - ClientGhost.cutscene + partial;
	}

	/** Elapsed ticks of the evil transformation / origin cutscene for this player (yourself only), or -1. */
	public static float darkTime(net.minecraft.entity.Entity e, float partial) {
		if (!active || ClientGhost.cutsceneKind != 5 || e != MinecraftClient.getInstance().player) return -1f;
		return com.phantompowers.Cutscenes.DARK_TICKS - ClientGhost.cutscene + partial;
	}

	public static float originTime(net.minecraft.entity.Entity e, float partial) {
		if (!active || ClientGhost.cutsceneKind != 6 || e != MinecraftClient.getInstance().player) return -1f;
		return com.phantompowers.Cutscenes.ORIGIN_TICKS - ClientGhost.cutscene + partial;
	}

	public static boolean spinning(net.minecraft.entity.Entity e) {
		return active && ClientGhost.cutsceneKind == 4 && e == MinecraftClient.getInstance().player;
	}

	/** How far into the prototype portal cutscene this player is (only known for yourself), or -1. */
	public static float portalTime(net.minecraft.entity.Entity e, float partial) {
		if (!active || ClientGhost.cutsceneKind != 3 || e != MinecraftClient.getInstance().player) return -1f;
		return com.phantompowers.DarkPowers.PORTAL_TICKS - ClientGhost.cutscene + partial;
	}

	/**
	 * Portal cutscene camera: a side-on shot of you and the humming portal, slowly pushing in; a tight shot of your face
	 * just before it fires; then a shaking wide shot as it blasts you backwards.
	 */
	private static float[] portalShot(MinecraftClient mc, net.minecraft.entity.Entity e, float t, float partial) {
		var look = net.minecraft.util.math.Vec3d.fromPolar(0, baseYaw);
		var side = new net.minecraft.util.math.Vec3d(-look.z, 0, look.x);
		var pos = e.getLerpedPos(partial);
		var head = pos.add(0, e.getStandingEyeHeight(), 0);
		var portal = anchor.add(look.multiply(2.3)).add(0, 0.6, 0);
		float blast = com.phantompowers.DarkPowers.PORTAL_BLAST;
		// three shots, blended smoothly into each other
		var midA = head.add(0, -0.4, 0).lerp(portal, 0.5);
		double dolly = 4.8 - 1.2 * smooth(Math.min(1f, t / (blast - 10)));
		var fromA = midA.add(side.multiply(dolly)).add(look.multiply(-0.8)).add(0, 0.7, 0);
		var fromB = head.add(look.multiply(1.5)).add(side.multiply(0.9)).add(0, 0.1, 0);
		float kc = smooth(Math.min(1f, Math.max(0f, (t - blast) / 12f)));
		var midC = head.add(0, -0.5, 0).lerp(portal, 0.35);
		var fromC = midC.add(side.multiply(3.2 + 1.6 * kc)).add(look.multiply(-1.2 * kc)).add(0, 0.6 + 0.4 * kc, 0);
		float ab = smooth(clamp01((t - (blast - 14)) / 7f));   // A -> B
		float bc = smooth(clamp01((t - (blast + 1)) / 6f));    // B -> C
		net.minecraft.util.math.Vec3d from = fromA.lerp(fromB, ab).lerp(fromC, bc);
		net.minecraft.util.math.Vec3d target = midA.lerp(head, ab).lerp(midC, bc);
		// camera shake from the blast, easing off
		float shake = Math.max(0f, 1f - (t - blast) / 18f) * (t >= blast ? 0.16f : 0f);
		from = from.add(Math.sin(t * 3.7) * shake, Math.cos(t * 4.3) * shake, Math.sin(t * 5.1) * shake);
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

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static float smooth(float v) {
		return v * v * (3 - 2 * v);
	}

	private static void caption(DrawContext ctx, MinecraftClient mc, int w, int h, int bar, String line, int color, float size, float jitter) {
		ctx.getMatrices().push();
		ctx.getMatrices().translate(w / 2f + jitter, h - bar / 2f - 6, 0);
		ctx.getMatrices().scale(size, size, 1f);
		ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
		ctx.getMatrices().pop();
	}

	/** True while any cutscene is playing (the normal HUD is hidden so the captions are clear). */
	public static boolean active() {
		return active;
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

		if (ClientGhost.cutsceneKind == 4) return;   // the tornado: no captions, just the action
		if (ClientGhost.cutsceneKind == 9) {
			float el = com.phantompowers.Cutscenes.HERO_OFF_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float change = com.phantompowers.Cutscenes.HERO_OFF_CHANGE;
			int a = (int) (60 * Math.max(0f, 1f - el / change));
			ctx.fillGradient(0, bar, w, h / 3, (a << 24) | 0x33FF77, 0);
			String line = el < 20 ? "Phew..." : el < change ? "Okay, ghost time's over." : "Back to normal. Mostly.";
			caption(ctx, mc, w, h, bar, line, el >= change ? 0xFFE8FFF0 : 0xFFCFFFE0, el >= change ? 1.9f : 1.5f, 0f);
			return;
		}
		if (ClientGhost.cutsceneKind == 10) {
			float el = com.phantompowers.Cutscenes.DARK_OFF_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float change = com.phantompowers.Cutscenes.DARK_OFF_CHANGE;
			// the green tint fights back in flickers, then drains away
			float beat = (float) Math.pow(Math.abs(Math.sin(el * 0.26)), 6);
			int a = el < change ? (int) (40 + 70 * beat) : (int) (110 * Math.max(0f, 1f - (el - change) / 12f));
			ctx.fillGradient(0, bar, w, h / 3, (a << 24) | 0x0F6A1E, 0);
			ctx.fillGradient(0, h * 2 / 3, w, h - bar, 0, (a << 24) | 0x0F6A1E);
			if (el < change && ((int) el % 17 == 0)) ctx.fill(0, bar, w, h - bar, 0x30FF4FC8);
			String line = el < 24 ? "No... not yet..." : el < change ? "Fine. The darkness can wait." : "...This isn't over.";
			caption(ctx, mc, w, h, bar, line, el < change ? 0xFFFF7FD8 : 0xFF7DDC8A, el >= change ? 1.9f : 1.6f, el < change ? (float) Math.sin(shown * 2.9) * 1.5f : 0f);
			return;
		}
		if (ClientGhost.cutsceneKind == 11) {
			float el = com.phantompowers.Cutscenes.SUIT_OFF_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float done = com.phantompowers.Cutscenes.SUIT_OFF_DONE;
			// the visor HUD shutting down: brackets shrink in, the scan line slows and dims, then a blink out
			float k = Math.min(1f, el / done);
			int alpha = (int) (160 * (1 - k));
			int bw = (int) (24 * (1 - k)) + 2;
			for (int[] c : new int[][]{{8, bar + 8, 1, 1}, {w - 8, bar + 8, -1, 1}, {8, h - bar - 8, 1, -1}, {w - 8, h - bar - 8, -1, -1}}) {
				ctx.fill(Math.min(c[0], c[0] + c[2] * bw), c[1], Math.max(c[0], c[0] + c[2] * bw), c[1] + c[3], (alpha << 24) | 0x66E0FF);
				ctx.fill(c[0], Math.min(c[1], c[1] + c[3] * bw), c[0] + c[2], Math.max(c[1], c[1] + c[3] * bw), (alpha << 24) | 0x66E0FF);
			}
			int sweepY = bar + (int) ((el * 3 * (1 - k * 0.7)) % (h - 2 * bar));
			ctx.fill(0, sweepY, w, sweepY + 1, ((alpha / 2) << 24) | 0x66E0FF);
			String line = el < done ? "Suit powering down...  " + (int) (100 * (1 - k)) + "%" : "Suit stowed.";
			if (el >= done && el < done + 3) ctx.fill(0, h / 2 - 1, w, h / 2 + 1, 0xC0FFFFFF);   // old-TV blink out
			caption(ctx, mc, w, h, bar, line, 0xFF9FEFFF, el >= done ? 1.8f : 1.4f, 0f);
			return;
		}
		if (ClientGhost.cutsceneKind == 7) {
			float el = com.phantompowers.Cutscenes.HERO_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float change = com.phantompowers.Cutscenes.HERO_CHANGE;
			// a soft green glow building at the edges of the screen
			int a = (int) (Math.min(1f, el / change) * 70);
			ctx.fillGradient(0, bar, w, h / 3, (a << 24) | 0x33FF77, 0);
			ctx.fillGradient(0, h * 2 / 3, w, h - bar, 0, (a << 24) | 0x33FF77);
			String line;
			int color = 0xFFFFFFFF;
			float size;
			if (el < 30) {
				line = "Okay... deep breath.";
				size = 1.5f;
			} else if (el < change - 4) {
				line = "Time to go to work.";
				size = 1.7f;
				color = 0xFFCFFFE0;
			} else {
				line = "I'M GOING GHOST!";
				size = 2.8f;
				color = 0xFF66FF99;
				if (el >= change) {
					int fl = (int) (220 * Math.max(0f, 1f - (el - change) / 8f));
					if (fl > 0) ctx.fill(0, 0, w, h, (fl << 24) | 0xE8FFF0);
				}
			}
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(size, size, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
			return;
		}
		if (ClientGhost.cutsceneKind == 8) {
			float el = com.phantompowers.Cutscenes.SUIT_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float plates = com.phantompowers.Cutscenes.SUIT_PLATES, done = com.phantompowers.Cutscenes.SUIT_DONE;
			// a cyan visor HUD booting up: scan line, corner brackets, status text
			int cyan = 0x66E0FF;
			int sweepY = bar + (int) (((el * 6) % (h - 2 * bar)));
			ctx.fill(0, sweepY, w, sweepY + 1, 0x4066E0FF);
			int bw = 24;
			for (int[] c : new int[][]{{8, bar + 8, 1, 1}, {w - 8, bar + 8, -1, 1}, {8, h - bar - 8, 1, -1}, {w - 8, h - bar - 8, -1, -1}}) {
				ctx.fill(Math.min(c[0], c[0] + c[2] * bw), c[1], Math.max(c[0], c[0] + c[2] * bw), c[1] + c[3], 0xA0000000 | cyan);
				ctx.fill(c[0], Math.min(c[1], c[1] + c[3] * bw), c[0] + c[2], Math.max(c[1], c[1] + c[3] * bw), 0xA0000000 | cyan);
			}
			String line;
			int color = 0xFF9FEFFF;
			float size = 1.5f;
			if (el < plates) line = "Suit pack online...";
			else if (el < done) {
				int pct = (int) (100 * (el - plates) / (done - plates));
				line = "Deploying armour  " + Math.min(100, pct) + "%";
			} else {
				line = "SUIT UP!";
				size = 2.6f;
				color = 0xFF66E8FF;
				int fl = (int) (180 * Math.max(0f, 1f - (el - done) / 6f));
				if (fl > 0) ctx.fill(0, 0, w, h, (fl << 24) | 0xC8F4FF);
			}
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(size, size, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
			return;
		}
		if (ClientGhost.cutsceneKind == 5) {
			float el = com.phantompowers.Cutscenes.DARK_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float change = com.phantompowers.Cutscenes.DARK_CHANGE;
			// a sickly green tint creeping in from the edges, pulsing like a heartbeat
			float beat = (float) Math.pow(Math.abs(Math.sin(el * 0.12)), 4);
			int a = (int) (Math.min(1f, el / 40f) * (50 + 50 * beat));
			ctx.fillGradient(0, bar, w, h / 3, (a << 24) | 0x0F6A1E, 0);
			ctx.fillGradient(0, h * 2 / 3, w, h - bar, 0, (a << 24) | 0x0F6A1E);
			ctx.fill(0, bar, w, h - bar, ((a / 3) << 24) | 0x1EA040);
			String line;
			int color;
			float size;
			if (el < 35) {
				line = "The darkness answers...";
				color = 0xFF7DDC8A;
				size = 1.5f;
			} else if (el < change - 12) {
				line = "No more holding back.";
				color = 0xFF9CFF9C;
				size = 1.7f;
			} else {
				line = "TIME TO GO GHOST.";
				color = 0xFFFF4FC8;
				size = 2.6f;
				if (el >= change) {
					int fl = (int) (220 * Math.max(0f, 1f - (el - change) / 8f));
					if (fl > 0) ctx.fill(0, 0, w, h, (fl << 24) | 0xFF80E0);
				}
			}
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(size, size, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
			return;
		}
		if (ClientGhost.cutsceneKind == 6) {
			float el = com.phantompowers.Cutscenes.ORIGIN_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float blast = com.phantompowers.Cutscenes.ORIGIN_BLAST;
			String line;
			int color = 0xFFFFFFFF;
			float size = 1.6f;
			if (el < 18) line = "Huh... the portal's turning on?";
			else if (el < blast) {
				line = "AAAAAAHHHHH!";
				color = 0xFF66FF99;
				size = 2.4f;
				// green flicker as the energy tears through you
				int a = (int) (40 + 60 * Math.abs(Math.sin(el * 1.3)));
				ctx.fill(0, bar, w, h - bar, (a << 24) | 0x33FF77);
			} else {
				float k = el - blast;
				int a = (int) (255 * Math.max(0f, 1f - k / 16f));
				if (a > 0) ctx.fill(0, 0, w, h, (a << 24) | (k < 4 ? 0xFFFFFF : 0x99FFBB));
				line = k < 18 ? "..." : "Whoa... I'm a GHOST?!";
				color = 0xFF66FF99;
				size = k < 18 ? 1.6f : 2.0f;
			}
			float jitter = el >= 18 && el < blast ? (float) Math.sin(shown * 3.3) * 2.5f : 0f;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f + jitter, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(size, size, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
			return;
		}
		if (ClientGhost.cutsceneKind == 3) {
			float el = com.phantompowers.DarkPowers.PORTAL_TICKS - ClientGhost.cutscene + tickCounter.getTickDelta(true);
			float blast = com.phantompowers.DarkPowers.PORTAL_BLAST;
			String line;
			int color = 0xFFFFFFFF;
			float size = 1.6f;
			if (el < 14) line = "A homemade ghost portal...";
			else if (el < 30) line = "What does this switch do?";
			else if (el < blast) {
				line = "Uh... is it supposed to hum like that?";
				// the screen starts to glow green as it charges
				int a = (int) (60 * (el - 30) / (blast - 30));
				ctx.fill(0, bar, w, h - bar, (Math.max(0, a) << 24) | 0x33FF77);
			} else {
				float k = el - blast;
				// blinding green-white flash that fades out
				int a = (int) (255 * Math.max(0f, 1f - k / 14f));
				if (a > 0) ctx.fill(0, 0, w, h, (a << 24) | (k < 3 ? 0xEFFFF4 : 0x66FF99));
				line = k < 16 ? "WHOAAA!" : "...ugh... my face...";
				color = k < 16 ? 0xFF66FF99 : 0xFFB8FF60;
				size = k < 16 ? 2.6f : 1.6f;
			}
			float jitter = el >= blast && el < blast + 16 ? (float) Math.sin(shown * 3.3) * 3f : 0f;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f + jitter, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(size, size, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
			return;
		}
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
