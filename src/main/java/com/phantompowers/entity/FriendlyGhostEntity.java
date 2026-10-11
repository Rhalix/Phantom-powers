package com.phantompowers.entity;

import com.phantompowers.ModItems;
import com.phantompowers.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/** A gentle ghost that floats along beside players and gives a small gift once if you talk to it. */
public class FriendlyGhostEntity extends AbstractGhostEntity {
	private static final String[] LINES = {
			"Oooh, a visitor! Here, take this.",
			"It gets lonely out here... a present for you!",
			"Watch out for the mean ones. Take this, it might help.",
			"I found this floating around. You can have it!",
			"Have you seen my door? Never mind, here's a gift."};

	private boolean gifted;

	public FriendlyGhostEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(3, new FollowPlayerGoal(this));
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
		if (getWorld().isClient) return ActionResult.SUCCESS;
		if (gifted) {
			player.sendMessage(Text.literal("Ghost: I already gave you my gift! Be careful out there.").formatted(Formatting.AQUA), false);
			return ActionResult.SUCCESS;
		}
		gifted = true;
		player.sendMessage(Text.literal("Ghost: " + LINES[getRandom().nextInt(LINES.length)]).formatted(Formatting.AQUA), false);
		ItemStack[] gifts = {
				new ItemStack(ModItems.ECTOPLASM, 2 + getRandom().nextInt(3)),
				new ItemStack(Items.GOLDEN_APPLE),
				new ItemStack(Items.EMERALD, 2 + getRandom().nextInt(4)),
				new ItemStack(Items.ENDER_PEARL, 1 + getRandom().nextInt(2)),
				new ItemStack(Items.GLOW_BERRIES, 6),
				new ItemStack(Items.EXPERIENCE_BOTTLE, 3)};
		ItemStack gift = gifts[getRandom().nextInt(gifts.length)];
		if (!player.giveItemStack(gift)) player.dropItem(gift, false);
		if (getWorld() instanceof ServerWorld sw) {
			sw.spawnParticles(ParticleTypes.HEART, getX(), getY() + getHeight(), getZ(), 5, 0.4, 0.3, 0.4, 0);
		}
		playSound(ModSounds.GHOST_FRIENDLY, 1.0f, 1.3f);
		return ActionResult.SUCCESS;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Gifted", gifted);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		gifted = nbt.getBoolean("Gifted");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.GHOST_FRIENDLY;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}

	/** Float along a few blocks away from the nearest player. */
	static class FollowPlayerGoal extends Goal {
		private final AbstractGhostEntity ghost;
		private PlayerEntity player;

		FollowPlayerGoal(AbstractGhostEntity ghost) {
			this.ghost = ghost;
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			player = ghost.getWorld().getClosestPlayer(ghost, 14);
			return player != null && !player.isSpectator() && ghost.squaredDistanceTo(player) > 16;
		}

		@Override
		public boolean shouldContinue() {
			return player != null && player.isAlive() && ghost.squaredDistanceTo(player) > 9 && ghost.squaredDistanceTo(player) < 400;
		}

		@Override
		public void tick() {
			Vec3d side = player.getRotationVec(1f).crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(2.0);
			ghost.flyTo(player.getPos().add(side).add(0, 1.2, 0), 0.6);
		}
	}
}
