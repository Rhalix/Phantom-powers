package com.phantompowers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A half-ghost sucked into a Thermos ends up in their own little cell inside it: a steel cylinder in its
 * own dimension. Punch the walls over and over to burst out, right back where you were caught.
 */
public final class ThermosPrison {
	public static final RegistryKey<World> DIM = RegistryKey.of(RegistryKeys.WORLD, PhantomMod.id("thermos_prison"));
	public static final int BREAK_HITS = 30;
	private static final int R = 4, H = 7, Y = 100;
	private static final Map<UUID, Integer> PROGRESS = new HashMap<>();

	private ThermosPrison() {}

	/** Every player gets their own cell, far away from everyone else's. */
	public static BlockPos cellFor(UUID id) {
		int slot = Math.floorMod(id.hashCode(), 4096);
		return new BlockPos((slot % 64) * 64, Y, (slot / 64) * 64);
	}

	public static boolean inPrison(ServerPlayerEntity p) {
		return p.getWorld().getRegistryKey().equals(DIM);
	}

	/** Builds the cell: a glowing steel cylinder with a floor and a lid. Returns true if the walls are there. */
	public static boolean buildCell(ServerWorld w, BlockPos c) {
		BlockState wall = ModBlocks.THERMOS_WALL.getDefaultState();
		BlockState air = Blocks.AIR.getDefaultState();
		for (int x = -R - 1; x <= R + 1; x++)
			for (int z = -R - 1; z <= R + 1; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > R + 0.6) continue;
				for (int y = 0; y <= H; y++) {
					boolean shell = y == 0 || y == H || d > R - 0.4;
					w.setBlockState(c.add(x, y, z), shell ? wall : air, Block.NOTIFY_LISTENERS);
				}
			}
		return w.getBlockState(c).isOf(ModBlocks.THERMOS_WALL);
	}

	/** Called when the Thermos finishes sucking in a half-ghost player. */
	public static void imprison(ServerPlayerEntity victim, ServerPlayerEntity by) {
		MinecraftServer server = victim.getServer();
		GhostState st = GhostState.of(server);
		GhostData d = st.data(victim.getUuid());
		if (d.transformed) GhostPowers.endForm(victim, d);
		d.exhaust = Math.max(d.exhaust, 120);
		ServerWorld cellWorld = server.getWorld(DIM);
		if (cellWorld == null) {
			victim.sendMessage(Text.literal("You got sucked out of ghost form by a Thermos!").formatted(Formatting.RED), false);
			return;
		}
		if (!inPrison(victim)) {
			d.thermosDim = victim.getWorld().getRegistryKey().getValue().toString();
			d.thermosPos = victim.getBlockPos();
		}
		st.markDirty();
		BlockPos c = cellFor(victim.getUuid());
		buildCell(cellWorld, c);
		victim.teleport(cellWorld, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, victim.getYaw(), 0f);
		victim.fallDistance = 0;
		PROGRESS.put(victim.getUuid(), 0);
		victim.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Trapped in a Thermos!").formatted(Formatting.GREEN, Formatting.BOLD)));
		victim.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Punch the walls to break out!").formatted(Formatting.WHITE)));
		cellWorld.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		if (by != null) {
			by.sendMessage(Text.literal("You trapped " + victim.getName().getString() + " in the Thermos!").formatted(Formatting.GREEN), true);
		}
	}

	/** Each punch on the wall counts toward breaking out. */
	public static void onPunch(ServerPlayerEntity p, BlockPos at) {
		if (!inPrison(p)) return;
		int prog = PROGRESS.merge(p.getUuid(), 1, Integer::sum);
		ServerWorld w = p.getServerWorld();
		float t = prog / (float) BREAK_HITS;
		w.playSound(null, at, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.35f + t * 0.4f, 0.6f + t * 0.8f);
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.THERMOS_WALL.getDefaultState()),
				at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
		w.spawnParticles(GhostPowers.GREEN, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
		int bars = (int) (t * 20);
		p.sendMessage(Text.literal("BREAK OUT  ").formatted(Formatting.GREEN, Formatting.BOLD)
				.append(Text.literal("|".repeat(bars)).formatted(Formatting.GREEN))
				.append(Text.literal("|".repeat(20 - bars)).formatted(Formatting.DARK_GRAY)), true);
		if (prog >= BREAK_HITS) release(p, true);
	}

	/** Out of the Thermos and back where you were caught. */
	public static void release(ServerPlayerEntity p, boolean brokeOut) {
		MinecraftServer server = p.getServer();
		GhostState st = GhostState.of(server);
		GhostData d = st.data(p.getUuid());
		PROGRESS.remove(p.getUuid());
		ServerWorld dest = null;
		if (d.thermosDim != null) dest = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(d.thermosDim)));
		BlockPos at = d.thermosPos;
		if (dest == null || at == null) {
			dest = server.getOverworld();
			at = dest.getSpawnPos();
		}
		d.thermosDim = null;
		d.thermosPos = null;
		st.markDirty();
		p.teleport(dest, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYaw(), p.getPitch());
		dest.spawnParticles(GhostPowers.GREEN, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1.0, 0.6, 0.1);
		dest.spawnParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 3, 0.5, 0.5, 0.5, 0);
		dest.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.0f, 1.4f);
		dest.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.PLAYERS, 1.2f, 0.8f);
		if (brokeOut) {
			p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("You broke out!").formatted(Formatting.GREEN, Formatting.BOLD)));
			p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
		}
	}

	public static void tick(MinecraftServer server) {
		ServerWorld w = server.getWorld(DIM);
		if (w == null) return;
		for (ServerPlayerEntity p : w.getPlayers()) {
			BlockPos c = cellFor(p.getUuid());
			PROGRESS.putIfAbsent(p.getUuid(), 0);
			// No phasing or flying out into the void: you have to fight your way out
			Vec3d center = Vec3d.ofBottomCenter(c.up());
			if (p.getPos().squaredDistanceTo(center) > 16 || p.getY() < c.getY()) {
				if (!w.getBlockState(c).isOf(ModBlocks.THERMOS_WALL)) buildCell(w, c);
				p.requestTeleport(center.x, center.y, center.z);
				p.fallDistance = 0;
			}
			if (p.age % 4 == 0) {
				double a = p.age * 0.15;
				for (int i = 0; i < 3; i++) {
					double ang = a + i * Math.PI * 2 / 3;
					w.spawnParticles(GhostPowers.GREEN, c.getX() + 0.5 + Math.cos(ang) * 3, c.getY() + 1 + (p.age % 60) / 10.0,
							c.getZ() + 0.5 + Math.sin(ang) * 3, 1, 0, 0, 0, 0);
				}
			}
			if (p.age % 100 == 0) {
				p.sendMessage(Text.literal("Punch the Thermos walls to break out!").formatted(Formatting.GREEN), true);
			}
		}
	}
}
