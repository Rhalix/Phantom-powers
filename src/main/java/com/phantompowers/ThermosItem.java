package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.GhostJailerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Point it at a ghost and right-click: the ghost gets sucked inside. It holds as many ghosts as you like,
 * but each catch wears it down. Sneak + right-click to let them all out. If it breaks, they all escape!
 */
public class ThermosItem extends Item {
	private static final String KEY = "TrappedGhosts";
	private static final List<Capture> CAPTURES = new ArrayList<>();

	public ThermosItem(Settings settings) {
		super(settings);
	}

	private static final class Capture {
		final ServerPlayerEntity player;
		final Hand hand;
		final Entity target;
		final boolean prevAi;
		int ticks;
		/** Half-ghost players can struggle free by spamming jump. */
		int struggle;

		Capture(ServerPlayerEntity player, Hand hand, Entity target) {
			this.player = player;
			this.hand = hand;
			this.target = target;
			this.prevAi = target instanceof MobEntity m && m.isAiDisabled();
		}
	}

	public static boolean canTrap(Entity e) {
		if (e.getCommandTags().contains("phantom_ep_boss")) return e.getCommandTags().contains("phantom_ep_dazed");
		// A boss tangled in a Ghost Net can be sucked up no matter how healthy it is
		if (FrostLogic.isBoss(e) && com.phantompowers.entity.GhostNetTrapEntity.isNetted(e)) return true;
		if (e instanceof GhostJailerEntity j) return j.getHealth() < j.getMaxHealth() * 0.3f;
		if (e instanceof com.phantompowers.entity.CrateKingEntity k) return k.getHealth() < k.getMaxHealth() * 0.2f;
		if (e instanceof com.phantompowers.entity.VoltageEntity v) return v.getHealth() < v.getMaxHealth() * 0.2f;
		if (e instanceof com.phantompowers.entity.WishkeeperEntity k) return k.getHealth() < k.getMaxHealth() * 0.2f;
		if (e instanceof com.phantompowers.entity.RevEntity k) return k.getHealth() < k.getMaxHealth() * 0.2f;
		if (e instanceof com.phantompowers.entity.RoxyEntity k) return k.getHealth() < k.getMaxHealth() * 0.2f;
		if (e instanceof com.phantompowers.entity.ChefGhoulashEntity k) return k.dazed() || k.getHealth() < k.getMaxHealth() * 0.25f;
		if (e instanceof com.phantompowers.entity.CollectorEntity k) return !k.chasing() && (k.dazed() || k.getHealth() < k.getMaxHealth() * 0.2f);
		if (e instanceof com.phantompowers.entity.RageWyvernEntity k) return !k.storyMode() && k.getHealth() < k.getMaxHealth() * 0.2f;
		if (e instanceof com.phantompowers.entity.ShadeEntity s) return !s.isBoss() || s.getHealth() < s.getMaxHealth() * 0.25f;
		return e instanceof AbstractGhostEntity || e instanceof VexEntity || e instanceof PhantomEntity;
	}

	// ------------------------------------------------------------------- use

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerPlayerEntity p = (ServerPlayerEntity) user;

		if (user.isSneaking()) {
			int n = releaseAll((ServerWorld) world, stack, p.getPos().add(p.getRotationVec(1f).multiply(2.5)));
			if (n > 0) {
				world.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.THERMOS_SUCK, SoundCategory.PLAYERS, 1.0f, 0.6f);
				p.sendMessage(Text.literal("Released " + n + " ghost" + (n == 1 ? "" : "s") + "!").formatted(Formatting.AQUA), true);
			} else {
				p.sendMessage(Text.literal("The Thermos is empty.").formatted(Formatting.GRAY), true);
			}
			return TypedActionResult.success(stack);
		}

		for (Capture c : CAPTURES) if (c.player == p) return TypedActionResult.pass(stack);

		Entity target = aim(p, 18);
		if (target == null) {
			p.sendMessage(Text.literal("Point the Thermos at a ghost!").formatted(Formatting.GRAY), true);
			return TypedActionResult.fail(stack);
		}
		if (FrostLogic.isBoss(target) && !canTrap(target)) {
			p.sendMessage(Text.literal("It's too strong! Weaken it below a quarter of its health first.").formatted(Formatting.RED), true);
			return TypedActionResult.fail(stack);
		}
		if (target instanceof MobEntity m) m.setAiDisabled(true);
		CAPTURES.add(new Capture(p, hand, target));
		world.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.THERMOS_SUCK, SoundCategory.PLAYERS, 1.2f, 1.0f);
		scream((ServerWorld) world, target);
		p.getItemCooldownManager().set(this, 25);
		return TypedActionResult.success(stack);
	}

	private static final int PLAYER_CAPTURE_TICKS = 70;
	private static final int STRUGGLE_FREE = 14;

	/** A captured half-ghost pressed jump: fight the pull. */
	public static void struggle(ServerPlayerEntity target) {
		for (Capture c : CAPTURES) {
			if (c.target != target) continue;
			c.struggle++;
			ServerWorld w = target.getServerWorld();
			w.spawnParticles(GhostPowers.GREEN, target.getX(), target.getY() + 1, target.getZ(), 6, 0.4, 0.5, 0.4, 0.05);
			w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 0.6f, 1.2f + c.struggle * 0.05f);
			return;
		}
	}

	private static void breakFree(ServerWorld w, ServerPlayerEntity holder, ServerPlayerEntity tp) {
		Vec3d away = tp.getPos().subtract(holder.getPos()).normalize();
		tp.addVelocity(away.x * 1.2, 0.6, away.z * 1.2);
		tp.velocityModified = true;
		holder.addVelocity(-away.x * 0.8, 0.3, -away.z * 0.8);
		holder.velocityModified = true;
		holder.getItemCooldownManager().set(ModItems.THERMOS, 100);
		w.spawnParticles(GhostPowers.GREEN, tp.getX(), tp.getY() + 1, tp.getZ(), 60, 0.6, 0.8, 0.6, 0.15);
		w.spawnParticles(ParticleTypes.EXPLOSION, tp.getX(), tp.getY() + 1, tp.getZ(), 1, 0, 0, 0, 0);
		w.playSound(null, tp.getX(), tp.getY(), tp.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.8f, 1.6f);
		tp.sendMessage(Text.literal("You broke free of the Thermos!").formatted(Formatting.GREEN, Formatting.BOLD), true);
		holder.sendMessage(Text.literal(tp.getName().getString() + " broke free of the Thermos!").formatted(Formatting.RED), true);
	}

	/** The ghost begs as it gets pulled in: "No, no, NOOOO!" */
	public static void scream(ServerWorld w, Entity t) {
		boolean boss = FrostLogic.isBoss(t);
		float pitch = boss ? 0.72f : 0.9f + w.random.nextFloat() * 0.3f;
		w.playSound(null, t.getX(), t.getY(), t.getZ(), ModSounds.GHOST_NONONO, SoundCategory.HOSTILE, 1.6f, pitch);
		Text line = Text.literal("<").append(t.getDisplayName()).append("> ")
				.append(Text.literal("No... no... NOOOOO!").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
		for (ServerPlayerEntity sp : w.getPlayers()) {
			if (sp != t && sp.squaredDistanceTo(t) < 32 * 32) sp.sendMessage(line, false);
		}
	}

	/** Ghosts, plus players who are in ghost form. */
	private static Entity aim(ServerPlayerEntity p, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = start.add(p.getRotationVec(1f).multiply(range));
		BlockHitResult bh = p.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		if (bh.getType() != HitResult.Type.MISS) end = bh.getPos();
		Box box = p.getBoundingBox().stretch(end.subtract(start)).expand(1.5);
		EntityHitResult eh = ProjectileUtil.raycast(p, start, end, box,
				e -> e != p && (canTrap(e) || FrostLogic.isBoss(e) || isGhostPlayer(e)), start.squaredDistanceTo(end));
		return eh == null ? null : eh.getEntity();
	}

	private static boolean isGhostPlayer(Entity e) {
		if (!(e instanceof ServerPlayerEntity sp)) return false;
		GhostData d = GhostState.of(sp.getServer()).data(sp.getUuid());
		return d.transformed && d.possessing == null && !GhostFlags.HOSTS.contains(sp.getUuid());
	}

	// ------------------------------------------------------------------ tick

	public static void tick() {
		Iterator<Capture> it = CAPTURES.iterator();
		while (it.hasNext()) {
			Capture c = it.next();
			ServerPlayerEntity p = c.player;
			Entity t = c.target;
			ItemStack stack = p.getStackInHand(c.hand);
			boolean holding = stack.getItem() instanceof ThermosItem;
			if (p.isRemoved() || !p.isAlive() || !holding || t.isRemoved() || !t.isAlive() || t.getWorld() != p.getWorld()
					|| t.squaredDistanceTo(p) > 30 * 30) {
				if (t instanceof MobEntity m) m.setAiDisabled(c.prevAi);
				it.remove();
				continue;
			}
			ServerWorld w = p.getServerWorld();
			c.ticks++;
			Vec3d mouth = p.getEyePos().add(p.getRotationVec(1f).multiply(0.9)).add(0, -0.35, 0);
			Vec3d center = t.getPos().add(0, t.getHeight() / 2, 0);
			Vec3d toMouth = mouth.subtract(center);

			// Pull the ghost in, spinning (players fight it, so they come in slower)
			boolean isPlayer = t instanceof ServerPlayerEntity;
			double pull = isPlayer ? Math.min(1.0, 0.02 + c.ticks * 0.0045) : Math.min(1.0, 0.06 + c.ticks * 0.012);
			Vec3d next = center.add(toMouth.multiply(pull)).subtract(0, t.getHeight() / 2, 0);
			if (t instanceof ServerPlayerEntity tp) tp.requestTeleport(next.x, next.y, next.z);
			else {
				t.refreshPositionAndAngles(next.x, next.y, next.z, t.getYaw() + 25f, t.getPitch());
				t.setVelocity(Vec3d.ZERO);
			}

			// Swirling suction stream
			double len = toMouth.length();
			Vec3d dir = len > 0 ? toMouth.multiply(1 / len) : Vec3d.ZERO;
			Vec3d u = dir.crossProduct(new Vec3d(0, 1, 0));
			if (u.lengthSquared() < 1.0E-4) u = new Vec3d(1, 0, 0);
			u = u.normalize();
			Vec3d v = dir.crossProduct(u).normalize();
			for (double s = 0; s < len; s += 0.25) {
				double a = s * 4 + c.ticks * 0.8;
				double rad = 0.08 + 0.3 * (s / Math.max(1, len));
				Vec3d pt = mouth.subtract(dir.multiply(s)).add(u.multiply(Math.cos(a) * rad)).add(v.multiply(Math.sin(a) * rad));
				w.spawnParticles(c.ticks % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
			}
			w.spawnParticles(GhostPowers.GREEN, center.x, center.y, center.z, 4, 0.3, 0.4, 0.3, 0);

			if (t instanceof ServerPlayerEntity tp) {
				// Spam jump to break free!
				int bars = Math.min(20, c.struggle * 20 / STRUGGLE_FREE);
				tp.sendMessage(Text.literal("SPAM JUMP TO BREAK FREE!  ").formatted(Formatting.RED, Formatting.BOLD)
						.append(Text.literal("|".repeat(bars)).formatted(Formatting.GREEN))
						.append(Text.literal("|".repeat(20 - bars)).formatted(Formatting.DARK_GRAY)), true);
				if (c.struggle >= STRUGGLE_FREE) {
					it.remove();
					breakFree(w, p, tp);
					continue;
				}
			}
			if ((isPlayer ? c.ticks >= PLAYER_CAPTURE_TICKS : (c.ticks >= 22 || len < 0.9))) {
				it.remove();
				if (t instanceof ServerPlayerEntity tp) {
					w.playSound(null, tp.getX(), tp.getY(), tp.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.6f);
					scream(w, tp);
					ThermosPrison.imprison(tp, p, stack);
					continue;
				}
				trap(w, p, c, stack, t);
			}
		}
	}

	private static void trap(ServerWorld w, ServerPlayerEntity p, Capture c, ItemStack stack, Entity t) {
		if (t instanceof MobEntity m) m.setAiDisabled(c.prevAi);
		NbtCompound saved = new NbtCompound();
		if (!t.saveNbt(saved)) return;
		Story.onTrapped(w, p, t);
		NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
		NbtList list = data.getList(KEY, NbtElement.COMPOUND_TYPE);
		list.add(saved);
		data.put(KEY, list);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
		t.discard();
		w.spawnParticles(ParticleTypes.FLASH, p.getX(), p.getEyeY() - 0.3, p.getZ(), 1, 0, 0, 0, 0);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1.0f, 1.4f);
		p.sendMessage(Text.literal("Trapped! (" + list.size() + " ghost" + (list.size() == 1 ? "" : "s") + " inside)").formatted(Formatting.GREEN), true);

		NbtCompound backup = data.copy();
		stack.damage(1, p, LivingEntity.getSlotForHand(c.hand));
		if (stack.isEmpty()) {
			ItemStack ghostsOnly = new ItemStack(ModItems.THERMOS);
			ghostsOnly.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(backup));
			int n = releaseAll(w, ghostsOnly, p.getPos().add(0, 1, 0));
			p.sendMessage(Text.literal("The Thermos cracked! " + n + " ghosts escaped!").formatted(Formatting.RED, Formatting.BOLD), false);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f);
		}
	}

	private static int releaseAll(ServerWorld w, ItemStack stack, Vec3d at) {
		NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
		NbtList list = data.getList(KEY, NbtElement.COMPOUND_TYPE);
		int n = 0;
		for (int i = 0; i < list.size(); i++) {
			NbtCompound saved = list.getCompound(i);
			final double ox = at.x + (w.random.nextDouble() - 0.5) * 2;
			final double oz = at.z + (w.random.nextDouble() - 0.5) * 2;
			Entity e = EntityType.loadEntityWithPassengers(saved, w, ent -> {
				ent.refreshPositionAndAngles(ox, at.y, oz, ent.getYaw(), ent.getPitch());
				return ent;
			});
			if (e != null) {
				e.setUuid(UUID.randomUUID());
				if (w.spawnEntity(e)) n++;
				w.spawnParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.5, 0.3, 0.03);
			}
		}
		data.remove(KEY);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
		return n;
	}

	/** Every Thermos has its own id, so everyone it catches ends up inside the same Thermos. */
	public static UUID thermosId(ItemStack stack) {
		NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
		if (data.containsUuid("ThermosId")) return data.getUuid("ThermosId");
		UUID id = UUID.randomUUID();
		data.putUuid("ThermosId", id);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
		return id;
	}

	/** The ghosts saved inside a Thermos (copies; the Thermos keeps the originals). */
	public static List<NbtCompound> trappedGhosts(ItemStack stack) {
		List<NbtCompound> out = new ArrayList<>();
		NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (c == null) return out;
		NbtList list = c.copyNbt().getList(KEY, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) out.add(list.getCompound(i).copy());
		return out;
	}

	public static int trappedCount(ItemStack stack) {
		NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
		return c == null ? 0 : c.copyNbt().getList(KEY, NbtElement.COMPOUND_TYPE).size();
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		int n = trappedCount(stack);
		tooltip.add(Text.literal("Ghosts trapped: " + n).formatted(n > 0 ? Formatting.GREEN : Formatting.GRAY));
		tooltip.add(Text.literal("Right-click a ghost to trap it").formatted(Formatting.DARK_GRAY));
		tooltip.add(Text.literal("Sneak + right-click to release all").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return trappedCount(stack) > 0;
	}
}
