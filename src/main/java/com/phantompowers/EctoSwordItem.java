package com.phantompowers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Hunter Ecto Sword: a hilt that projects a humming blade of solid ecto-energy, like a laser sword.
 * Sneak + right-click to ignite or retract it. While it's lit, swings hit hard (extra hard against ghosts), and
 * holding right-click raises a guard that blocks hits and bats projectiles straight back at whoever fired them.
 * Paint it at the Hunter Workbench to change the blade colour; upgrades make it hit harder.
 */
public class EctoSwordItem extends Item {
	public static final int IGNITE_TICKS = 6;
	public static final int DEFAULT_COLOR = 0x46FF8C;

	public EctoSwordItem(Settings settings) {
		super(settings);
	}

	private static NbtCompound data(ItemStack s) {
		return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
	}

	public static boolean lit(ItemStack s) {
		return data(s).getBoolean("Lit");
	}

	/** World time when the blade last ignited or retracted (for the animation). */
	public static long litAt(ItemStack s) {
		return data(s).getLong("LitAt");
	}

	public static int bladeColor(ItemStack s) {
		int p = HunterPaint.paint(s);
		return p >= 0 ? p : DEFAULT_COLOR;
	}

	/** How far out the blade is right now, 0..1. */
	public static float extension(ItemStack s, long worldTime, float partial) {
		float t = Math.min(1f, (worldTime - litAt(s) + partial) / IGNITE_TICKS);
		t = Math.max(0f, t);
		return lit(s) ? t : 1f - t;
	}

	public static void setLit(ItemStack s, boolean lit, long time) {
		NbtCompound n = data(s);
		n.putBoolean("Lit", lit);
		n.putLong("LitAt", time);
		s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.isSneaking() || !lit(stack)) {
			if (!world.isClient) {
				boolean on = !lit(stack);
				setLit(stack, on, world.getTime());
				world.playSound(null, user.getX(), user.getY(), user.getZ(), on ? SoundEvents.BLOCK_BEACON_ACTIVATE : SoundEvents.BLOCK_BEACON_DEACTIVATE,
						SoundCategory.PLAYERS, 0.9f, on ? 2.0f : 1.8f);
				world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.4f, on ? 1.9f : 1.4f);
			}
			return TypedActionResult.success(stack, world.isClient);
		}
		// lit: hold right-click to guard
		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return lit(stack) ? UseAction.BLOCK : UseAction.NONE;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource damageSource) {
		if (!(damageSource.getWeaponStack() instanceof ItemStack s) || !lit(s)) return 0f;
		return (8f * HunterPaint.weaponBoost(s)) * HunterSuit.ghostBonus(target) - 1f;
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (lit(stack) && attacker.getWorld() instanceof ServerWorld w) {
			w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.6f);
			w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.5f, 2.0f);
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.6), target.getZ(), 14, 0.3, 0.4, 0.3, 0.2);
			if (target.isOnFire() || attacker.getRandom().nextInt(4) == 0) target.setOnFireFor(2);
		}
		return true;
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (!lit(stack) || !(entity instanceof LivingEntity holder)) return;
		boolean inHand = selected || holder.getOffHandStack() == stack;
		if (!inHand) {
			// put it away and it switches off
			if (!world.isClient) setLit(stack, false, world.getTime());
			return;
		}
		if (world.isClient) return;
		ServerWorld w = (ServerWorld) world;
		if (world.getTime() % 24 == 0) {
			w.playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.5f, 1.9f);
		}
		// guarding: bat incoming projectiles straight back
		if (holder.isUsingItem() && holder.getActiveItem() == stack) {
			for (ProjectileEntity pr : w.getEntitiesByClass(ProjectileEntity.class, holder.getBoundingBox().expand(2.5), p -> p.getOwner() != holder)) {
				Vec3d v = pr.getVelocity();
				if (v.lengthSquared() < 0.01) continue;
				Vec3d toMe = holder.getPos().subtract(pr.getPos());
				if (v.dotProduct(toMe) <= 0) continue;
				Vec3d back = holder.getRotationVec(1f).multiply(Math.max(1.2, v.length()));
				pr.setVelocity(back);
				pr.velocityModified = true;
				pr.setOwner(holder);
				w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pr.getX(), pr.getY(), pr.getZ(), 10, 0.1, 0.1, 0.1, 0.2);
				w.playSound(null, pr.getX(), pr.getY(), pr.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8f, 1.8f);
			}
		}
	}

	@Override
	public boolean canMine(net.minecraft.block.BlockState state, World world, net.minecraft.util.math.BlockPos pos, PlayerEntity miner) {
		return !miner.isCreative();
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal(lit(stack) ? "Blade: ON" : "Blade: off").formatted(lit(stack) ? Formatting.GREEN : Formatting.GRAY));
		tooltip.add(Text.literal("Sneak + right-click: ignite / retract").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Hold right-click (lit): guard and deflect shots").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Extra damage to ghosts. Paint it at the Hunter Workbench").formatted(Formatting.DARK_GRAY));
	}
}
