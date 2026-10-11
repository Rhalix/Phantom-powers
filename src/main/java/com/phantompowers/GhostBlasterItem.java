package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/**
 * The ghost hunter's weapon: fires a bolt of anti-ghost energy. Extra damage to ghosts,
 * and stronger and faster while the Ghost Hunter Suit is deployed.
 */
public class GhostBlasterItem extends Item {
	public GhostBlasterItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerPlayerEntity p = (ServerPlayerEntity) user;
		ServerWorld w = p.getServerWorld();
		boolean suited = GhostState.of(p.getServer()).data(p.getUuid()).suitOn;

		Vec3d look = p.getRotationVec(1f);
		Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
		Vec3d muzzle = p.getEyePos().add(look.multiply(0.8)).add(right.normalize().multiply(hand == Hand.MAIN_HAND ? 0.3 : -0.3)).add(0, -0.2, 0);
		Vec3d end = p.getEyePos().add(look.multiply(48));
		BlockHitResult bh = w.raycast(new RaycastContext(p.getEyePos(), end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		if (bh.getType() != HitResult.Type.MISS) end = bh.getPos();
		Box box = p.getBoundingBox().stretch(end.subtract(p.getEyePos())).expand(1.0);
		EntityHitResult eh = ProjectileUtil.raycast(p, p.getEyePos(), end, box,
				e -> e != p && !e.isSpectator() && e.canHit(), p.getEyePos().squaredDistanceTo(end));
		if (eh != null) {
			end = eh.getPos();
			Entity t = eh.getEntity();
			float dmg = t instanceof AbstractGhostEntity ? 9f : 5f;
			if (suited) dmg *= 1.5f;
			t.damage(w.getDamageSources().indirectMagic(p, p), dmg);
			if (t instanceof LivingEntity le) le.takeKnockback(0.4, -look.x, -look.z);
		}
		BeamPayload beam = new BeamPayload((float) muzzle.x, (float) muzzle.y, (float) muzzle.z,
				(float) end.x, (float) end.y, (float) end.z, suited ? 0x55E8FF : 0x77FFAA);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, muzzle, 96)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.9f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.35f, 2.0f);
		p.getItemCooldownManager().set(this, suited ? 5 : 9);
		stack.damage(1, p, LivingEntity.getSlotForHand(hand));
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click to fire. Extra damage to ghosts.").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Stronger and faster with the Ghost Hunter Suit on.").formatted(Formatting.DARK_AQUA));
		tooltip.add(Text.literal("Repair with ectoplasm in an anvil.").formatted(Formatting.DARK_GRAY));
	}
}
