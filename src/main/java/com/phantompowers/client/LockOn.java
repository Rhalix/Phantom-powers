package com.phantompowers.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Lock-on: press the key to lock your view onto the enemy nearest the middle of your screen. Your camera follows it
 * (so blasts and attacks aim at it automatically) until it dies, gets too far away or you press the key again.
 */
public final class LockOn {
	private static final double RANGE = 40, MAX_ANGLE = Math.toRadians(40);
	private static LivingEntity target;
	private static int hiddenTicks;

	private LockOn() {}

	public static LivingEntity target() {
		return target;
	}

	public static void toggle(MinecraftClient mc) {
		ClientPlayerEntity p = mc.player;
		if (p == null) return;
		if (target != null) {
			release(mc, "Lock-on off");
			return;
		}
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (Entity e : mc.world.getOtherEntities(p, p.getBoundingBox().expand(RANGE))) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator() || le.isInvisibleTo(p)) continue;
			if (le.hasPassenger(p) || p.hasPassenger(le)) continue;
			Vec3d to = aimPoint(le).subtract(eye);
			double dist = to.length();
			if (dist > RANGE || dist < 0.5) continue;
			double angle = Math.acos(MathHelper.clamp(to.normalize().dotProduct(look), -1, 1));
			if (angle > MAX_ANGLE || !p.canSee(le)) continue;
			double score = angle + dist * 0.004 + (le instanceof Monster || le instanceof com.phantompowers.entity.AbstractGhostEntity ? 0 : 0.35);
			if (score < bestScore) {
				bestScore = score;
				best = le;
			}
		}
		if (best == null) {
			p.sendMessage(Text.literal("Nothing to lock on to"), true);
			return;
		}
		target = best;
		hiddenTicks = 0;
		p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.7f, 1.8f);
		p.sendMessage(Text.literal("Locked on: " + best.getName().getString()), true);
	}

	private static void release(MinecraftClient mc, String why) {
		target = null;
		if (mc.player != null && why != null) mc.player.sendMessage(Text.literal(why), true);
	}

	private static Vec3d aimPoint(LivingEntity e) {
		return e.getPos().add(0, e.getHeight() * 0.6, 0);
	}

	public static void tick(MinecraftClient mc) {
		if (target == null) return;
		ClientPlayerEntity p = mc.player;
		if (p == null || mc.world == null || target.getWorld() != mc.world) {
			target = null;
			return;
		}
		if (!target.isAlive() || target.isRemoved()) {
			release(mc, "Target down!");
			return;
		}
		if (p.squaredDistanceTo(target) > (RANGE + 8) * (RANGE + 8)) {
			release(mc, "Target too far away");
			return;
		}
		hiddenTicks = p.canSee(target) ? 0 : hiddenTicks + 1;
		if (hiddenTicks > 60) release(mc, "Lost sight of the target");
	}

	/** Every frame: turn the camera smoothly toward the target. */
	public static void frame(MinecraftClient mc) {
		if (target == null || mc.player == null || mc.currentScreen != null) return;
		ClientPlayerEntity p = mc.player;
		Vec3d to = aimPoint(target).subtract(p.getEyePos());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		float wantYaw = (float) (MathHelper.atan2(to.z, to.x) * MathHelper.DEGREES_PER_RADIAN) - 90f;
		float wantPitch = (float) -(MathHelper.atan2(to.y, flat) * MathHelper.DEGREES_PER_RADIAN);
		float dy = MathHelper.wrapDegrees(wantYaw - p.getYaw()), dp = wantPitch - p.getPitch();
		float k = 0.35f;
		p.setYaw(p.getYaw() + dy * k);
		p.setPitch(MathHelper.clamp(p.getPitch() + dp * k, -90f, 90f));
		p.setHeadYaw(p.getYaw());
	}

	/** A lock-on reticle in the middle of the screen, with the target's name and health. */
	public static void render(DrawContext ctx, RenderTickCounter tick) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (target == null || mc.options.hudHidden) return;
		int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
		int s = 9, l = 4, col = 0xFFFF4848;
		float pulse = (mc.world.getTime() % 20) / 20f;
		int g = s + (int) (Math.sin(pulse * Math.PI * 2) * 1.5);
		// four corner brackets
		ctx.fill(cx - g, cy - g, cx - g + l, cy - g + 1, col);
		ctx.fill(cx - g, cy - g, cx - g + 1, cy - g + l, col);
		ctx.fill(cx + g - l + 1, cy - g, cx + g + 1, cy - g + 1, col);
		ctx.fill(cx + g, cy - g, cx + g + 1, cy - g + l, col);
		ctx.fill(cx - g, cy + g, cx - g + l, cy + g + 1, col);
		ctx.fill(cx - g, cy + g - l + 1, cx - g + 1, cy + g + 1, col);
		ctx.fill(cx + g - l + 1, cy + g, cx + g + 1, cy + g + 1, col);
		ctx.fill(cx + g, cy + g - l + 1, cx + g + 1, cy + g + 1, col);
		// name and health bar
		String name = target.getName().getString();
		int w = mc.textRenderer.getWidth(name);
		ctx.drawTextWithShadow(mc.textRenderer, name, cx - w / 2, cy + g + 5, 0xFFFF8080);
		float hp = MathHelper.clamp(target.getHealth() / Math.max(1f, target.getMaxHealth()), 0f, 1f);
		int bw = 40, by = cy + g + 15;
		ctx.fill(cx - bw / 2 - 1, by - 1, cx + bw / 2 + 1, by + 3, 0xC0000000);
		ctx.fill(cx - bw / 2, by, cx - bw / 2 + (int) (bw * hp), by + 2, 0xFFE03030);
	}
}
