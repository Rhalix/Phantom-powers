package com.phantompowers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PortalLogic {
	public static final RegistryKey<World> GHOST_ZONE = RegistryKey.of(RegistryKeys.WORLD, PhantomMod.id("ghost_zone"));

	private static final int MAX_INTERIOR = 64;
	private static final int MIN_INTERIOR = 2;
	private static final int TRAVEL_COOLDOWN = 60;

	private static final List<Runnable> NEXT_TICK = new ArrayList<>();
	private static final Map<UUID, BlockPos> TRAVEL = new LinkedHashMap<>();
	private static final Map<UUID, Integer> LAST_TRAVEL = new HashMap<>();

	private PortalLogic() {}

	public static void later(Runnable r) {
		NEXT_TICK.add(r);
	}

	public static void tick(MinecraftServer server) {
		if (!NEXT_TICK.isEmpty()) {
			List<Runnable> run = new ArrayList<>(NEXT_TICK);
			NEXT_TICK.clear();
			run.forEach(Runnable::run);
		}
		if (!TRAVEL.isEmpty()) {
			Map<UUID, BlockPos> todo = new LinkedHashMap<>(TRAVEL);
			TRAVEL.clear();
			todo.forEach((id, pos) -> {
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
				if (p != null && p.isAlive()) travel(p, pos);
			});
		}
	}

	// ------------------------------------------------------------ activation

	public static void onButtonPressed(ServerPlayerEntity p, ServerWorld w, BlockPos buttonPos) {
		BlockPos pos = buttonPos.toImmutable();
		later(() -> tryActivate(p, w, pos));
	}

	public record Frame(Direction.Axis axis, Set<BlockPos> interior) {}

	private record Scan(Frame frame, String problem, int quality) {}

	/** Looks for a closed frame of iron blocks + black concrete around the button, in either direction. */
	private static Scan findFrame(ServerWorld w, BlockPos start) {
		Scan best = null;
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Scan s = scan(w, start, axis);
			if (s.frame() != null) return s;
			if (best == null || s.quality() > best.quality()) best = s;
		}
		return best;
	}

	private static Scan scan(ServerWorld w, BlockPos start, Direction.Axis axis) {
		Direction side = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction[] dirs = {side, side.getOpposite(), Direction.UP, Direction.DOWN};
		Set<BlockPos> interior = new HashSet<>();
		Set<BlockPos> frame = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		interior.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			for (Direction d : dirs) {
				BlockPos n = cur.offset(d);
				if (interior.contains(n) || frame.contains(n)) continue;
				BlockState s = w.getBlockState(n);
				if (isFrame(s)) {
					frame.add(n);
				} else if (isOpening(s)) {
					interior.add(n);
					if (interior.size() > MAX_INTERIOR) {
						return new Scan(null, "The opening is too big or not closed in (max " + MAX_INTERIOR + " empty blocks inside).", 1);
					}
					queue.add(n);
				} else {
					String name = s.getBlock().getName().getString();
					return new Scan(null, "The frame has a gap: found " + name + " at "
							+ n.getX() + " " + n.getY() + " " + n.getZ() + ". Use only Ghost Portal Frame and Hazard Plating.", 2);
				}
			}
		}
		// Corners only touch the opening diagonally, so count them too.
		for (BlockPos ip : interior) {
			for (Direction a : new Direction[]{side, side.getOpposite()}) {
				for (Direction b : new Direction[]{Direction.UP, Direction.DOWN}) {
					BlockPos c = ip.offset(a).offset(b);
					if (!interior.contains(c) && isFrame(w.getBlockState(c))) frame.add(c);
				}
			}
		}
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (BlockPos ip : interior) {
			minY = Math.min(minY, ip.getY());
			maxY = Math.max(maxY, ip.getY());
		}
		if (interior.size() < MIN_INTERIOR || maxY - minY < 1) {
			return new Scan(null, "The opening is too small: make it at least 1 wide and 2 tall so a person fits inside.", 3);
		}
		return new Scan(new Frame(axis, interior), null, 4);
	}

	private static boolean isFrame(BlockState s) {
		return s.isOf(ModBlocks.PORTAL_FRAME) || s.isOf(ModBlocks.HAZARD_PLATING);
	}

	/** The old iron + black concrete build: point people to the new frame blocks. */
	private static boolean isOldFrame(BlockState s) {
		return s.isOf(Blocks.IRON_BLOCK) || s.isOf(Blocks.BLACK_CONCRETE);
	}

	private static boolean isOpening(BlockState s) {
		return s.isAir() || s.getBlock() instanceof ButtonBlock;
	}

	/** Only explain frame problems for buttons that are clearly part of a portal build. */
	private static boolean nearFrameBlocks(ServerWorld w, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (isFrame(w.getBlockState(pos.offset(d)))) return true;
		}
		return false;
	}

	private static boolean nearOldFrame(ServerWorld w, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (isOldFrame(w.getBlockState(pos.offset(d)))) return true;
		}
		return false;
	}

	/** Lights the portal if the button sits inside a valid frame. Returns the frame, or null. */
	public static Frame lightPortal(ServerWorld w, BlockPos buttonPos, ServerPlayerEntity explainTo) {
		if (!(w.getBlockState(buttonPos).getBlock() instanceof ButtonBlock)) return null;
		Scan scan = findFrame(w, buttonPos);
		if (scan.frame() == null) {
			if (explainTo != null && !nearFrameBlocks(w, buttonPos) && nearOldFrame(w, buttonPos)) {
				explainTo.sendMessage(Text.literal("Ghost Portal: the lab portal is now built from Ghost Portal Frame blocks (8 from iron, redstone and copper) "
						+ "with optional Hazard Plating trim. Make a ring around an opening (2 wide and 3 tall is perfect, 1x2 is the smallest), then press a button inside it.")
						.formatted(Formatting.YELLOW), false);
				return null;
			}
			if (explainTo != null && scan.problem() != null && nearFrameBlocks(w, buttonPos)) {
				explainTo.sendMessage(Text.literal("Ghost Portal: " + scan.problem()).formatted(Formatting.YELLOW), false);
			}
			return null;
		}
		Frame f = scan.frame();
		BlockState portal = ModBlocks.GHOST_PORTAL.getDefaultState().with(GhostPortalBlock.AXIS, f.axis());
		for (BlockPos ip : f.interior()) w.setBlockState(ip, portal, Block.NOTIFY_ALL);

		Vec3d center = centerOf(f);
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt != null) {
			bolt.setCosmetic(true);
			bolt.refreshPositionAfterTeleport(center);
			w.spawnEntity(bolt);
		}
		w.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 0.6f);
		w.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0f, 1.4f);
		w.spawnParticles(GhostPowers.GREEN, center.x, center.y, center.z, 120, 0.8, 1.2, 0.8, 0);
		return f;
	}

	private static void tryActivate(ServerPlayerEntity p, ServerWorld w, BlockPos buttonPos) {
		if (p.isRemoved()) return;
		Frame f = lightPortal(w, buttonPos, p);
		if (f == null) return;

		// Is the player standing in (or right at) the portal opening?
		boolean inside = false;
		for (BlockPos ip : f.interior()) {
			if (p.getWorld() == w && p.getBlockPos().getSquaredDistance(ip) <= 6.0) {
				inside = true;
				break;
			}
		}
		Vec3d front = frontOf(w, f);

		GhostState st = GhostState.of(w.getServer());
		GhostData d = st.data(p.getUuid());
		if (inside && !d.powers && p.isAlive()) {
			// the origin story: the portal surges through you (a cutscene), and you step out half ghost
			p.sendMessage(Text.literal("The Ghost Portal surges to life... with you inside it!").formatted(Formatting.GREEN), false);
			Cutscenes.startOrigin(p, centerOf(f), front);
		} else if (!d.powers) {
			p.sendMessage(Text.literal("The Ghost Portal is open! (You have to be standing inside it when it turns on to get ghost powers.)").formatted(Formatting.GREEN), false);
		} else {
			p.sendMessage(Text.literal("The Ghost Portal is open! Step inside to enter the Ghost Zone.").formatted(Formatting.GREEN), false);
		}
	}

	/** Builds a test portal frame with a button and tries to light it. Used by /phantom testportal. */
	public static int testPortal(ServerWorld w, BlockPos base) {
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int x = base.getX() - 1; x <= base.getX() + 8; x++)
			for (int y = base.getY(); y <= base.getY() + 8; y++)
				for (int z = base.getZ() - 2; z <= base.getZ() + 2; z++) w.setBlockState(m.set(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		Set<BlockPos> interior = buildLabPortal(w, base, Direction.Axis.X, Block.NOTIFY_LISTENERS, false);
		BlockPos button = interior.stream().min((a, b) -> a.getY() != b.getY() ? a.getY() - b.getY() : a.getX() - b.getX()).orElseThrow();
		w.setBlockState(button, Blocks.STONE_BUTTON.getDefaultState().with(ButtonBlock.FACE, BlockFace.FLOOR), Block.NOTIFY_LISTENERS);
		Frame f = lightPortal(w, button, null);
		return f == null ? -1 : f.interior().size();
	}

	/**
	 * The lab portal: a compact ring of Ghost Portal Frame with Hazard Plating along the bottom and top,
	 * around a 2 wide, 3 tall opening. {@code base} is the bottom-left corner of the frame.
	 * Returns the opening (filled with portal if {@code lit}).
	 */
	public static Set<BlockPos> buildLabPortal(ServerWorld w, BlockPos base, Direction.Axis axis, int flags, boolean lit) {
		Set<BlockPos> interior = new HashSet<>();
		Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		for (int j = 1; j <= 3; j++)
			for (int i = 1; i <= 2; i++) interior.add(base.offset(along, i).up(j).toImmutable());
		BlockState frame = ModBlocks.PORTAL_FRAME.getDefaultState();
		BlockState hazard = ModBlocks.HAZARD_PLATING.getDefaultState();
		for (BlockPos ip : interior) {
			for (int di = -1; di <= 1; di++) {
				for (int dj = -1; dj <= 1; dj++) {
					BlockPos n = ip.offset(along, di).up(dj);
					if (interior.contains(n) || !w.getBlockState(n).isAir() && isFrame(w.getBlockState(n))) continue;
					int j = n.getY() - base.getY();
					w.setBlockState(n, (j == 0 || j == 4) ? hazard : frame, flags);
				}
			}
		}
		BlockState inside = lit ? ModBlocks.GHOST_PORTAL.getDefaultState().with(GhostPortalBlock.AXIS, axis) : Blocks.AIR.getDefaultState();
		for (BlockPos ip : interior) w.setBlockState(ip, inside, flags);
		return interior;
	}

	/** Called after the player respawns: put them in front of the portal and give them their powers. */
	public static void finishGhostRebirth(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (!d.pendingPowers) return;
		if (d.respawnDim != null && d.respawnPos != null) {
			ServerWorld dest = p.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(d.respawnDim)));
			if (dest != null) {
				p.teleport(dest, d.respawnPos.getX() + 0.5, d.respawnPos.getY(), d.respawnPos.getZ() + 0.5, p.getYaw(), p.getPitch());
			}
		}
		GhostPowers.grantPowers(p);
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("You're half ghost!").formatted(Formatting.GREEN, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Press V to go ghost").formatted(Formatting.WHITE)));
		p.sendMessage(Text.literal("You came back... different. You have ghost powers!").formatted(Formatting.GREEN), false);
		p.sendMessage(Text.literal("V = go ghost / go human.  R = Ecto Blast.  G / H / B = new powers you unlock with stat points.").formatted(Formatting.GRAY), false);
		p.sendMessage(Text.literal("Stay in ghost form to earn stat points (1 every 30 seconds).").formatted(Formatting.GRAY), false);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.0f, 0.6f);
		w.spawnParticles(GhostPowers.GREEN, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1.0, 0.5, 0);
	}

	private static Vec3d centerOf(Frame f) {
		double x = 0, y = 0, z = 0;
		for (BlockPos p : f.interior()) {
			x += p.getX();
			y += p.getY();
			z += p.getZ();
		}
		int n = f.interior().size();
		return new Vec3d(x / n + 0.5, y / n + 0.5, z / n + 0.5);
	}

	/** A safe spot 2 blocks in front of the bottom of the portal. */
	private static Vec3d frontOf(ServerWorld w, Frame f) {
		int minY = Integer.MAX_VALUE;
		for (BlockPos p : f.interior()) minY = Math.min(minY, p.getY());
		double sum = 0;
		int count = 0;
		BlockPos any = null;
		for (BlockPos p : f.interior()) {
			if (p.getY() == minY) {
				sum += f.axis() == Direction.Axis.X ? p.getX() : p.getZ();
				count++;
				any = p;
			}
		}
		double along = sum / count + 0.5;
		Direction normal = f.axis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		for (Direction side : new Direction[]{normal, normal.getOpposite()}) {
			BlockPos spot = any.offset(side, 2);
			if (isStandable(w, spot)) return toVec(f.axis(), along, spot);
		}
		return toVec(f.axis(), along, any.offset(normal, 2));
	}

	private static Vec3d toVec(Direction.Axis axis, double along, BlockPos spot) {
		return axis == Direction.Axis.X
				? new Vec3d(along, spot.getY(), spot.getZ() + 0.5)
				: new Vec3d(spot.getX() + 0.5, spot.getY(), along);
	}

	private static boolean isStandable(ServerWorld w, BlockPos pos) {
		return w.getBlockState(pos).getCollisionShape(w, pos).isEmpty()
				&& w.getBlockState(pos.up()).getCollisionShape(w, pos.up()).isEmpty();
	}

	// ---------------------------------------------------------------- travel

	public static void requestTravel(ServerPlayerEntity p, BlockPos portalPos) {
		TRAVEL.putIfAbsent(p.getUuid(), portalPos.toImmutable());
	}

	private static void travel(ServerPlayerEntity p, BlockPos portalPos) {
		MinecraftServer server = p.getServer();
		int now = server.getTicks();
		Integer last = LAST_TRAVEL.get(p.getUuid());
		if (last != null && now - last < TRAVEL_COOLDOWN) return;
		ServerWorld from = p.getServerWorld();
		BlockState ps = from.getBlockState(portalPos);
		if (!ps.isOf(ModBlocks.GHOST_PORTAL)) return;
		LAST_TRAVEL.put(p.getUuid(), now);

		GhostState st = GhostState.of(server);
		GhostData d = st.data(p.getUuid());

		if (from.getRegistryKey().equals(GHOST_ZONE)) {
			ServerWorld dest = null;
			BlockPos spot = d.portalPos;
			if (d.portalDim != null) {
				dest = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(d.portalDim)));
			}
			if (dest == null || spot == null) {
				dest = server.getOverworld();
				spot = dest.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, dest.getSpawnPos());
			}
			p.teleport(dest, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getYaw(), p.getPitch());
			GhostPowers.bar(p, "You escaped the Ghost Zone!", Formatting.GREEN);
		} else {
			ServerWorld zone = server.getWorld(GHOST_ZONE);
			if (zone == null) {
				GhostPowers.bar(p, "The Ghost Zone couldn't be found in this world.", Formatting.RED);
				return;
			}
			// Remember where to come back to: 2 blocks out of the portal, on the side you walked in from
			Direction.Axis axis = ps.get(GhostPortalBlock.AXIS);
			BlockPos bottom = portalPos;
			while (from.getBlockState(bottom.down()).isOf(ModBlocks.GHOST_PORTAL)) bottom = bottom.down();
			Direction normal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
			Vec3d look = p.getRotationVec(1f);
			double facing = axis == Direction.Axis.X ? look.z : look.x;
			Direction cameFrom = facing > 0 ? normal.getOpposite() : normal;
			d.portalDim = from.getRegistryKey().getValue().toString();
			d.portalPos = bottom.offset(cameFrom, 2);
			st.markDirty();

			sendToGhostZone(p, zone);
		}
		ServerWorld now2 = p.getServerWorld();
		now2.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.4f, 1.6f);
	}

	public static void sendToGhostZone(ServerPlayerEntity p, ServerWorld zone) {
		Vec3d arrival = ghostZoneArrival(zone, p.getBlockPos());
		p.teleport(zone, arrival.x, arrival.y, arrival.z, 180f, 0f);
		LAST_TRAVEL.put(p.getUuid(), p.getServer().getTicks());
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("The Ghost Zone").formatted(Formatting.GREEN, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Walk back through the portal to go home").formatted(Formatting.GRAY)));
	}

	/** Finds the return portal near these coordinates in the Ghost Zone, or builds one on a floating platform. */
	private static Vec3d ghostZoneArrival(ServerWorld zone, BlockPos near) {
		int bx = near.getX();
		int bz = near.getZ();
		int y0 = 100;
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int dx = -16; dx <= 16; dx++) {
			for (int dz = -16; dz <= 16; dz++) {
				m.set(bx + dx, y0 + 1, bz + dz);
				if (zone.getBlockState(m).isOf(ModBlocks.GHOST_PORTAL)
						&& !zone.getBlockState(m.west()).isOf(ModBlocks.GHOST_PORTAL)) {
					int w2 = 0;
					while (w2 < 6 && zone.getBlockState(m.east(w2)).isOf(ModBlocks.GHOST_PORTAL)) w2++;
					return new Vec3d(m.getX() + w2 / 2.0, y0, m.getZ() + 2.5);
				}
			}
		}

		int x0 = bx - 2;
		int z0 = bz;
		int flags = Block.NOTIFY_LISTENERS;
		BlockState air = Blocks.AIR.getDefaultState();
		for (int x = x0 - 3; x <= x0 + 8; x++) {
			for (int z = z0 - 3; z <= z0 + 5; z++) {
				for (int y = y0; y <= y0 + 8; y++) zone.setBlockState(m.set(x, y, z), air, flags);
				zone.setBlockState(m.set(x, y0 - 1, z), ModBlocks.ECTO_GRASS.getDefaultState(), flags);
				zone.setBlockState(m.set(x, y0 - 2, z), ModBlocks.ECTO_STONE.getDefaultState(), flags);
			}
		}
		buildLabPortal(zone, new BlockPos(x0 - 1, y0, z0), Direction.Axis.X, flags, true);
		return new Vec3d(x0 + 1.0, y0, z0 + 2.5);
	}
}
