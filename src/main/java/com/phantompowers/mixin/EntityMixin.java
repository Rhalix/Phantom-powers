package com.phantompowers.mixin;

import com.phantompowers.GhostFlags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

	/**
	 * Intangible movement:
	 *  - sideways: straight through walls
	 *  - floors and ceilings stay solid, so you walk normally
	 *  - hold sneak to sink down through the floor
	 *  - while flying, you can rise and drop through anything
	 */
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void phantom$phaseMove(MovementType type, Vec3d movement, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (GhostFlags.isHost(self)) {
			self.noClip = true;
			return;
		}
		if (!GhostFlags.isIntangible(self)) return;
		self.noClip = true;
		if (type != MovementType.SELF && type != MovementType.PLAYER) return;
		ci.cancel();

		boolean flying = self instanceof PlayerEntity p && p.getAbilities().flying;
		boolean sinking = !flying && self.isSneaking();
		double dy = movement.y;
		if (sinking) {
			dy = Math.max(Math.min(dy, -0.15), -0.35);
			Vec3d v = self.getVelocity();
			self.setVelocity(v.x, Math.max(v.y, -0.35), v.z);
		}

		if (!flying && !sinking && dy != 0) {
			Box moved = self.getBoundingBox().offset(movement.x, 0, movement.z);
			Iterable<VoxelShape> shapes = self.getWorld().getBlockCollisions(self, moved.stretch(0, dy, 0));
			double allowed = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, moved, shapes, dy);
			boolean hit = Math.abs(allowed - dy) > 1.0E-7;
			self.setOnGround(hit && dy < 0);
			if (hit) {
				Vec3d v = self.getVelocity();
				self.setVelocity(v.x, 0, v.z);
			}
			dy = allowed;
		} else {
			self.setOnGround(false);
		}
		self.setPosition(self.getX() + movement.x, self.getY() + dy, self.getZ() + movement.z);
		self.fallDistance = 0;
	}

	/** Possessed hosts float while the ghost steers them. */
	@Inject(method = "hasNoGravity", at = @At("HEAD"), cancellable = true)
	private void phantom$float(CallbackInfoReturnable<Boolean> cir) {
		if (GhostFlags.isHost((Entity) (Object) this)) cir.setReturnValue(true);
	}

	/** No suffocating inside walls while intangible. */
	@Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
	private void phantom$notInWall(CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (GhostFlags.isIntangible(self) || GhostFlags.isHost(self)) cir.setReturnValue(false);
	}
}
