package com.phantompowers;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side list of portal openings that should show a big swirling vortex. Portal blocks report themselves
 * from their display ticks (which only run on the client); the renderer draws one vortex per opening.
 */
public final class PortalSwirls {
	public static final class Swirl {
		public Vec3d center, along, normal;
		public double halfW, halfH;
		public long seen, born;
		public boolean small;
	}

	public static final Map<Long, Swirl> ACTIVE = new ConcurrentHashMap<>();

	private PortalSwirls() {}

	public static void report(Vec3d center, Vec3d along, double halfW, double halfH, boolean small, long time) {
		long key = BlockPos.ofFloored(center.multiply(2)).asLong();
		Swirl s = ACTIVE.get(key);
		if (s == null) {
			s = new Swirl();
			s.born = time;
			ACTIVE.put(key, s);
		}
		s.center = center;
		s.along = along;
		s.normal = new Vec3d(-along.z, 0, along.x);
		s.halfW = halfW;
		s.halfH = halfH;
		s.small = small;
		s.seen = time;
	}

	/** Measure a Ghost Portal opening from one of its blocks and report it. */
	public static void reportGhostPortal(World world, BlockPos pos, BlockState state, boolean xAxis) {
		BlockPos.Mutable m = new BlockPos.Mutable();
		int dx = xAxis ? 1 : 0, dz = xAxis ? 0 : 1;
		int lo = 0, hi = 0, down = 0, up = 0;
		while (lo < 24 && world.getBlockState(m.set(pos.getX() - dx * (lo + 1), pos.getY(), pos.getZ() - dz * (lo + 1))).isOf(state.getBlock())) lo++;
		while (hi < 24 && world.getBlockState(m.set(pos.getX() + dx * (hi + 1), pos.getY(), pos.getZ() + dz * (hi + 1))).isOf(state.getBlock())) hi++;
		while (down < 24 && world.getBlockState(m.set(pos.getX(), pos.getY() - down - 1, pos.getZ())).isOf(state.getBlock())) down++;
		while (up < 24 && world.getBlockState(m.set(pos.getX(), pos.getY() + up + 1, pos.getZ())).isOf(state.getBlock())) up++;
		double minA = -lo, maxA = hi + 1, minY = -down, maxY = up + 1;
		Vec3d along = new Vec3d(dx, 0, dz);
		Vec3d corner = new Vec3d(pos.getX() + (xAxis ? 0 : 0.5), pos.getY(), pos.getZ() + (xAxis ? 0.5 : 0));
		Vec3d center = corner.add(along.multiply((minA + maxA) / 2)).add(0, (minY + maxY) / 2, 0);
		report(center, along, (maxA - minA) / 2, (maxY - minY) / 2, false, world.getTime());
	}
}
